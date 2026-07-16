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
 * Each endpoint object carries a `generate` extension property exposing the DSL entry
 * points (`request`, `response<NNN>`), each taking a scope receiver. `request { … }`
 * returns a `Gen<Request>`; the `call()` extension on that `Gen` is the one way to send it:
 *
 * ```
 * PutTodo.generate.request {
 *     path   { id = Arb.constant("42") }
 *     query  { done = Arb.constant(true) }   // `name` (nullable) may be omitted
 *     header { token = Arb.constant(Token("iss")) }
 *     body   { name = Arb.constant("milk"); done = Arb.constant(false) }
 * }.call()
 * ```
 *
 * `call()` draws one request from the `Gen` (seeded by the ambient `RandomSource`) and returns
 * the contract-validated `PutTodo.Response<*>`; narrow it with e.g.
 * kotest's `shouldBeInstanceOf<PutTodo.Response200>()`. `response<NNN> { … }` likewise returns a
 * `Gen<Response<NNN>>`.
 *
 * Each slot is an assignable `var` holding a builder lambda (`path = { … }`), with a
 * same-named function form (`path { … }`) assigning the same slot. `buildRequest()` first
 * calls `flush()`, which applies the assigned slot lambdas to the runtime call and
 * **validates required values** — a path/query/header slot with a non-nullable field is
 * required, as is each non-nullable field within it. A missing required slot or field
 * throws a clear error at scenario start. (Kotlin cannot require that a `var` be assigned
 * inside a lambda, so this enforcement is runtime, not compile-time.) Nullable fields
 * default to `Arb.constant(null)`; the request body stays optional (the runtime generates
 * unset fields).
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
            // `<Endpoint>.Request.call()` sends a request built by `generate.request { … }` as-is.
            import("community.flock.wirespec.integration.kotest.dsl", "requestCall")
            import("community.flock.wirespec.integration.kotest.dsl", "WirespecScenarioDsl")
            // Per-variant `responseNNN { … }` builders delegate to the runtime `responseCall`;
            // `Gen<Response<*>>.mock { … }` stubs a drawn response on the ambient mock server.
            if (shape.responseVariants.isNotEmpty()) {
                import("community.flock.wirespec.integration.kotest.dsl", "responseCall")
                import("community.flock.wirespec.integration.kotest.dsl", "responseMock")
            }
            // The generated body transform reconstructs the contract default body with the
            // typed builder's per-field override `Gen`s, drawing each value via `Gen.draw(rs)`.
            if (shape.bodyFieldShapes.isNotEmpty()) {
                import("community.flock.wirespec.integration.kotest.dsl", "draw")
            }
            import(endpointPkg, shape.name)
            // `request`/`response`/`buildRequest` return `Gen<…>` and `call()` extends `Gen<Request>`.
            import("io.kotest.property", "Gen")
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

            raw(renderGenerateExtension(shape))
            raw(renderRequestCallExtension(shape))
            if (shape.responseVariants.isNotEmpty()) {
                raw(renderResponseMockExtension(shape))
            }
            raw(renderScopeClass(shape, present, required))
            shape.responseVariants.forEach { variant -> raw(renderResponseScope(shape, variant)) }
            present.forEach { slot -> raw(renderSlotBuilder(shape, slot)) }
            // Request body field builders are the shared `<Type>Builder`s (emitted by each record's
            // own `<Type>Dsl.kt`); the scope and body transform reference them by name.
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
     * The DSL entry point: a `generate` extension property on the generated endpoint object
     * grouping the scenario builders (e.g. `PutTodo.generate.request { … }`). The
     * `<Endpoint>Generate` wrapper carries:
     * - `request` — opens the `<Endpoint>Scope` receiver and returns a `Gen<Request>`; sending
     *   chains off it via the `Gen<Request>.call()` extension;
     * - `response<NNN>` — one per response variant, returning a `Gen<Response<NNN>>` via its
     *   `<Endpoint>Response<NNN>Scope`.
     */
    private fun renderGenerateExtension(shape: EndpointShape): String = buildString {
        appendLine("public class ${shape.name}Generate internal constructor() {")
        appendLine("    public suspend fun request(block: suspend ${shape.name}Scope.() -> Unit): Gen<${shape.name}.Request> {")
        appendLine("        val scope = ${shape.name}Scope()")
        appendLine("        scope.block()")
        appendLine("        return scope.buildRequest()")
        appendLine("    }")
        shape.responseVariants.forEach { variant ->
            val scopeName = "${shape.name}${variant.className}Scope"
            appendLine("    public fun response${variant.status}(block: $scopeName.() -> Unit = {}): Gen<${shape.name}.${variant.className}> =")
            appendLine("        $scopeName().apply(block).build()")
        }
        appendLine("}")
        appendLine("public val ${shape.name}.generate: ${shape.name}Generate")
        append("    get() = ${shape.name}Generate()")
    }

    /**
     * A `call()` extension on `Gen<Request>`, so the request `Gen` chains straight into a send:
     * `PutTodo.generate.request { … }.call()`. It draws one request (seeded by the ambient
     * `RandomSource`), transports it, and returns the contract-validated response variant.
     */
    private fun renderRequestCallExtension(shape: EndpointShape): String = buildString {
        appendLine("public suspend fun Gen<${shape.name}.Request>.call(): ${shape.name}.Response<*> =")
        append("    requestCall(${shape.name}.Handler, ${shape.name}, this)")
    }

    /**
     * A `mock()` extension on `Gen<Response<*>>`, the response-side twin of `Gen<Request>.call()`:
     * `PutTodo.generate.response200 { … }.mock { req -> req.path.id == "1" }`. It draws one
     * response (seeded by the ambient `RandomSource`) and registers it on the ambient mock server
     * for every incoming request the typed [predicate] accepts. The receiver is the endpoint's
     * `Response<*>` supertype, so any `responseNNN { … }` variant `Gen` chains straight into it.
     */
    private fun renderResponseMockExtension(shape: EndpointShape): String = buildString {
        appendLine("public suspend fun Gen<${shape.name}.Response<*>>.mock(predicate: (${shape.name}.Request) -> Boolean): Unit =")
        append("    responseMock(${shape.name}.Handler, this, predicate)")
    }

    // ------------------------------------------------------------------------------------
    // Response scopes
    // ------------------------------------------------------------------------------------

    /**
     * Per-variant response scope, opened by `generate.responseNNN { … }`. The scope exposes a
     * whole-value `body` setter (when the variant has a body) and one setter per response
     * header field, each a `Gen<…>?` defaulting to a generated value. The terminal returns a
     * `Gen<Response<NNN>>`.
     */
    private fun renderResponseScope(shape: EndpointShape, variant: EndpointShape.ResponseVariantShape): String = buildString {
        val scopeName = "${shape.name}${variant.className}Scope"
        val variantType = "${shape.name}.${variant.className}"
        val hasBody = variant.bodyKind != EndpointShape.BodyKind.None && variant.bodyType != null

        appendLine("@WirespecScenarioDsl")
        appendLine("public class $scopeName internal constructor() {")
        appendLine("    private val inner = responseCall(${shape.name}, $variantType::class)")
        if (hasBody) {
            appendLine("    public var body: Gen<${variant.bodyType}>? = null")
        }
        variant.headerFields.forEach { f ->
            appendLine("    public var ${KotlinIdentifier.escape(f.name)}: Gen<${f.kotlinType}>? = null")
        }
        appendLine("    public fun build(): Gen<$variantType> {")
        if (hasBody) {
            appendLine("        body?.let { inner.body(it) }")
        }
        variant.headerFields.forEach { f ->
            appendLine("        ${KotlinIdentifier.escape(f.name)}?.let { inner.headerGen(\"${f.name}\", it) }")
        }
        appendLine("        @Suppress(\"UNCHECKED_CAST\")")
        appendLine("        return inner.buildGen() as Gen<$variantType>")
        appendLine("    }")
        append("}")
    }

    // ------------------------------------------------------------------------------------
    // Scope class
    // ------------------------------------------------------------------------------------

    private fun renderScopeClass(shape: EndpointShape, present: List<Slot>, required: List<Slot>): String = buildString {
        appendLine("@WirespecScenarioDsl")
        appendLine("public class ${shape.name}Scope internal constructor() {")
        appendLine("    private val inner = endpointCall(${shape.name}.Handler, ${shape.name})")
        // Each slot is both an assignable `var` (`path = { … }`) and a same-named function
        // (`path { … }`) assigning it — the function form reads better inside the DSL block.
        present.forEach { slot ->
            val builderClass = slotBuilderName(shape, slot)
            appendLine("    public var ${slot.name}: ($builderClass.() -> Unit)? = null")
            appendLine("    public fun ${slot.name}(block: $builderClass.() -> Unit) { this.${slot.name} = block }")
        }
        if (shape.bodyFieldShapes.isNotEmpty()) {
            val builderName = RecordBuilder.builderName(shape.bodyElementType ?: error("bodyFieldShapes present but no bodyElementType for ${shape.name}"))
            appendLine("    public var body: ($builderName.() -> Unit)? = null")
            appendLine("    public fun body(block: $builderName.() -> Unit) { this.body = block }")
            if (shape.bodyKind == EndpointShape.BodyKind.List) {
                appendLine("    public var bodyCount: IntRange = 1..3")
            }
        }
        appendLine(renderFlush(shape, present, required))
        // `buildRequest()` flushes (validating required slots eagerly) then returns a `Gen` that
        // materialises the typed request on each draw; it is the terminal behind the
        // `generate.request { … }` entry point. Sending happens through `Gen<Request>.call()`.
        appendLine("    public fun buildRequest(): Gen<${shape.name}.Request> {")
        appendLine("        flush()")
        appendLine("        return inner.buildRequestGen()")
        append("    }")
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
            val builderName = RecordBuilder.builderName(shape.bodyElementType ?: error("bodyFieldShapes present but no bodyElementType for ${shape.name}"))
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
            out.appendLine("$indent    ${KotlinIdentifier.escape(f.name)} = ${copyValueExpr(f, baseExpr, receiver)},")
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
    ): String {
        val fieldRef = KotlinIdentifier.escape(field.name)
        val blockRef = KotlinIdentifier.escape("_${field.name}Block")
        val baseField = "$baseExpr.$fieldRef"
        return when (field) {
            is EndpointShape.BodyFieldShape.Primitive ->
                "$receiver.$fieldRef?.let { gen -> ${wrapDrawn("gen.draw(rs)", field)} } ?: $baseField"
            is EndpointShape.BodyFieldShape.NestedObject -> {
                val nestedBuilder = RecordBuilder.builderName(field.typeName)
                val nestedVar = KotlinIdentifier.escape("nested_${field.name}")
                val elemVar = KotlinIdentifier.escape("base_${field.name}")
                val subs = field.fields.joinToString(", ") {
                    "${KotlinIdentifier.escape(it.name)} = ${copyValueExpr(it, elemVar, nestedVar)}"
                }
                "$receiver.$fieldRef?.let { gen -> gen.draw(rs) } ?: " +
                    "$receiver.$blockRef?.let { block -> val $nestedVar = $nestedBuilder().apply(block); " +
                    "$baseField?.let { $elemVar -> $elemVar.copy($subs) } } ?: $baseField"
            }
            is EndpointShape.BodyFieldShape.NestedList -> {
                val nestedBuilder = RecordBuilder.builderName(field.elementTypeName)
                val nestedVar = KotlinIdentifier.escape("nested_${field.name}")
                val elemVar = KotlinIdentifier.escape("elem_${field.name}")
                val subs = field.fields.joinToString(", ") {
                    "${KotlinIdentifier.escape(it.name)} = ${copyValueExpr(it, elemVar, nestedVar)}"
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
}
