package community.flock.wirespec.integration.kotest.convert

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FileBuilder
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.generator.escapeKotlinIdentifier
import community.flock.wirespec.ir.core.Type as IrType

/** Builds the per-endpoint block-style Kotest DSL file (`<Endpoint>Dsl.kt`). */
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
            val needsArbConstant = (shape.pathFields + shape.queryFields + shape.headerFields).any { it.isNullable } ||
                present.isNotEmpty() ||
                shape.responseVariants.any { it.bodyType != null || it.headerFields.isNotEmpty() }
            if (isListBody || needsArbConstant) {
                import("io.kotest.property", "Arb")
            }
            if (isListBody) {
                import("io.kotest.property.arbitrary", "int")
            }
            if (needsArbConstant) {
                import("io.kotest.property.arbitrary", "constant")
            }
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

    private fun genOf(inner: IrType): IrType = IrType.Custom("Gen", listOf(inner))
    private fun genOf(inner: String): IrType = genOf(IrType.Custom(inner))
    private fun genNullableOf(inner: IrType): IrType = IrType.Nullable(genOf(inner))
    private fun genNullableOf(inner: String): IrType = genNullableOf(IrType.Custom(inner))
    private fun blockType(builder: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(builder))
    private fun suspendScopeType(scope: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(scope), isAsync = true)

    /** The `<Endpoint>Generate` wrapper grouping the scenario builders. */
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

    /** Per-variant response scope, opened by `generate.responseNNN { … }`. */
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
                property(f.name, genNullableOf(f.type), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("null"))
                valueSetter(f.name, f.type)
            }
            function("build") {
                visibility(Visibility.PUBLIC)
                returnType(genOf(variantType))
                if (bodyType != null) {
                    raw("body?.let { inner.body(it) }")
                }
                variant.headerFields.forEach { f ->
                    raw("${f.name.escapeKotlinIdentifier()}?.let { inner.headerGen(\"${f.name}\", it) }")
                }
                raw("@Suppress(\"UNCHECKED_CAST\")")
                returns(cast(FunctionCall(receiver = VariableReference("inner"), name = Name.of("buildGen")), genOf(variantType)))
            }
        }
    }

    private fun FileBuilder.buildScopeClass(shape: EndpointShape, present: List<Slot>, required: List<Slot>) {
        struct("${shape.name}Scope") {
            plainClass()
            annotation("@WirespecScenarioDsl")
            visibility(Visibility.PUBLIC)
            constructorVisibility(Visibility.INTERNAL)
            raw("private val inner = endpointCall(${shape.name}.Handler, ${shape.name})")
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
            function("buildRequest") {
                visibility(Visibility.PUBLIC)
                returnType(genOf("${shape.name}.Request"))
                raw("flush()")
                returns(rawExpr("inner.buildRequestGen()"))
            }
        }
    }

    /** Body of `flush()`: applies the assigned slot/body lambdas to the runtime call and validates required values. */
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
        val ref = "$builderVar.${field.name.escapeKotlinIdentifier()}"
        val fallback = if (field.isNullable) {
            "Arb.constant(null)"
        } else {
            "error(\"${shape.name}.${slot.name}: required `${field.name}` is missing\")"
        }
        return "${indent}inner.${slot.genFn}(\"${field.name}\", $ref ?: $fallback)"
    }

    private fun FileBuilder.buildSlotBuilder(shape: EndpointShape, slot: Slot) {
        struct(slotBuilderName(shape, slot)) {
            plainClass()
            annotation("@WirespecScenarioDsl")
            visibility(Visibility.PUBLIC)
            slot.fields.forEach { f ->
                property(f.name, genNullableOf(f.type), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("null"))
                valueSetter(f.name, f.type)
            }
        }
    }

    /** Emit `$baseExpr.copy(field = <value>, …)` for a body shape, one field per line. */
    private fun StringBuilder.appendBodyCopy(
        baseExpr: String,
        receiver: String,
        shape: EndpointShape,
        indent: String,
    ) {
        appendLine("$indent$baseExpr.copy(")
        shape.bodyFieldShapes.forEach { f ->
            appendLine("$indent    ${f.name.escapeKotlinIdentifier()} = ${copyValueExpr(f, baseExpr, receiver)},")
        }
        appendLine("$indent)")
    }

    /** A single body field's reconstructed value: a drawn override or the generated default. */
    private fun copyValueExpr(
        field: EndpointShape.BodyFieldShape,
        baseExpr: String,
        receiver: String,
    ): String {
        val fieldRef = field.name.escapeKotlinIdentifier()
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

    /** The reconstructed value for a nested object/list field. */
    private fun nestedCopyExpr(
        fieldName: String,
        nestedTypeName: String,
        fields: List<EndpointShape.BodyFieldShape>,
        baseExpr: String,
        receiver: String,
        isList: Boolean,
    ): String {
        val fieldRef = fieldName.escapeKotlinIdentifier()
        val blockRef = "_${fieldName}Block".escapeKotlinIdentifier()
        val baseField = "$baseExpr.$fieldRef"
        val nestedBuilder = RecordBuilder.builderName(nestedTypeName)
        val nestedVar = "nested_$fieldName".escapeKotlinIdentifier()
        val elemVar = "${if (isList) "elem" else "base"}_$fieldName".escapeKotlinIdentifier()
        val overBase = if (isList) "?.map" else "?.let"
        val subs = fields.joinToString(", ") {
            "${it.name.escapeKotlinIdentifier()} = ${copyValueExpr(it, elemVar, nestedVar)}"
        }
        return "$receiver.$fieldRef?.let { gen -> gen.draw(rs) } ?: " +
            "$receiver.$blockRef?.let { block -> val $nestedVar = $nestedBuilder().apply(block); " +
            "$baseField$overBase { $elemVar -> $elemVar.copy($subs) } } ?: $baseField"
    }

    /** Wrap a drawn body value to match the model field's declared type, re-wrapping refined values. */
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
    private fun EndpointShape.requireBodyElementType(): String = bodyElementType ?: error("bodyFieldShapes present but no bodyElementType for $name")
}

