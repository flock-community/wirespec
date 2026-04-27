package community.flock.wirespec.emitters.rust

import community.flock.wirespec.ir.core.ArrayIndexCall
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.Function
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Precision
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Transformer
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.transformer

// ─────────────────────────────────────────────────────────────────────────────
// Low-level borrow primitives (package-private top-level extensions).
// Used by both [RustTransform] and the hand-written `shared` trait DSL in
// `RustIrEmitter` — single source of truth for the `&` spelling.
// ─────────────────────────────────────────────────────────────────────────────

internal fun Type.Custom.borrow(): Type.Custom = copy(name = "&$name")
internal fun Type.Custom.borrowDyn(): Type.Custom = copy(name = "&dyn $name")
internal fun Type.Custom.borrowImpl(): Type.Custom = copy(name = "&impl $name")
internal fun Type.Custom.ownedImpl(): Type.Custom = copy(name = "impl $name")

/**
 * Post-convert Rustification pass.
 *
 * Runs immediately after `.convert()` (and after any structural transforms that reshape the IR, like
 * `flattenForRust`) and is the single source of truth for every Rust-specific type decision that isn't
 * a trivial one-to-one mapping:
 *
 *   1. [primitivesToCustom] — IR primitives whose Rust spelling is *non-trivial* become
 *      `Type.Custom` carrying their exact Rust spelling: `Any → Box<dyn std::any::Any>`,
 *      `Bytes → Vec<u8>`, `Reflect → std::any::TypeId`, `Wildcard → _`, `IntegerLiteral → i32`,
 *      `StringLiteral → String`. Trivial primitives (`Integer`, `Number`, `Boolean`, `Unit`,
 *      `String`, `Array`, `Dict`, `Nullable`) stay structural because [RustGenerator] has
 *      position-sensitive handling for them (e.g., Unit-return suppression, Literal rendering).
 *   2. [borrow] — function parameters get borrow rules applied (`String → &str`, non-Copy `T → &T`,
 *      Serializer/Deserializer → `&impl T`). Recurses through structural `Type.Nullable` so that
 *      `Option<&str>` and `Option<&T>` are produced.
 *   3. [boxWrapping] — placeholder for `Box<T>` / `Box<dyn Trait>` wrapping beyond the `Box<dyn Any>`
 *      case already handled in step 1.
 *   4. [serializationArgs] — expression-level: first argument of `serialize_*` / `deserialize_*`
 *      calls is wrapped in `&`.
 *
 * `Cow<'a, str>` and `Arc<T>` extension-point helpers ([cowStringFields], [arcWrapFields]) are available
 * but not wired into [apply] until a concrete use case surfaces.
 */
object RustTransform {

