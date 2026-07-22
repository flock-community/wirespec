package community.flock.wirespec.ir.core

import arrow.core.NonEmptyList

typealias IR = NonEmptyList<Element>

data class Name(val parts: List<String>) {
    constructor(vararg parts: String) : this(parts.toList())

    fun value(): String = parts.joinToString("")

    private fun wordParts(): List<String> = parts.filter { it.isNotEmpty() && it.any { ch -> ch.isLetterOrDigit() } }

    fun camelCase(): String {
        val words = wordParts()
        return if (words.size <= 1) {
            words.firstOrNull()?.replaceFirstChar { it.lowercase() } ?: ""
        } else {
            words.mapIndexed { index, part ->
                if (index == 0) part.replaceFirstChar { it.lowercase() } else part.replaceFirstChar { it.uppercase() }
            }.joinToString("")
        }
    }

    fun pascalCase(): String {
        val words = wordParts()
        return if (words.size <= 1) {
            words.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: ""
        } else {
            words.joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    fun snakeCase(): String {
        val words = wordParts()
        return if (words.size <= 1) {
            words.firstOrNull()
                ?.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
                ?.lowercase()
                ?: ""
        } else {
            words.joinToString("_") { it.lowercase() }
        }
    }

    fun referenceName(): String = if (parts.size > 1) pascalCase() else value()

    override fun toString(): String = camelCase()

    companion object {
        private val SPLIT_PATTERN = Regex("[A-Z]{2,}(?=[A-Z][a-z])|[A-Z]?[a-z0-9]+|[A-Z]+|[^a-zA-Z0-9]+")
        fun of(value: String): Name = Name(SPLIT_PATTERN.findAll(value).map { it.value }.toList())
    }
}

fun name(vararg parts: String): Name = Name(parts.toList())

enum class Precision {
    P32,
    P64,
}

/**
 * Declaration visibility. `null` on a declaration means "emit no modifier" (the language
 * default), preserving byte-for-byte output for every node that predates this field; a
 * non-null value opts a declaration into an explicit modifier (e.g. Kotlin `public`/`internal`).
 */
enum class Visibility {
    PUBLIC,
    INTERNAL,
    PRIVATE,
    PROTECTED,
}

sealed interface Type {
    data class Integer(val precision: Precision = Precision.P32) : Type
    data class Number(val precision: Precision = Precision.P64) : Type
    object Any : Type
    object String : Type
    object Boolean : Type
    object Bytes : Type
    object Unit : Type
    object Wildcard : Type
    object Reflect : Type
    data class Array(val elementType: Type) : Type
    data class Dict(val keyType: Type, val valueType: Type) : Type
    data class Custom(val name: Name, val generics: List<Type> = emptyList()) : Type {
        // Convenience constructor for raw type expressions (e.g. `Wirespec.Model`,
        // `List<String>`, `$x.Call`) that contain non-identifier characters and
        // must be preserved as-is. Names made up of letters, digits, or
        // underscores are routed through `Name.of(...)` so the generator's
        // `pascalCase()` normalises them the same way struct names are.
        constructor(name: kotlin.String, generics: List<Type> = emptyList()) : this(
            if (name.any { c -> !c.isLetterOrDigit() && c != '_' }) {
                Name(listOf(name))
            } else {
                Name.of(name)
            },
            generics,
        )
    }
    data class Nullable(val type: Type) : Type
    data class IntegerLiteral(val value: Int) : Type
    data class StringLiteral(val value: kotlin.String) : Type

    /**
     * A function type. [receiver] (when non-null) makes it a receiver function type
     * (Kotlin `Receiver.() -> R`); [isAsync] marks it `suspend` in Kotlin. Both default so plain
     * `(A, B) -> R` function types are unchanged.
     */
    data class Function(
        val parameterTypes: List<Type>,
        val returnType: Type,
        val receiver: Type? = null,
        val isAsync: kotlin.Boolean = false,
    ) : Type
}

sealed interface Element

sealed interface HasName : Element {
    val name: Name
}

sealed interface HasElements {
    val elements: List<Element>
}

data class File(
    override val name: Name,
    override val elements: List<Element>,
) : HasName,
    HasElements

data class Package(
    val path: String,
) : Element

data class Import(
    val path: String,
    val type: Type.Custom,
    val isTypeOnly: Boolean = false,
) : Element

data class Struct(
    override val name: Name,
    val fields: List<Element>,
    val constructors: List<Constructor> = emptyList(),
    val interfaces: List<Type.Custom> = emptyList(),
    override val elements: List<Element> = emptyList(),
    val typeParameters: List<TypeParameter> = emptyList(),
    val visibility: Visibility? = null,
    val annotations: List<String> = emptyList(),
    /**
     * When set, forces a rendering that the field/constructor inference cannot produce.
     * [Kind.PLAIN_CLASS] emits a stateful `class` (its [elements] are member properties and
     * methods) instead of an inferred `object`/`data object`/`data class`. `null` keeps the
     * original inference, so pre-existing structs are unaffected.
     */
    val kind: Kind? = null,
    /** Visibility of the primary constructor, e.g. Kotlin `internal constructor()`. */
    val constructorVisibility: Visibility? = null,
) : HasName,
    HasElements {
    enum class Kind { PLAIN_CLASS }
}

/**
 * The [Field] entries of this struct's parameter list. A struct's [Struct.fields] may also
 * contain other elements (such as [RawElement] annotations injected before a field by an IR
 * extension); this helper filters those out when only the declared fields are needed.
 */
fun Struct.fieldList(): List<Field> = fields.filterIsInstance<Field>()

/**
 * Pairs each declared [Field] with the annotation codes that immediately precede it.
 *
 * An IR extension annotates a field by inserting [RawElement]s directly in front of it (see
 * the Jackson integration). This walks [fields] in order, buffering each raw annotation code
 * it encounters and attaching the buffer to the next [Field]. Elements that are neither a
 * [Field] nor a [RawElement] are ignored.
 */
fun Struct.annotatedFields(): List<Pair<Field, List<String>>> = buildList {
    val pendingAnnotations = mutableListOf<String>()
    fields.forEach { element ->
        when (element) {
            is RawElement -> pendingAnnotations += element.code
            is Field -> {
                add(element to pendingAnnotations.toList())
                pendingAnnotations.clear()
            }
            else -> Unit
        }
    }
}

data class Constructor(
    val parameters: List<Parameter>,
    val body: List<Statement>,
)

data class Field(
    val name: Name,
    val type: Type,
    val isOverride: Boolean = false,
    /**
     * When any of the following are set, the KotlinGenerator renders this [Field] as a
     * standalone property declaration (as a class member or a top-level/extension property)
     * rather than a constructor parameter. A field left with all defaults keeps its original
     * role and renders to nothing on its own, so existing usages are unaffected.
     */
    val isMutable: Boolean = false,
    val visibility: Visibility? = null,
    val annotations: List<String> = emptyList(),
    /** Extension-property receiver, e.g. the `Queue` in `val Queue.generate`. */
    val receiver: Type? = null,
    /** Property initializer (`= <expr>`). Mutually exclusive with [getter] in practice. */
    val initializer: Expression? = null,
    /** Property getter body (`get() = <expr>`). */
    val getter: Expression? = null,
) : Element

data class Function(
    override val name: Name,
    val typeParameters: List<TypeParameter> = emptyList(),
    val parameters: List<Parameter>,
    val returnType: Type?,
    val body: List<Statement>,
    val isAsync: Boolean = false,
    val isStatic: Boolean = false,
    val isOverride: Boolean = false,
    /** Extension-function receiver, e.g. the `Gen<Request>` in `fun Gen<Request>.call()`. */
    val receiver: Type? = null,
    val visibility: Visibility? = null,
    val annotations: List<String> = emptyList(),
) : HasName

data class Namespace(
    override val name: Name,
    override val elements: List<Element>,
    val extends: Type.Custom? = null,
) : HasName,
    HasElements

data class Interface(
    override val name: Name,
    override val elements: List<Element>,
    val extends: List<Type.Custom> = emptyList(),
    val isSealed: Boolean = false,
    val typeParameters: List<TypeParameter> = emptyList(),
    val fields: List<Field> = emptyList(),
) : HasName,
    HasElements

data class Union(
    override val name: Name,
    val extends: Type.Custom? = null,
    val members: List<Type.Custom> = emptyList(),
    val typeParameters: List<TypeParameter> = emptyList(),
) : HasName

data class Enum(
    override val name: Name,
    val extends: Type.Custom? = null,
    val entries: List<Entry>,
    val fields: List<Field> = emptyList(),
    val constructors: List<Constructor> = emptyList(),
    override val elements: List<Element> = emptyList(),
) : HasName,
    HasElements {
    data class Entry(val name: Name, val values: List<String>)
}

data class Parameter(
    val name: Name,
    val type: Type,
    /** Default value (`= <expr>`); `null` means the parameter is required. */
    val default: Expression? = null,
)

data class TypeParameter(
    val type: Type,
    val extends: List<Type> = emptyList(),
)

sealed interface Statement : Expression
sealed interface Expression

data class RawExpression(val code: String) : Statement

// Main entry point - represents a language-specific main/entry point

data class Main(val statics: List<Element> = emptyList(), val body: List<Statement>, val isAsync: Boolean = false) : Element

// Raw element - allows injecting raw code as an Element
data class RawElement(val code: String) : Element

// Null literal - represents the null value
data object NullLiteral : Statement, Expression

// Nullable empty literal - represents the empty optional value (e.g., Optional.empty() in Java, null in Kotlin)
data object NullableEmpty : Statement, Expression

// Variable/identifier reference - represents a reference to a variable
data class VariableReference(val name: Name) :
    Statement,
    Expression {
    constructor(name: String) : this(Name.of(name))
}

// Field access - represents accessing a field, optionally on a receiver (e.g., request.body or just body)
data class FieldCall(
    val receiver: Expression? = null,
    val field: Name,
) : Statement,
    Expression

// Function/method call - represents calling a function or method, optionally on a receiver
// If receiver is null, it's a standalone or static function call (e.g., fromRequest(...), java.util.Collections.emptyList())
// If receiver is present, it's a method call on an object (e.g., list.get(index))
data class FunctionCall(
    val receiver: Expression? = null,
    val typeArguments: List<Type> = emptyList(),
    val name: Name,
    val arguments: Map<Name, Expression> = emptyMap(),
    val isAwait: Boolean = false,
) : Statement,
    Expression

// Array/map index access - represents bracket syntax (e.g., receiver[index])
data class ArrayIndexCall(
    val receiver: Expression,
    val index: Expression,
    val caseSensitive: Boolean = true,
) : Statement,
    Expression

// Enum constant reference - represents an enum constant (e.g., Wirespec.Method.GET)
data class EnumReference(
    val enumType: Type.Custom,
    val entry: Name,
) : Statement,
    Expression

// Enum value name access - gets the string name of an enum value
// In Java: .name(), in Kotlin: .name, in TypeScript: no-op (enums are already strings)
data class EnumValueCall(
    val expression: Expression,
) : Statement,
    Expression

// Binary operations - represents binary operators (e.g., "message" + status)
data class BinaryOp(
    val left: Expression,
    val operator: Operator,
    val right: Expression,
) : Statement,
    Expression {
    enum class Operator { PLUS, EQUALS, NOT_EQUALS, UNTIL }
}

// Type descriptor - represents a runtime type descriptor for serialization
// In Java this emits Wirespec.getType(Type.class, Container.class)
// In other languages it may emit different type descriptor patterns
data class TypeDescriptor(val type: Type) :
    Statement,
    Expression

// Type cast - asserts the static type of `expression` as `targetType`.
// Kotlin/Scala emit an unchecked cast (`x as T` / `x.asInstanceOf[T]`); Java emits `((T) x)`;
// Rust emits `x as T`; TypeScript emits `x as T`; Python passes through (no static cast).
data class Cast(val expression: Expression, val targetType: Type) :
    Statement,
    Expression

data class PrintStatement(val expression: Expression) : Statement
data class ReturnStatement(val expression: Expression) : Statement
data class ConstructorStatement(val type: Type, val namedArguments: Map<Name, Expression> = emptyMap()) :
    Statement,
    Expression
data class Literal(val value: Any, val type: Type) :
    Statement,
    Expression
data class ClassReference(val type: Type) : Expression
data class LiteralList(val values: List<Expression>, val type: Type) :
    Statement,
    Expression
data class LiteralMap(val values: Map<String, Expression>, val keyType: Type, val valueType: Type) :
    Statement,
    Expression
data class Assignment(val name: Name, val value: Expression, val isProperty: Boolean = false) : Statement
data class ErrorStatement(val message: Expression) : Statement
data class AssertStatement(val expression: Expression, val message: String) : Statement

data class NullCheck(
    val expression: Expression,
    val body: Expression,
    val alternative: Expression?,
) : Statement,
    Expression

data class NullableMap(
    val expression: Expression,
    val body: Expression,
    val alternative: Expression,
) : Statement,
    Expression

data class NullableOf(
    val expression: Expression,
) : Statement,
    Expression

data class NullableGet(
    val expression: Expression,
) : Statement,
    Expression

sealed interface Constraint :
    Statement,
    Expression {
    data class RegexMatch(val pattern: String, val rawValue: String, val value: Expression) : Constraint
    data class BoundCheck(val min: String?, val max: String?, val value: Expression) : Constraint
}

// Boolean negation
data class NotExpression(val expression: Expression) :
    Statement,
    Expression

// Conditional expression (ternary)
data class IfExpression(
    val condition: Expression,
    val thenExpr: Expression,
    val elseExpr: Expression,
) : Statement,
    Expression

// Map over a list
data class MapExpression(
    val receiver: Expression,
    val variable: Name,
    val body: Expression,
) : Statement,
    Expression

// Indexed flatMap over a list
data class FlatMapIndexed(
    val receiver: Expression,
    val indexVar: Name,
    val elementVar: Name,
    val body: Expression,
) : Statement,
    Expression

// Concatenate multiple lists
data class ListConcat(val lists: List<Expression>) :
    Statement,
    Expression

// Lambda / thunk - represents a deferred expression with zero or more parameters.
// Kotlin: { p -> body }, Java: (p) -> body, TypeScript: (p: T) => body,
// Python: lambda p: body, Rust: Box::new(|p| body), Scala: (p) => body
data class Lambda(val parameters: List<Parameter>, val body: Expression) :
    Statement,
    Expression

// String interpolation
data class StringTemplate(val parts: List<Part>) :
    Statement,
    Expression {
    sealed interface Part {
        data class Text(val value: String) : Part
        data class Expr(val expression: Expression) : Part
    }
}

data class Switch(
    val expression: Expression,
    val cases: List<Case>,
    val default: List<Statement>? = null,
    val variable: Name? = null,
) : Statement

data class Case(
    val value: Expression,
    val body: List<Statement>,
    val type: Type? = null,
)
