package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.file

/**
 * Builds the per-endpoint block-style Kotest DSL file (`<Endpoint>Dsl.kt`) as an IR
 * [File], so the wrapping `KotlinIrEmitter` renders it alongside the generated models
 * and endpoints. The DSL body is rendered as raw Kotlin via [raw] — the shapes are
 * specific enough that hand-rolled templates are clearer than IR nodes.
 *
 * ## Block style
 *
 * Each endpoint object carries a `call` extension taking a `<Endpoint>Scope` receiver:
 *
 * ```
 * PutTodo.call {
 *     path   = { id = Arb.constant("42") }
 *     query  = { done = Arb.constant(true) }   // `name` (nullable) may be omitted
 *     header = { token = Arb.constant(Token("iss")) }
 *     body   = { name = Arb.constant("milk"); done = Arb.constant(false) }
 *     expecting<PutTodo.Response200>()
 * }
 * ```
 *
 * Each slot is an assignable `var` holding a builder lambda. The terminal
 * `expecting`/`returning`/`collecting` functions first call `flush()`, which applies the
 * assigned slot lambdas to the runtime call and **validates required values** — a path/
 * query/header slot with a non-nullable field is required, as is each non-nullable field
 * within it. A missing required slot or field throws a clear error at scenario start.
 * (Kotlin cannot require that a `var` be assigned inside a lambda, so this enforcement is
 * runtime, not compile-time.) Nullable fields default to `Arb.constant(null)`; the request
 * body stays optional (the runtime generates unset fields).
 */
internal object EndpointDslFile {