    fun <T : Element> apply(element: T): T = element
        .transform { apply(primitivesToCustom) }
        .transform { apply(borrow) }
        .transform { apply(boxWrapping) }
        .transform { apply(serializationArgs) }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. primitivesToCustom
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Substitutes only the primitives whose Rust spelling is *non-trivial* (not a simple name)
     * OR whose presence shouldn't leak past this pass. Trivial primitives (`Integer`, `Number`,
     * `Boolean`, `Unit`) stay as structural [Type]s because [RustGenerator] has position-sensitive
     * handling for them (Unit return-type suppression, Literal rendering, array-index type
     * checks) that expects the concrete variant, not an opaque `Custom`.
     */
    val primitivesToCustom: Transformer = transformer {
        type { t, tr ->
            when (t) {
                Type.Any -> Type.Custom("Box<dyn std::any::Any>")
                Type.Bytes -> Type.Custom("Vec<u8>")
                Type.Reflect -> Type.Custom("std::any::TypeId")
                Type.Wildcard -> Type.Custom("_")
                is Type.IntegerLiteral -> Type.Custom("i32")
                is Type.StringLiteral -> Type.Custom("String")
                else -> t.transformChildren(tr)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. borrow
    // ─────────────────────────────────────────────────────────────────────────────

    val borrow: Transformer = transformer {
        element { element, tr ->
            when (element) {
                is Interface -> {
                    // Skip the user-facing `Call` trait — its methods take owned types so the
                    // user can move them into the `Request` constructor (which wants owned).
                    if (element.name == Name.of("Call")) {
                        element
                    } else {
                        val borrowed = element.elements.map { inner ->
                            if (inner is Function) borrowParamsOf(inner) else inner
                        }
                        element.copy(elements = borrowed).transformChildren(tr)
                    }
                }
                is Function -> borrowParamsOf(element).transformChildren(tr)
                else -> element.transformChildren(tr)
            }
        }
    }

    private fun borrowParamsOf(fn: Function): Function = fn.copy(
        parameters = fn.parameters.map { param ->
            if (param.name.isSelfReceiver()) {
                param
            } else {
                param.copy(type = applyBorrowRule(param.type))
            }
        },
    )

    private fun applyBorrowRule(type: Type): Type = when (type) {
        is Type.Nullable -> Type.Nullable(applyBorrowRule(type.type))
        Type.String -> Type.Custom("&str")
        is Type.Custom -> when {
            type.isAlreadyBorrowed() -> type
            type.isSerializerLike() -> type.borrowImpl()
            type.isGeneratorFieldTrait() -> type.ownedImpl()
            else -> type
        }
        else -> type
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. boxWrapping — placeholder pass; concrete rules added as use cases appear
    // ─────────────────────────────────────────────────────────────────────────────

    val boxWrapping: Transformer = transformer { }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. serializationArgs
    // ─────────────────────────────────────────────────────────────────────────────

    private val serializationMethodNames = setOf(
        "serialize_path",
        "serialize_param",
        "serialize_body",
        "deserialize_path",
        "deserialize_param",
        "deserialize_body",
    )

    val serializationArgs: Transformer = transformer {
        statementAndExpression { s, t ->
            if (s is FunctionCall && s.name.snakeCase() in serializationMethodNames) {
                val newArgs = s.arguments.entries.mapIndexed { idx, (key, value) ->
                    val transformed = t.transformExpression(value)
                    if (idx == 0 && !(transformed is VariableReference && transformed.name.value() == "it")) {
                        key to transformed.toBorrowedRaw()
                    } else {
                        key to transformed
                    }
                }.toMap()
                s.copy(arguments = newArgs)
            } else {
                s.transformChildren(t)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Cow / Arc extension points
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Returns a transformer that replaces `Type.String` fields matching [predicate] with
     * `Custom("Cow<'a, str>")`. Caller is responsible for adding the `'a` type parameter to
     * the containing struct. Not wired into [apply] by default.
     */
    fun cowStringFields(predicate: (Name) -> Boolean): Transformer = transformer {
        field { f, tr ->
            val transformed = if (f.type is Type.String && predicate(f.name)) {
                f.copy(type = Type.Custom("Cow<'a, str>"))
            } else {
                f
            }
            transformed.transformChildren(tr)
        }
    }

    /**
     * Returns a transformer that wraps `Type.Custom` fields matching [predicate] in `Arc<T>`.
     * Not wired into [apply] by default.
     */
    fun arcWrapFields(predicate: (Name) -> Boolean): Transformer = transformer {
        field { f, tr ->
            val type = f.type
            val transformed = when {
                type is Type.Custom && predicate(f.name) -> f.copy(type = Type.Custom("Arc<${type.name}>"))
                else -> f
            }
            transformed.transformChildren(tr)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Raw-string rendering (for code paths that bypass the IR, e.g. client params)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Renders a fully-substituted [Type] to its Rust type string. Assumes [apply] has already
     * run (i.e. all primitives/containers are `Type.Custom`). `Type.String` is the one pre-
     * substitution leaf that survives — rendered as "String".
     */
    fun Type.rustName(): String = when (this) {
        is Type.Custom -> if (generics.isEmpty()) {
            name
        } else {
            "$name<${generics.joinToString(", ") { it.rustName() }}>"
        }
        Type.String -> "String"
        is Type.Nullable -> "Option<${type.rustName()}>"
        is Type.Array -> "Vec<${elementType.rustName()}>"
        is Type.Dict -> "std::collections::HashMap<${keyType.rustName()}, ${valueType.rustName()}>"
        Type.Any -> "Box<dyn std::any::Any>"
        Type.Bytes -> "Vec<u8>"
        Type.Unit -> "()"
        Type.Boolean -> "bool"
        Type.Wildcard -> "_"
        Type.Reflect -> "std::any::TypeId"
        is Type.Integer -> when (precision) {
            Precision.P32 -> "i32"
            Precision.P64 -> "i64"
        }
        is Type.Number -> when (precision) {
            Precision.P32 -> "f32"
            Precision.P64 -> "f64"
        }
        is Type.IntegerLiteral -> "i32"
        is Type.StringLiteral -> "String"
    }

    /**
     * Renders a [Type] to its Rust type string, applying [borrow] rules as if the position were
     * a function parameter. Used by `RustIrEmitter.buildClientParams` where params are emitted
     * as raw strings (bypassing the IR).
     */
    fun Type.toBorrowedParamRustName(): String = applyBorrowRule(this).rustName()

    // ─────────────────────────────────────────────────────────────────────────────
    // Predicates and low-level string primitives
    // ─────────────────────────────────────────────────────────────────────────────

    internal fun Type.isAlreadyBorrowed(): Boolean = this is Type.Custom && (name.startsWith("&") || name.startsWith("&dyn ") || name.startsWith("&impl ") || name.startsWith("&mut "))

    internal fun Type.Custom.isSerializerLike(): Boolean = name == "Serializer" || name == "Deserializer"

    internal fun Type.Custom.isGeneratorFieldTrait(): Boolean = name == "GeneratorField"

    /** `Copy`-like leaves that don't need borrowing in parameter positions. */
    internal fun Type.Custom.isCopyLike(): Boolean = name in copyLikeCustomNames

    private val copyLikeCustomNames = setOf(
        "i32", "i64", "f32", "f64", "bool", "u8", "u16", "u32", "u64", "usize", "isize", "()",
    )

    internal fun Name.isSelfReceiver(): Boolean {
        val v = value()
        return v == "self" || v == "&self" || v == "&mut self"
    }

    private fun Expression.toBorrowedRaw(): RawExpression = RawExpression("&${toRawCode()}")

    private fun String.sanitizeKeywords(): String = if (this in RustIrEmitter.reservedKeywords) "r#$this" else this

    /** Renders a Rust-side expression to its raw source-code form. Mirrors the pre-refactor
     * behavior that previously lived in `RustIrEmitter.toRawCode`. */
    private fun Expression.toRawCode(): String = when (this) {
        is VariableReference -> name.snakeCase().sanitizeKeywords()
        is FieldCall -> {
            val recv = receiver?.let { "${it.toRawCode()}." } ?: ""
            "$recv${field.snakeCase().sanitizeKeywords()}"
        }
        is ArrayIndexCall -> {
            val lit = index as? Literal
            val idx = when {
                lit != null && (lit.type is Type.Integer || lit.type is Type.Number) -> "${lit.value}"
                lit != null && lit.type is Type.String -> "\"${lit.value}\""
                else -> index.toRawCode()
            }
            "${receiver.toRawCode()}[$idx]"
        }
        is ConstructorStatement -> {
            val typeName = (type as? Type.Custom)?.name ?: type.toString()
            val args = namedArguments.entries.joinToString(", ") { "${it.key.snakeCase()}: ${it.value.toRawCode()}" }
            if (args.isEmpty()) "$typeName {}" else "$typeName { $args }"
        }
        is Literal -> when {
            type is Type.String -> "String::from(\"$value\")"
            type is Type.Boolean -> "$value"
            type is Type.Integer -> "$value"
            type is Type.Number -> "$value"
            else -> "$value"
        }
        is RawExpression -> code
        else -> error("Unsupported expression type in toRawCode: ${this::class.simpleName}")
    }
}
