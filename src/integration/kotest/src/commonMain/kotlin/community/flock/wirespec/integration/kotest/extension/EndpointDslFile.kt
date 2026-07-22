package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FileBuilder
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.Type as IrType

/**
 * Builds the per-endpoint block-style Kotest DSL file (`<Endpoint>Dsl.kt`): the
 * `<Endpoint>Generate` class and its `generate` extension property, the `Gen<Request>.call()` /
 * `Gen<Response<*>>.mock()` extensions, the request `<Endpoint>Scope`, per-variant response
 * scopes, and slot builders.
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
 * `flush()` (run by `buildRequest()`) validates required slots/fields and throws a clear error
 * at scenario start. Kotlin cannot require a `var` be assigned inside a lambda, so this
 * enforcement is runtime, not compile-time; nullable fields default to `Arb.constant(null)`.
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
            import("community.flock.wirespec.integration.kotest.dsl", "requestCall")
            import("community.flock.wirespec.integration.kotest.dsl", "WirespecScenarioDsl")
            if (shape.responseVariants.isNotEmpty()) {
                import("community.flock.wirespec.integration.kotest.dsl", "responseCall")
                import("community.flock.wirespec.integration.kotest.dsl", "responseMock")
            }
            if (shape.bodyFieldShapes.isNotEmpty()) {
                import("community.flock.wirespec.integration.kotest.dsl", "draw")
            }
            import(endpointPkg, shape.name)
            import("io.kotest.property", "Gen")
            val isListBody = shape.bodyKind == EndpointShape.BodyKind.List
            // `Arb.constant` backs both the nullable-slot fallback and every `<field>(value)` setter
            // emitted next to a `Gen<…>?` slot (see [valueSetter]).
            val needsArbConstant = (shape.pathFields + shape.queryFields + shape.headerFields).any { it.isNullable } ||
                present.isNotEmpty() ||
                shape.responseVariants.any { it.bodyType != null || it.headerFields.isNotEmpty() }
            if (isListBody || needsArbConstant) {
                import("io.kotest.property", "Arb")
            }
            if (isListBody) {
                // Arb.int is an extension on Arb.Companion in io.kotest.property.arbitrary.
                import("io.kotest.property.arbitrary", "int")
            }
            if (needsArbConstant) {
                import("io.kotest.property.arbitrary", "constant")
            }
            // Model class names are pascalCased by the Kotlin IR emitter (KotlinIrTransformer.buildModelImports);
            // apply the same normalisation so underscore-bearing types (e.g. HAL `Contact_embedded`) resolve.
            shape.modelImports.forEach { import(modelPkg, Name.of(it).pascalCase()) }

            buildGenerateWrapper(shape)
            property(
                name = "generate",
                type = IrType.Custom("${shape.name}Generate"),
                visibility = Visibility.PUBLIC,
                receiver = IrType.Custom(shape.name),
                getter = FunctionCall(name = Name.of("${shape.name}Generate")),
            )
            buildRequestCall(shape)
            if (shape.responseVariants.isNotEmpty()) {
                buildResponseMock(shape)
            }
            buildScopeClass(shape, present, required)
            shape.responseVariants.forEach { variant -> buildResponseScope(shape, variant) }
            present.forEach { slot -> buildSlotBuilder(shape, slot) }
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
    private fun suspendScopeType(scope: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(scope), isAsync = true)

    // ------------------------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------------------------

    /**
     * The `<Endpoint>Generate` wrapper grouping the scenario builders: `request` opens the
     * `<Endpoint>Scope` returning a `Gen<Request>`; each `response<NNN>` returns a `Gen<Response<NNN>>`.
     */
    private fun FileBuilder.buildGenerateWrapper(shape: EndpointShape) {
        struct("${shape.name}Generate") {
            plainClass()
            visibility(Visibility.PUBLIC)
            constructorVisibility(Visibility.INTERNAL)
            asyncFunction("request") {
                visibility(Visibility.PUBLIC)
                arg("block", suspendScopeType("${shape.name}Scope"))
                returnType(genOf("${shape.name}.Request"))
                assign("scope", FunctionCall(name = Name.of("${shape.name}Scope")))
                functionCall("block", receiver = VariableReference("scope"))
                returns(FunctionCall(receiver = VariableReference("scope"), name = Name.of("buildRequest")))
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

    /** A `call()` extension on `Gen<Request>`: `PutTodo.generate.request { … }.call()`. */
    private fun FileBuilder.buildRequestCall(shape: EndpointShape) {
        asyncFunction("call") {
            visibility(Visibility.PUBLIC)
            receiver(genOf("${shape.name}.Request"))
            returnType(IrType.Custom("${shape.name}.Response<*>"))
            returns(rawExpr("requestCall(${shape.name}.Handler, ${shape.name}, this)"))
        }
    }

    /** A `mock()` extension on `Gen<Response<*>>`: `PutTodo.generate.response200 { … }.mock { req -> req.path.id == "1" }`. */
    private fun FileBuilder.buildResponseMock(shape: EndpointShape) {
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
     * Per-variant response scope, opened by `generate.responseNNN { … }`: a whole-value `body`
     * setter (when the variant has a body) and one `Gen<…>?` setter per response header field;
     * `build()` returns a `Gen<Response<NNN>>`.
     */
    private fun FileBuilder.buildResponseScope(shape: EndpointShape, variant: EndpointShape.ResponseVariantShape) {
        val scopeName = "${shape.name}${variant.className}Scope"
        val variantType = "${shape.name}.${variant.className}"
        val bodyType = variant.bodyType?.takeIf { variant.bodyKind != EndpointShape.BodyKind.None }

        struct(scopeName) {
            plainClass()
            annotation("@WirespecScenarioDsl")
            visibility(Visibility.PUBLIC)
            constructorVisibility(Visibility.INTERNAL)
            raw("private val inner = responseCall(${shape.name}, $variantType::class)")
            if (bodyType != null) {
                property("body", genNullableOf(bodyType), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("null"))
                valueSetter("body", bodyType)
            }
            variant.headerFields.forEach { f ->
                property(f.name, genNullableOf(f.kotlinType), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("null"))
                valueSetter(f.name, f.kotlinType)
            }
            function("build") {
                visibility(Visibility.PUBLIC)
                returnType(genOf(variantType))
                if (bodyType != null) {
                    raw("body?.let { inner.body(it) }")
                }
                variant.headerFields.forEach { f ->
                    raw("${KotlinIdentifier.escape(f.name)}?.let { inner.headerGen(\"${f.name}\", it) }")
                }
                raw("@Suppress(\"UNCHECKED_CAST\")")
                returns(cast(FunctionCall(receiver = VariableReference("inner"), name = Name.of("buildGen")), genOf(variantType)))
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Scope class
    // ------------------------------------------------------------------------------------

    private fun FileBuilder.buildScopeClass(shape: EndpointShape, present: List<Slot>, required: List<Slot>) {
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
                val builderName = RecordBuilder.builderName(shape.requireBodyElementType())
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
            val builderName = RecordBuilder.builderName(shape.requireBodyElementType())
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
                appendBodyCopy("base", "builder", shape, indent = "            ")
                appendLine("        }")
            } else {
                appendLine("        val base = rawBase as $elementType")
                appendBodyCopy("base", "builder", shape, indent = "        ")
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

    private fun FileBuilder.buildSlotBuilder(shape: EndpointShape, slot: Slot) {
        struct(slotBuilderName(shape, slot)) {
            plainClass()
            annotation("@WirespecScenarioDsl")
            visibility(Visibility.PUBLIC)
            slot.fields.forEach { f ->
                property(f.name, genNullableOf(f.kotlinType), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("null"))
                valueSetter(f.name, f.kotlinType)
            }
        }
    }

    /**
     * Emit `$baseExpr.copy(field = <value>, …)` for a body shape, one field per line.
     * Each value is produced by [copyValueExpr] — an override drawn from the builder's
     * `Gen`, falling back to the generated default carried on [baseExpr].
     */
    private fun StringBuilder.appendBodyCopy(
        baseExpr: String,
        receiver: String,
        shape: EndpointShape,
        indent: String,
    ) {
        appendLine("$indent$baseExpr.copy(")
        shape.bodyFieldShapes.forEach { f ->
            appendLine("$indent    ${KotlinIdentifier.escape(f.name)} = ${copyValueExpr(f, baseExpr, receiver)},")
        }
        appendLine("$indent)")
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
        val baseField = "$baseExpr.$fieldRef"
        return when (field) {
            is EndpointShape.BodyFieldShape.Primitive ->
                "$receiver.$fieldRef?.let { gen -> ${wrapDrawn("gen.draw(rs)", field)} } ?: $baseField"
            is EndpointShape.BodyFieldShape.NestedObject ->
                nestedCopyExpr(field.name, field.typeName, field.fields, baseExpr, receiver, isList = false)
            is EndpointShape.BodyFieldShape.NestedList ->
                nestedCopyExpr(field.name, field.elementTypeName, field.fields, baseExpr, receiver, isList = true)
        }
    }

    /**
     * The reconstructed value for a nested object/list field: draw the whole-value override `Gen`
     * if set, else apply the sub-builder block over the generated default (via `?.let` for an
     * object, `?.map` for a list), else fall back to the default unchanged.
     */
    private fun nestedCopyExpr(
        fieldName: String,
        nestedTypeName: String,
        fields: List<EndpointShape.BodyFieldShape>,
        baseExpr: String,
        receiver: String,
        isList: Boolean,
    ): String {
        val fieldRef = KotlinIdentifier.escape(fieldName)
        val blockRef = KotlinIdentifier.escape("_${fieldName}Block")
        val baseField = "$baseExpr.$fieldRef"
        val nestedBuilder = RecordBuilder.builderName(nestedTypeName)
        val nestedVar = KotlinIdentifier.escape("nested_$fieldName")
        val elemVar = KotlinIdentifier.escape("${if (isList) "elem" else "base"}_$fieldName")
        val overBase = if (isList) "?.map" else "?.let"
        val subs = fields.joinToString(", ") {
            "${KotlinIdentifier.escape(it.name)} = ${copyValueExpr(it, elemVar, nestedVar)}"
        }
        return "$receiver.$fieldRef?.let { gen -> gen.draw(rs) } ?: " +
            "$receiver.$blockRef?.let { block -> val $nestedVar = $nestedBuilder().apply(block); " +
            "$baseField$overBase { $elemVar -> $elemVar.copy($subs) } } ?: $baseField"
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

    /** The element type backing this endpoint's body-field shapes; present whenever `bodyFieldShapes` is. */
    private fun EndpointShape.requireBodyElementType(): String =
        bodyElementType ?: error("bodyFieldShapes present but no bodyElementType for $name")
}