    fun build(
        endpoint: Endpoint,
        packageName: PackageName,
        types: Map<String, Type> = emptyMap(),
        refined: Map<String, Refined> = emptyMap(),
    ): File {
        val shape = EndpointShape.from(endpoint, types, refined)
        val present = presentSlots(shape)
        val required = requiredSlots(shape)
        val kotestPkg = "${packageName.value}.kotest"
        val endpointPkg = "${packageName.value}.endpoint"
        val modelPkg = "${packageName.value}.model"
        val fileName = PackageName(kotestPkg).toDir() + "${shape.name}Dsl"

        return file(Name.of(fileName)) {
            `package`(kotestPkg)

            import("community.flock.wirespec.integration.kotest.dsl", "endpointCall")
            import("community.flock.wirespec.integration.kotest.dsl", "WirespecScenarioDsl")
            import("kotlin.time", "Duration")
            import("kotlin.reflect", "KClass")
            // The generated body transform reconstructs the contract default body with the
            // typed builder's per-field override `Gen`s, drawing each value via `Gen.draw(rs)`.
            if (shape.bodyFieldShapes.isNotEmpty()) {
                import("community.flock.wirespec.integration.kotest.dsl", "draw")
            }
            import(endpointPkg, shape.name)
            val needsGen = shape.bodyType != null || shape.bodyFieldShapes.isNotEmpty() || present.isNotEmpty()
            if (needsGen) {
                import("io.kotest.property", "Gen")
            }
            val isListBody = shape.bodyKind == EndpointShape.BodyKind.List
            // Nullable path/query/header fields default to `Arb.constant(null)` when the
            // caller leaves them unset; that default is applied in `flush()`.
            val needsArbConstant = (shape.pathFields + shape.queryFields + shape.headerFields).any { it.isNullable }
            if (isListBody || needsArbConstant) {
                import("io.kotest.property", "Arb")
            }
            if (isListBody) {
                // List body builder samples its element count with `Arb.int(count)`.
                // Arb.int is an extension on Arb.Companion in io.kotest.property.arbitrary.
                import("io.kotest.property.arbitrary", "int")
            }
            if (needsArbConstant) {
                import("io.kotest.property.arbitrary", "constant")
            }
            // Model class names are pascalCased by the Kotlin IR emitter (see
            // KotlinIrTransformer.buildModelImports). Apply the same normalisation so
            // imports of underscore-bearing types (e.g. the HAL `Contact_embedded`)
            // resolve to the emitted class (`ContactEmbedded`).
            shape.modelImports.forEach { import(modelPkg, Name.of(it).pascalCase()) }

            raw(renderCallExtension(shape))
            raw(renderScopeClass(shape, present, required))
            present.forEach { slot -> raw(renderSlotBuilder(shape, slot)) }
            if (shape.bodyType != null && shape.bodyFieldShapes.isNotEmpty()) {
                val element = shape.bodyElementType
                    ?: error("bodyFieldShapes present but no bodyElementType for ${shape.name}")
                raw(renderBodyBuilder(element, shape.bodyFieldShapes, shape.name))
                // Emit nested-type builders, deduped by type name. Each appears once per file.
                val nestedDefs = collectNestedBuilders(shape.bodyFieldShapes, alreadyEmitted = setOf(element))
                nestedDefs.forEach { (typeName, fields) ->
                    raw(renderBodyBuilder(typeName, fields, shape.name))
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Slots
    // ------------------------------------------------------------------------------------

    /** A path/query/header slot present on an endpoint, with the runtime registration fn. */
    private data class Slot(val name: String, val genFn: String, val fields: List<EndpointShape.NamedTypedField>)

    private fun presentSlots(shape: EndpointShape): List<Slot> = listOf(
        Slot("path", "pathGen", shape.pathFields),
        Slot("query", "queryGen", shape.queryFields),
        Slot("header", "headerGen", shape.headerFields),
    ).filter { it.fields.isNotEmpty() }

    /** Slots the caller must supply: those carrying at least one non-nullable field. */
    private fun requiredSlots(shape: EndpointShape): List<Slot> = presentSlots(shape).filter { slot -> slot.fields.any { !it.isNullable } }

    private fun cap(name: String): String = name.replaceFirstChar(Char::uppercaseChar)

    private fun slotBuilderName(shape: EndpointShape, slot: Slot): String = "${shape.name}${cap(slot.name)}Builder"

    // ------------------------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------------------------

    /**
     * The DSL entry point: a `call` extension on the generated endpoint object (e.g.
     * `PutTodo.call { … }`). It opens the `<Endpoint>Scope` receiver and runs the block,
     * returning whatever the terminal (`expecting`/`collecting`) yields.
     */
    private fun renderCallExtension(shape: EndpointShape): String = buildString {
        appendLine("public suspend fun <R> ${shape.name}.call(block: suspend ${shape.name}Scope.() -> R): R =")
        append("    ${shape.name}Scope().block()")
    }

    // ------------------------------------------------------------------------------------
    // Scope class
    // ------------------------------------------------------------------------------------

    private fun renderScopeClass(shape: EndpointShape, present: List<Slot>, required: List<Slot>): String = buildString {
        appendLine("@WirespecScenarioDsl")
        appendLine("public class ${shape.name}Scope internal constructor() {")
        appendLine("    private val inner = endpointCall(${shape.name}.Handler, ${shape.name})")
        present.forEach { slot ->
            appendLine("    public var ${slot.name}: (${slotBuilderName(shape, slot)}.() -> Unit)? = null")
        }
        if (shape.bodyFieldShapes.isNotEmpty()) {
            val builderName = "${shape.name}${shape.bodyElementType}BodyBuilder"
            appendLine("    public var body: ($builderName.() -> Unit)? = null")
            if (shape.bodyKind == EndpointShape.BodyKind.List) {
                appendLine("    public var bodyCount: IntRange = 1..3")
            }
        }
        appendLine(renderFlush(shape, present, required))
        append(renderTerminalMembers(shape))
        append("\n}")
    }

    /**
     * Applies the assigned slot/body lambdas to the runtime call and validates required
     * values. Idempotent (guarded by `flushed`) so repeated terminal calls are safe.
     */
    private fun renderFlush(shape: EndpointShape, present: List<Slot>, required: List<Slot>): String = buildString {
        val requiredNames = required.map { it.name }.toSet()
        appendLine("    private var flushed: Boolean = false")
        appendLine("    private fun flush() {")
        appendLine("        if (flushed) return")
        appendLine("        flushed = true")
        present.forEach { slot ->
            val builderClass = slotBuilderName(shape, slot)
            val builderVar = "${slot.name}Builder"
            if (slot.name in requiredNames) {
                appendLine("        val $builderVar = $builderClass().apply(${slot.name} ?: error(\"${shape.name}: required `${slot.name}` block is missing\"))")
                slot.fields.forEach { f -> appendLine(registerLine(shape, slot, builderVar, f, "        ")) }
            } else {
                appendLine("        ${slot.name}?.let { block ->")
                appendLine("            val $builderVar = $builderClass().apply(block)")
                slot.fields.forEach { f -> appendLine(registerLine(shape, slot, builderVar, f, "            ")) }
                appendLine("        }")
            }
        }
        if (shape.bodyFieldShapes.isNotEmpty()) {
            val builderName = "${shape.name}${shape.bodyElementType}BodyBuilder"
            val isList = shape.bodyKind == EndpointShape.BodyKind.List
            val elementType = shape.bodyElementType
            appendLine("        body?.let { block ->")
            appendLine("            val builder = $builderName().apply(block)")
            if (isList) {
                appendLine("            inner.bodyListSize(Arb.int(bodyCount))")
            }
            // Reconstruct the contract default body (`rawBase`) by copying it with each
            // overridden field replaced; un-set fields keep their generated default. A list
            // body applies the same per-element overrides to every generated element.
            appendLine("            inner.bodyTransform { rawBase, rs ->")
            if (isList) {
                appendLine("                @Suppress(\"UNCHECKED_CAST\")")
                appendLine("                (rawBase as List<$elementType>).map { base ->")
                appendBodyCopy(this, "base", "builder", shape, indent = "                    ")
                appendLine("                }")
            } else {
                appendLine("                val base = rawBase as $elementType")
                appendBodyCopy(this, "base", "builder", shape, indent = "                ")
            }
            appendLine("            }")
            appendLine("        }")
        }
        append("    }")
    }

    /** A single `inner.<gen>("wire", builder.field ?: …)` registration line for a slot field. */
    private fun registerLine(
        shape: EndpointShape,
        slot: Slot,
        builderVar: String,
        field: EndpointShape.NamedTypedField,
        indent: String,
    ): String {
        val ref = "$builderVar.${KotlinIdentifier.escape(field.name)}"
        val fallback = if (field.isNullable) {
            "Arb.constant(null)"
        } else {
            "error(\"${shape.name}.${slot.name}: required `${field.name}` is missing\")"
        }
        return "${indent}inner.${slot.genFn}(\"${field.name}\", $ref ?: $fallback)"
    }

    private fun renderTerminalMembers(shape: EndpointShape): String = buildString {
        val resp = "${shape.name}.Response<*>"
        // The public terminals stay `inline`/`reified` so call sites read `expecting<R>()`, but
        // they only capture `R::class` and delegate to the non-inline `@PublishedApi` bridges
        // below. That keeps `inner`/`flush()`/`flushed` private — the inline bodies never touch
        // them, so only the small bridge surface is exposed for inlining.
        appendLine("    @PublishedApi internal suspend fun <R : $resp> expectingClass(variantClass: KClass<R>): R {")
        appendLine("        flush()")
        appendLine("        return inner.expecting(variantClass)")
        appendLine("    }")
        appendLine("    @PublishedApi internal suspend fun <R : $resp> expectingClass(variantClass: KClass<R>, block: (R) -> Unit): R {")
        appendLine("        flush()")
        appendLine("        return inner.expecting(variantClass, block)")
        appendLine("    }")
        appendLine("    @PublishedApi internal suspend fun <R : $resp> expectingClass(variantClass: KClass<R>, block: (${shape.name}.Request, R) -> Unit): R {")
        appendLine("        flush()")
        appendLine("        return inner.expecting(variantClass, block)")
        appendLine("    }")
        appendLine("    @PublishedApi internal suspend fun <R : $resp> collectingClass(variantClass: KClass<R>, block: (List<R>) -> Unit) {")
        appendLine("        flush()")
        appendLine("        inner.collecting(variantClass, block)")
        appendLine("    }")
        appendLine("    public suspend inline fun <reified R : $resp> expecting(): R = expectingClass(R::class)")
        appendLine("    public suspend inline fun <reified R : $resp> expecting(noinline block: (R) -> Unit): R = expectingClass(R::class, block)")
        appendLine("    public suspend inline fun <reified R : $resp> expecting(noinline block: (${shape.name}.Request, R) -> Unit): R = expectingClass(R::class, block)")
        // No `returning<R, T>`: `R` is only in the projection's input position so it can never
        // be inferred, and Kotlin's all-or-nothing type args would then force `T` to be spelled
        // out too — naming the response type twice. `expecting<R>()` already returns `R`, so
        // `expecting<R>().body` / `expecting<R>().let { … }` projects while naming `R` once.
        appendLine("    public suspend inline fun <reified R : $resp> collecting(count: Int, noinline block: (List<R>) -> Unit) = collectingClass(R::class, block)")
        append("    public suspend inline fun <reified R : $resp> collecting(duration: Duration, noinline block: (List<R>) -> Unit) = collectingClass(R::class, block)")
    }

    // ------------------------------------------------------------------------------------
    // Builders
    // ------------------------------------------------------------------------------------

    private fun renderSlotBuilder(shape: EndpointShape, slot: Slot): String = buildString {
        appendLine("@WirespecScenarioDsl")
        appendLine("public class ${slotBuilderName(shape, slot)} {")
        slot.fields.forEach { f ->
            appendLine("    public var ${KotlinIdentifier.escape(f.name)}: Gen<${f.kotlinType}>? = null")
        }
        append("}")
    }

    /**
     * Emit `$baseExpr.copy(field = <value>, …)` for a body shape, one field per line.
     * Each value is produced by [copyValueExpr] — an override drawn from the builder's
     * `Gen`, falling back to the generated default carried on [baseExpr].
     */
    private fun appendBodyCopy(
        out: StringBuilder,
        baseExpr: String,
        receiver: String,
        shape: EndpointShape,
        indent: String,
    ) {
        out.appendLine("$indent$baseExpr.copy(")
        shape.bodyFieldShapes.forEach { f ->
            out.appendLine("$indent    ${KotlinIdentifier.escape(f.name)} = ${copyValueExpr(f, baseExpr, receiver, shape.name)},")
        }
        out.appendLine("$indent)")
    }

    /**
     * A single field's reconstructed value. A primitive override draws from its `Gen` and
     * re-wraps refined values; an unset override falls back to `$baseExpr.<field>`. Nested
     * object/list fields recurse, copying the generated default in place (and mapping over
     * every element for a nested list, matching the previous `"*"` wildcard semantics).
     */
    private fun copyValueExpr(
        field: EndpointShape.BodyFieldShape,
        baseExpr: String,
        receiver: String,
        builderPrefix: String,
    ): String {
        val fieldRef = KotlinIdentifier.escape(field.name)
        val blockRef = KotlinIdentifier.escape("_${field.name}Block")
        val baseField = "$baseExpr.$fieldRef"
        return when (field) {
            is EndpointShape.BodyFieldShape.Primitive ->
                "$receiver.$fieldRef?.let { gen -> ${wrapDrawn("gen.draw(rs)", field)} } ?: $baseField"
            is EndpointShape.BodyFieldShape.NestedObject -> {
                val nestedBuilder = "$builderPrefix${field.typeName}BodyBuilder"
                val nestedVar = KotlinIdentifier.escape("nested_${field.name}")
                val elemVar = KotlinIdentifier.escape("base_${field.name}")
                val subs = field.fields.joinToString(", ") {
                    "${KotlinIdentifier.escape(it.name)} = ${copyValueExpr(it, elemVar, nestedVar, builderPrefix)}"
                }
                "$receiver.$fieldRef?.let { gen -> gen.draw(rs) } ?: " +
                    "$receiver.$blockRef?.let { block -> val $nestedVar = $nestedBuilder().apply(block); " +
                    "$baseField?.let { $elemVar -> $elemVar.copy($subs) } } ?: $baseField"
            }
            is EndpointShape.BodyFieldShape.NestedList -> {
                val nestedBuilder = "$builderPrefix${field.elementTypeName}BodyBuilder"
                val nestedVar = KotlinIdentifier.escape("nested_${field.name}")
                val elemVar = KotlinIdentifier.escape("elem_${field.name}")
                val subs = field.fields.joinToString(", ") {
                    "${KotlinIdentifier.escape(it.name)} = ${copyValueExpr(it, elemVar, nestedVar, builderPrefix)}"
                }
                "$receiver.$fieldRef?.let { gen -> gen.draw(rs) } ?: " +
                    "$receiver.$blockRef?.let { block -> val $nestedVar = $nestedBuilder().apply(block); " +
                    "$baseField?.map { $elemVar -> $elemVar.copy($subs) } } ?: $baseField"
            }
        }
    }

    /**
     * Wrap a drawn body value to match the model field's declared type. The builder exposes
     * refined fields as their unwrapped base primitive (`Gen<String>`), so a drawn value must
     * be re-wrapped into the `Refined` class before it can go into `copy(...)`: `Refined(v)`
     * for a scalar, `v.map { Refined(it) }` for a list, with null-safe variants. Non-refined
     * values pass through unchanged.
     */
    private fun wrapDrawn(drawn: String, field: EndpointShape.BodyFieldShape.Primitive): String {
        val refined = field.refinedTypeName ?: return drawn
        return when {
            field.isList && field.isNullable -> "$drawn?.map { $refined(it) }"
            field.isList -> "$drawn.map { $refined(it) }"
            field.isNullable -> "$drawn?.let { $refined(it) }"
            else -> "$refined($drawn)"
        }
    }

    private fun renderBodyBuilder(
        elementType: String,
        fields: List<EndpointShape.BodyFieldShape>,
        builderPrefix: String,
    ): String = buildString {
        appendLine("@WirespecScenarioDsl")
        appendLine("public class $builderPrefix${elementType}BodyBuilder {")
        fields.forEach { f ->
            val fieldRef = KotlinIdentifier.escape(f.name)
            val blockRef = KotlinIdentifier.escape("_${f.name}Block")
            when (f) {
                is EndpointShape.BodyFieldShape.Primitive -> {
                    appendLine("    public var $fieldRef: Gen<${f.kotlinType}>? = null")
                }
                is EndpointShape.BodyFieldShape.NestedObject -> {
                    // Whole-value setter (`config = Arb.constant(...)`) pins the entire object; the
                    // `${field}Block` sub-block keeps per-field overrides. The setter wins (copyValueExpr).
                    appendLine("    public var $fieldRef: Gen<${f.typeName}>? = null")
                    appendLine("    @PublishedApi internal var $blockRef: ($builderPrefix${f.typeName}BodyBuilder.() -> Unit)? = null")
                    appendLine("    public fun ${KotlinIdentifier.escape("${f.name}Block")}(block: $builderPrefix${f.typeName}BodyBuilder.() -> Unit) { $blockRef = block }")
                }
                is EndpointShape.BodyFieldShape.NestedList -> {
                    // Whole-list setter (`tools = Arb.constant(listOf(...))`) fixes the exact list; the
                    // `${field}Block` sub-block keeps per-element overrides. The setter wins (copyValueExpr).
                    appendLine("    public var $fieldRef: Gen<List<${f.elementTypeName}>>? = null")
                    appendLine("    @PublishedApi internal var $blockRef: ($builderPrefix${f.elementTypeName}BodyBuilder.() -> Unit)? = null")
                    appendLine("    public fun ${KotlinIdentifier.escape("${f.name}Block")}(block: $builderPrefix${f.elementTypeName}BodyBuilder.() -> Unit) { $blockRef = block }")
                }
            }
        }
        append("}")
    }

    private fun collectNestedBuilders(
        fields: List<EndpointShape.BodyFieldShape>,
        alreadyEmitted: Set<String>,
    ): List<Pair<String, List<EndpointShape.BodyFieldShape>>> {
        val result = mutableListOf<Pair<String, List<EndpointShape.BodyFieldShape>>>()
        val emitted = alreadyEmitted.toMutableSet()
        fun walk(fs: List<EndpointShape.BodyFieldShape>) {
            fs.forEach { f ->
                when (f) {
                    is EndpointShape.BodyFieldShape.Primitive -> Unit
                    is EndpointShape.BodyFieldShape.NestedObject -> {
                        if (emitted.add(f.typeName)) {
                            result += f.typeName to f.fields
                            walk(f.fields)
                        }
                    }
                    is EndpointShape.BodyFieldShape.NestedList -> {
                        if (emitted.add(f.elementTypeName)) {
                            result += f.elementTypeName to f.fields
                            walk(f.fields)
                        }
                    }
                }
            }
        }
        walk(fields)
        return result
    }
}
