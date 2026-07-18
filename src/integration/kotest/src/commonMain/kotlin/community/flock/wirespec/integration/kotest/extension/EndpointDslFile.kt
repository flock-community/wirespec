package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.Type as IrType

/**
 * Builds the per-endpoint block-style Kotest DSL file (`<Endpoint>Dsl.kt`) as an IR [File]
 * using the IR DSL builders: the `<Endpoint>Generate` class and its `generate` extension
 * property, the `Gen<Request>.call()` / `Gen<Response<*>>.mock()` extensions, the request
 * `<Endpoint>Scope`, the per-variant response scopes, and the slot builders. Declarations are
 * modelled as DSL nodes; only the genuinely-imperative method bodies (`flush`, the body
 * transform) remain raw code.
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
 * Each slot is set through its function form (`path { … }`); the underlying `var` holding
 * the builder lambda is private, so that function is the only way in. `buildRequest()` first
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

            buildGenerateWrapper(shape)
            property(
                name = "generate",
                type = IrType.Custom("${shape.name}Generate"),
                visibility = Visibility.PUBLIC,
                receiver = IrType.Custom(shape.name),
                getter = rawExpr("${shape.name}Generate()"),
            )
            buildRequestCall(shape)
            if (shape.responseVariants.isNotEmpty()) {
                buildResponseMock(shape)
            }
            buildScopeClass(shape, present, required)
            shape.responseVariants.forEach { variant -> buildResponseScope(shape, variant) }
            present.forEach { slot -> buildSlotBuilder(shape, slot) }
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
    // IR type helpers
    // ------------------------------------------------------------------------------------

    private fun genOf(inner: String): IrType = IrType.Custom("Gen", listOf(IrType.Custom(inner)))
    private fun genNullableOf(inner: String): IrType = IrType.Nullable(genOf(inner))
    private fun blockType(builder: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(builder))
    private fun suspendScopeType(scope: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(scope), isSuspend = true)

    // ------------------------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------------------------

    /**
     * The `<Endpoint>Generate` wrapper grouping the scenario builders (reached via the
     * `generate` extension property): `request` opens the `<Endpoint>Scope` and returns a
     * `Gen<Request>`; each `response<NNN>` returns a `Gen<Response<NNN>>` via its response scope.
     */
    private fun community.flock.wirespec.ir.core.FileBuilder.buildGenerateWrapper(shape: EndpointShape) {
        struct("${shape.name}Generate") {
            plainClass()
            visibility(Visibility.PUBLIC)
            constructorVisibility(Visibility.INTERNAL)
            asyncFunction("request") {
                visibility(Visibility.PUBLIC)
                arg("block", suspendScopeType("${shape.name}Scope"))
                returnType(genOf("${shape.name}.Request"))
                raw("val scope = ${shape.name}Scope()")
                raw("scope.block()")
                returns(rawExpr("scope.buildRequest()"))
            }
            shape.responseVariants.forEach { variant ->
                val scopeName = "${shape.name}${variant.className}Scope"
                function("response${variant.status}") {
                    visibility(Visibility.PUBLIC)
                    arg("block", blockType(scopeName), rawExpr("{}"))
                    returnType(genOf("${shape.name}.${variant.className}"))
                    returns(rawExpr("$scopeName().apply(block).build()"))
                }
            }
        }
    }

    /**
     * A `call()` extension on `Gen<Request>`, so the request `Gen` chains straight into a send:
     * `PutTodo.generate.request { … }.call()`.
     */
    private fun community.flock.wirespec.ir.core.FileBuilder.buildRequestCall(shape: EndpointShape) {
        asyncFunction("call") {
            visibility(Visibility.PUBLIC)
            receiver(genOf("${shape.name}.Request"))
            returnType(IrType.Custom("${shape.name}.Response<*>"))
            returns(rawExpr("requestCall(${shape.name}.Handler, ${shape.name}, this)"))
        }
    }

    /**
     * A `mock()` extension on `Gen<Response<*>>`, the response-side twin of `Gen<Request>.call()`:
     * `PutTodo.generate.response200 { … }.mock { req -> req.path.id == "1" }`.
     */
    private fun community.flock.wirespec.ir.core.FileBuilder.buildResponseMock(shape: EndpointShape) {
        asyncFunction("mock") {
            visibility(Visibility.PUBLIC)
            receiver(genOf("${shape.name}.Response<*>"))
            arg("predicate", IrType.Function(listOf(IrType.Custom("${shape.name}.Request")), IrType.Boolean))
            returnType(IrType.Unit)
            returns(rawExpr("responseMock(${shape.name}.Handler, this, predicate)"))
        }
    }

    // ------------------------------------------------------------------------------------
    // Response scopes
    // ------------------------------------------------------------------------------------

    /**
     * Per-variant response scope, opened by `generate.responseNNN { … }`. Exposes a whole-value
     * `body` setter (when the variant has a body) and one setter per response header field, each
     * a `Gen<…>?` defaulting to a generated value; `build()` returns a `Gen<Response<NNN>>`.
     */
    private fun community.flock.wirespec.ir.core.FileBuilder.buildResponseScope(shape: EndpointShape, variant: EndpointShape.ResponseVariantShape) {
        val scopeName = "${shape.name}${variant.className}Scope"
        val variantType = "${shape.name}.${variant.className}"
        val hasBody = variant.bodyKind != EndpointShape.BodyKind.None && variant.bodyType != null

        struct(scopeName) {
            plainClass()
            annotation("@WirespecScenarioDsl")
            visibility(Visibility.PUBLIC)
            constructorVisibility(Visibility.INTERNAL)
            raw("private val inner = responseCall(${shape.name}, $variantType::class)")
            if (hasBody) {
                property("body", genNullableOf(variant.bodyType!!), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("null"))
            }
            variant.headerFields.forEach { f ->
                property(f.name, genNullableOf(f.kotlinType), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("null"))
            }
            function("build") {
                visibility(Visibility.PUBLIC)
                returnType(genOf(variantType))
                if (hasBody) {
                    raw("body?.let { inner.body(it) }")
                }
                variant.headerFields.forEach { f ->
                    raw("${KotlinIdentifier.escape(f.name)}?.let { inner.headerGen(\"${f.name}\", it) }")
                }
                raw("@Suppress(\"UNCHECKED_CAST\")")
                returns(rawExpr("inner.buildGen() as Gen<$variantType>"))
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Scope class
    // ------------------------------------------------------------------------------------

    private fun community.flock.wirespec.ir.core.FileBuilder.buildScopeClass(shape: EndpointShape, present: List<Slot>, required: List<Slot>) {
        struct("${shape.name}Scope") {
            plainClass()
            annotation("@WirespecScenarioDsl")
            visibility(Visibility.PUBLIC)
            constructorVisibility(Visibility.INTERNAL)
            raw("private val inner = endpointCall(${shape.name}.Handler, ${shape.name})")
            // Each slot is a private `var` holding a builder lambda, set only via its same-named
            // function form (`path { … }`); the `var` is hidden so the DSL admits just that form.
            present.forEach { slot ->
                val builderClass = slotBuilderName(shape, slot)
                property(slot.name, IrType.Nullable(blockType(builderClass)), isMutable = true, visibility = Visibility.PRIVATE, initializer = rawExpr("null"))
                function(slot.name) {
                    visibility(Visibility.PUBLIC)
                    arg("block", blockType(builderClass))
                    raw("this.${slot.name} = block")
                }
            }
            if (shape.bodyFieldShapes.isNotEmpty()) {
                val builderName = RecordBuilder.builderName(shape.bodyElementType ?: error("bodyFieldShapes present but no bodyElementType for ${shape.name}"))
                property("body", IrType.Nullable(blockType(builderName)), isMutable = true, visibility = Visibility.PRIVATE, initializer = rawExpr("null"))
                function("body") {
                    visibility(Visibility.PUBLIC)
                    arg("block", blockType(builderName))
                    raw("this.body = block")
                }
                if (shape.bodyKind == EndpointShape.BodyKind.List) {
                    property("bodyCount", IrType.Custom("IntRange"), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("1..3"))
                }
            }
            property("flushed", IrType.Boolean, isMutable = true, visibility = Visibility.PRIVATE, initializer = rawExpr("false"))
            function("flush") {
                visibility(Visibility.PRIVATE)
                raw(renderFlushBody(shape, present, required).trimEnd())
            }
            // `buildRequest()` flushes (validating required slots eagerly) then returns a `Gen` that
            // materialises the typed request on each draw; it is the terminal behind the
            // `generate.request { … }` entry point. Sending happens through `Gen<Request>.call()`.
            function("buildRequest") {
                visibility(Visibility.PUBLIC)
                returnType(genOf("${shape.name}.Request"))
                raw("flush()")
                returns(rawExpr("inner.buildRequestGen()"))
            }
        }
    }

    /**
     * Body of `flush()`: applies the assigned slot/body lambdas to the runtime call and validates
     * required values. Idempotent (guarded by `flushed`) so repeated terminal calls are safe.
     */
    private fun renderFlushBody(shape: EndpointShape, present: List<Slot>, required: List<Slot>): String = buildString {
        val requiredNames = required.map { it.name }.toSet()
        appendLine("if (flushed) return")
        appendLine("flushed = true")
        present.forEach { slot ->
            val builderClass = slotBuilderName(shape, slot)
            val builderVar = "${slot.name}Builder"
            if (slot.name in requiredNames) {
                appendLine("val $builderVar = $builderClass().apply(${slot.name} ?: error(\"${shape.name}: required `${slot.name}` block is missing\"))")
                slot.fields.forEach { f -> appendLine(registerLine(shape, slot, builderVar, f, "")) }
            } else {
                appendLine("${slot.name}?.let { block ->")
                appendLine("    val $builderVar = $builderClass().apply(block)")
                slot.fields.forEach { f -> appendLine(registerLine(shape, slot, builderVar, f, "    ")) }
                appendLine("}")
            }
        }
        if (shape.bodyFieldShapes.isNotEmpty()) {
            val builderName = RecordBuilder.builderName(shape.bodyElementType ?: error("bodyFieldShapes present but no bodyElementType for ${shape.name}"))
            val isList = shape.bodyKind == EndpointShape.BodyKind.List
            val elementType = shape.bodyElementType
            appendLine("body?.let { block ->")
            appendLine("    val builder = $builderName().apply(block)")
            if (isList) {
                appendLine("    inner.bodyListSize(Arb.int(bodyCount))")
            }
            // Reconstruct the contract default body (`rawBase`) by copying it with each
            // overridden field replaced; un-set fields keep their generated default. A list
            // body applies the same per-element overrides to every generated element.
            appendLine("    inner.bodyTransform { rawBase, rs ->")
            if (isList) {
                appendLine("        @Suppress(\"UNCHECKED_CAST\")")
                appendLine("        (rawBase as List<$elementType>).map { base ->")
                appendBodyCopy(this, "base", "builder", shape, indent = "            ")
                appendLine("        }")
            } else {
                appendLine("        val base = rawBase as $elementType")
                appendBodyCopy(this, "base", "builder", shape, indent = "        ")
            }
            appendLine("    }")
            appendLine("}")
        }
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

    private fun community.flock.wirespec.ir.core.FileBuilder.buildSlotBuilder(shape: EndpointShape, slot: Slot) {
        struct(slotBuilderName(shape, slot)) {
            plainClass()
            annotation("@WirespecScenarioDsl")
            visibility(Visibility.PUBLIC)
            slot.fields.forEach { f ->
                property(f.name, genNullableOf(f.kotlinType), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("null"))
            }
        }
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