internal data class EndpointShape(
    val name: String,
    val pathFields: List<NamedTypedField>,
    val queryFields: List<NamedTypedField>,
    val headerFields: List<NamedTypedField>,
    val bodyKind: BodyKind,
    /** Name of the body's element Type (`"Pet"` for both object and list bodies); `null` for `BodyKind.None`. */
    val bodyElementType: String?,
    /** Recursive classification of body fields. */
    val bodyFieldShapes: List<BodyFieldShape>,
    /** One entry per declared response variant (`200`, `201`, …), used to build random responses. */
    val responseVariants: List<ResponseVariantShape>,
    val modelImports: List<String>,
) {
    data class NamedTypedField(val name: String, val type: IrType, val isNullable: Boolean = false)

    /** A single response variant (`Response<status>`), carrying enough to build a random instance. */
    data class ResponseVariantShape(
        /** Numeric status, e.g. `"201"`. */
        val status: String,
        /** Generated variant class simple name, e.g. `"Response201"`. */
        val className: String,
        val bodyKind: BodyKind,
        /** IR type of the body, e.g. `TodoDto` or `List<TodoDto>` once rendered. `null` when there is no body. */
        val bodyType: IrType?,
        /** Name of the body's element Type — `"TodoDto"` for both object and list bodies. `null` for no body. */
        val bodyElementType: String?,
        val headerFields: List<NamedTypedField>,
    )

    enum class BodyKind { None, Object, List }

    sealed interface BodyFieldShape {
        val name: String

        /** Leaf body field declared as `Gen<[type]>?` in the typed builder. */
        data class Primitive(
            override val name: String,
            val type: IrType,
            val refinedTypeName: String? = null,
            val isList: Boolean = false,
            val isNullable: Boolean = false,
        ) : BodyFieldShape
        data class NestedObject(
            override val name: String,
            val typeName: String,
            val fields: List<BodyFieldShape>,
        ) : BodyFieldShape
        data class NestedList(
            override val name: String,
            val elementTypeName: String,
            val fields: List<BodyFieldShape>,
        ) : BodyFieldShape
    }

    companion object {
        fun from(
            endpoint: Endpoint,
            types: Map<String, Type> = emptyMap(),
            refined: Map<String, Refined> = emptyMap(),
        ): EndpointShape {
            val pathFields = endpoint.path
                .filterIsInstance<Endpoint.Segment.Param>()
                .map { NamedTypedField(it.identifier.value, it.reference.convert(), it.reference.isNullable) }
            val queryFields = endpoint.queries
                .map { NamedTypedField(it.identifier.value, it.reference.convert(), it.reference.isNullable) }
            val headerFields = endpoint.headers
                .map { NamedTypedField(it.identifier.value, it.reference.convert(), it.reference.isNullable) }
            val bodyRef = endpoint.requests.firstOrNull()?.content?.reference
            val (bodyKind, bodyElementType) = classifyBody(bodyRef)

            val bodyFieldShapes: List<BodyFieldShape> = bodyElementType
                ?.let { extractBodyFields(it, types, refined, visited = emptySet()) }
                ?: emptyList()

            val responseVariants = endpoint.responses
                .mapNotNull { response ->
                    val status = response.status.takeIf { it.all(Char::isDigit) } ?: return@mapNotNull null
                    val respBodyRef = response.content?.reference
                    val respBodyType = respBodyRef?.let { if (it is Reference.Unit) null else it.convert() }
                    val (respBodyKind, respElementName) = classifyBody(respBodyRef)
                    ResponseVariantShape(
                        status = status,
                        className = "Response$status",
                        bodyKind = respBodyKind,
                        bodyType = respBodyType,
                        bodyElementType = respElementName,
                        headerFields = response.headers.map {
                            NamedTypedField(it.identifier.value, it.reference.convert(), it.reference.isNullable)
                        },
                    )
                }
                .distinctBy { it.status }

            val refs = buildList {
                endpoint.path.filterIsInstance<Endpoint.Segment.Param>().forEach { add(it.reference) }
                endpoint.queries.forEach { add(it.reference) }
                endpoint.headers.forEach { add(it.reference) }
                if (bodyRef != null) add(bodyRef)
                endpoint.responses.forEach { response ->
                    response.content?.reference?.let { add(it) }
                    response.headers.forEach { add(it.reference) }
                }
            }
            val bodyFieldRefs = bodyElementType
                ?.let { types[it] }
                ?.shape?.value
                ?.map { it.reference }
                ?: emptyList()
            val modelImports = modelImportsFor(refs + bodyFieldRefs, bodyFieldShapes, types)

            return EndpointShape(
                name = endpoint.identifier.value,
                pathFields = pathFields,
                queryFields = queryFields,
                headerFields = headerFields,
                bodyKind = bodyKind,
                bodyElementType = bodyElementType,
                bodyFieldShapes = bodyFieldShapes,
                responseVariants = responseVariants,
                modelImports = modelImports,
            )
        }

        /** Classifies a request/response body reference into its [BodyKind] and element type name. */
        private fun classifyBody(reference: Reference?): Pair<BodyKind, String?> = when (reference) {
            null, is Reference.Unit -> BodyKind.None to null
            is Reference.Custom -> BodyKind.Object to reference.value
            is Reference.Iterable -> (reference.reference as? Reference.Custom)
                ?.let { BodyKind.List to it.value }
                ?: (BodyKind.None to null)
            else -> BodyKind.None to null
        }

        /** The distinct model type names an operation's DSL file must import. */
        internal fun modelImportsFor(
            refs: List<Reference>,
            fieldShapes: List<BodyFieldShape>,
            types: Map<String, Type>,
        ): List<String> = (
            refs.flatMap(::collectCustomNames) +
                collectNestedTypeNames(fieldShapes) +
                collectFieldTypeNames(fieldShapes, types)
            ).distinct()

        private fun collectCustomNames(reference: Reference): List<String> = when (reference) {
            is Reference.Custom -> listOf(reference.value)
            is Reference.Iterable -> collectCustomNames(reference.reference)
            is Reference.Dict -> collectCustomNames(reference.reference)
            else -> emptyList()
        }

        /** The custom type a nested body field drills into (`null` for a leaf primitive). */
        private val BodyFieldShape.nestedTypeName: String?
            get() = when (this) {
                is BodyFieldShape.NestedObject -> typeName
                is BodyFieldShape.NestedList -> elementTypeName
                is BodyFieldShape.Primitive -> null
            }

        /** The child field shapes of a nested body field (empty for a leaf primitive). */
        private val BodyFieldShape.childFields: List<BodyFieldShape>
            get() = when (this) {
                is BodyFieldShape.NestedObject -> fields
                is BodyFieldShape.NestedList -> fields
                is BodyFieldShape.Primitive -> emptyList()
            }

        private fun collectNestedTypeNames(fields: List<BodyFieldShape>): List<String> = fields.flatMap { f ->
            listOfNotNull(f.nestedTypeName) + collectNestedTypeNames(f.childFields)
        }

        /** Collects the [Reference.Custom] names appearing on nested builder types' own fields. */
        private fun collectFieldTypeNames(
            fields: List<BodyFieldShape>,
            types: Map<String, Type>,
        ): List<String> = fields.flatMap { f ->
            val nestedFieldRefs = f.nestedTypeName
                ?.let { types[it]?.shape?.value?.flatMap { field -> collectCustomNames(field.reference) } }
                ?: emptyList()
            nestedFieldRefs + collectFieldTypeNames(f.childFields, types)
        }

        internal fun extractBodyFields(
            typeName: String,
            types: Map<String, Type>,
            refined: Map<String, Refined>,
            visited: Set<String>,
        ): List<BodyFieldShape> {
            if (typeName in visited) return emptyList()
            val type = types[typeName] ?: return emptyList()
            val nextVisited = visited + typeName
            return type.shape.value.map { field ->
                val name = field.identifier.value
                when (val ref = field.reference) {
                    is Reference.Custom -> if (ref.value in types) {
                        BodyFieldShape.NestedObject(
                            name = name,
                            typeName = ref.value,
                            fields = extractBodyFields(ref.value, types, refined, nextVisited),
                        )
                    } else {
                        primitiveOf(name, ref, refined)
                    }
                    is Reference.Iterable -> {
                        val inner = ref.reference
                        if (inner is Reference.Custom && inner.value in types) {
                            BodyFieldShape.NestedList(
                                name = name,
                                elementTypeName = inner.value,
                                fields = extractBodyFields(inner.value, types, refined, nextVisited),
                            )
                        } else {
                            primitiveOf(name, ref, refined)
                        }
                    }
                    else -> primitiveOf(name, ref, refined)
                }
            }
        }

        /** Build a [BodyFieldShape.Primitive], recording the [Refined] wrapper class (if any). */
        private fun primitiveOf(
            name: String,
            ref: Reference,
            refined: Map<String, Refined>,
        ): BodyFieldShape.Primitive {
            val (refinedTypeName, isList) = when {
                ref is Reference.Custom && ref.value in refined -> ref.value to false
                ref is Reference.Iterable -> {
                    val inner = ref.reference
                    if (inner is Reference.Custom && inner.value in refined) inner.value to true else null to false
                }
                else -> null to false
            }
            return BodyFieldShape.Primitive(
                name = name,
                type = mapWithRefinedUnwrap(ref, refined),
                refinedTypeName = refinedTypeName,
                isList = isList,
                isNullable = ref.isNullable,
            )
        }

        /** Like [Reference.convert], but replaces a `Reference.Custom` to a [Refined] with the refined's underlying primitive type. */
        private fun mapWithRefinedUnwrap(reference: Reference, refined: Map<String, Refined>): IrType = when (reference) {
            is Reference.Custom -> refined[reference.value]
                ?.let { r -> r.reference.copy(isNullable = reference.isNullable).convert() }
                ?: reference.convert()
            is Reference.Iterable -> IrType.Array(mapWithRefinedUnwrap(reference.reference, refined))
                .let { if (reference.isNullable) IrType.Nullable(it) else it }
            is Reference.Dict -> IrType.Dict(IrType.String, mapWithRefinedUnwrap(reference.reference, refined))
                .let { if (reference.isNullable) IrType.Nullable(it) else it }
            else -> reference.convert()
        }
    }
}
