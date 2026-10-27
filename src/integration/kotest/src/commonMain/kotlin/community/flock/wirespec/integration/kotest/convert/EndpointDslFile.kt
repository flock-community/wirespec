package community.flock.wirespec.integration.kotest.convert

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.FileBuilder
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.Visibility
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.file
import community.flock.wirespec.compiler.core.ir.generator.escapeKotlinIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.compiler.core.ir.Type as IrType

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
            if (shape.bodyKind == EndpointShape.BodyKind.Primitive) {
                import("community.flock.wirespec.integration.kotest.dsl", "draw")
            }
            import(endpointPkg, shape.name)
            import("io.kotest.property", "Gen")
            import("io.kotest.property", "Arb")
            val isListBody = shape.bodyKind == EndpointShape.BodyKind.List
            val needsArbConstant = (shape.pathFields + shape.queryFields + shape.headerFields).any { it.isNullable } ||
                present.isNotEmpty() ||
                shape.bodyKind == EndpointShape.BodyKind.Primitive ||
                shape.responseVariants.any { it.bodyType != null || it.headerFields.isNotEmpty() }
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

    private fun requiredSlots(shape: EndpointShape): List<Slot> = presentSlots(shape).filter { slot -> slot.fields.any { !it.isNullable } }

    private fun cap(name: String): String = name.replaceFirstChar(Char::uppercaseChar)

    private fun slotBuilderName(shape: EndpointShape, slot: Slot): String = "${shape.name}${cap(slot.name)}Builder"

    private fun genOf(inner: IrType): IrType = IrType.Custom("Gen", listOf(inner))
    private fun genOf(inner: String): IrType = genOf(IrType.Custom(inner))
    private fun arbOf(inner: IrType): IrType = IrType.Custom("Arb", listOf(inner))
    private fun arbOf(inner: String): IrType = arbOf(IrType.Custom(inner))
    private fun genNullableOf(inner: IrType): IrType = IrType.Nullable(genOf(inner))
    private fun blockType(builder: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(builder))
    private fun suspendScopeType(scope: String): IrType.Function = IrType.Function(emptyList(), IrType.Unit, IrType.Custom(scope), isAsync = true)

    private fun FileBuilder.buildGenerateWrapper(shape: EndpointShape) {
        struct("${shape.name}Generate") {
            plainClass()
            visibility(Visibility.PUBLIC)
            constructorVisibility(Visibility.INTERNAL)
            asyncFunction("request") {
                visibility(Visibility.PUBLIC)
                arg("block", suspendScopeType("${shape.name}Scope"))
                returnType(arbOf("${shape.name}.Request"))
                assign("scope", FunctionCall(name = Name.of("${shape.name}Scope")))
                functionCall("block", receiver = VariableReference("scope"))
                returns(FunctionCall(receiver = VariableReference("scope"), name = Name.of("buildRequest")))
            }
            shape.responseVariants.forEach { variant ->
                val scopeName = "${shape.name}${variant.className}Scope"
                function("response${variant.status}") {
                    visibility(Visibility.PUBLIC)
                    arg("block", blockType(scopeName), rawExpr("{}"))
                    returnType(arbOf("${shape.name}.${variant.className}"))
                    returns(rawExpr("$scopeName().apply(block).build()"))
                }
            }
        }
    }

    private fun FileBuilder.buildRequestCall(shape: EndpointShape) {
        asyncFunction("call") {
            visibility(Visibility.PUBLIC)
            receiver(genOf("${shape.name}.Request"))
            returnType(IrType.Custom("${shape.name}.Response<*>"))
            returns(rawExpr("requestCall(${shape.name}.api, ${shape.name}, this)"))
        }
    }

    private fun FileBuilder.buildResponseMock(shape: EndpointShape) {
        asyncFunction("mock") {
            visibility(Visibility.PUBLIC)
            receiver(genOf("${shape.name}.Response<*>"))
            arg("predicate", IrType.Function(listOf(IrType.Custom("${shape.name}.Request")), IrType.Boolean))
            returnType(IrType.Unit)
            returns(rawExpr("responseMock(${shape.name}.api, this, predicate)"))
        }
    }

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
                returnType(arbOf(variantType))
                if (bodyType != null) {
                    raw("body?.let { inner.body(it) }")
                }
                variant.headerFields.forEach { f ->
                    raw("${f.name.escapeKotlinIdentifier()}?.let { inner.headerGen(\"${f.name}\", it) }")
                }
                raw("@Suppress(\"UNCHECKED_CAST\")")
                returns(cast(FunctionCall(receiver = VariableReference("inner"), name = Name.of("buildGen")), arbOf(variantType)))
            }
        }
    }

    private fun FileBuilder.buildScopeClass(shape: EndpointShape, present: List<Slot>, required: List<Slot>) {
        struct("${shape.name}Scope") {
            plainClass()
            annotation("@WirespecScenarioDsl")
            visibility(Visibility.PUBLIC)
            constructorVisibility(Visibility.INTERNAL)
            raw("private val inner = endpointCall(${shape.name}.api, ${shape.name})")
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
            if (shape.bodyKind == EndpointShape.BodyKind.Primitive) {
                val bodyType = shape.requireBodyPrimitiveType()
                property("body", genNullableOf(bodyType), isMutable = true, visibility = Visibility.PUBLIC, initializer = rawExpr("null"))
                valueSetter("body", bodyType)
            }
            property("flushed", IrType.Boolean, isMutable = true, visibility = Visibility.PRIVATE, initializer = rawExpr("false"))
            function("flush") {
                visibility(Visibility.PRIVATE)
                raw(renderFlushBody(shape, present, required).trimEnd())
            }
            function("buildRequest") {
                visibility(Visibility.PUBLIC)
                returnType(arbOf("${shape.name}.Request"))
                raw("flush()")
                returns(rawExpr("inner.buildRequestGen()"))
            }
        }
    }

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
                appendLine("val $builderVar = ${slot.name}?.let { block -> $builderClass().apply(block) }")
                slot.fields.forEach { f -> appendLine(registerLine(shape, slot, builderVar, f, "", nullableBuilder = true)) }
            }
        }
        if (shape.bodyFieldShapes.isNotEmpty()) {
            val elementType = shape.requireBodyElementType()
            val isList = shape.bodyKind == EndpointShape.BodyKind.List
            val body = RecordRef(elementType, isList = isList, isNullable = false, isElementNullable = false)
            appendLine("body?.let { block ->")
            appendLine("    val builder = ${RecordBuilder.builderName(elementType)}().apply(block)")
            if (isList) {
                appendLine("    inner.bodyListSize(Arb.int(bodyCount))")
            }
            appendLine("    inner.bodyTransform { base, rs -> ${body.applyBuilder("base", "builder")} }")
            appendLine("}")
        }
        if (shape.bodyKind == EndpointShape.BodyKind.Primitive) {
            appendLine("body?.let { gen ->")
            appendLine("    inner.bodyTransform { _, rs -> gen.draw(rs) }")
            appendLine("}")
        }
    }

    private fun registerLine(
        shape: EndpointShape,
        slot: Slot,
        builderVar: String,
        field: EndpointShape.NamedTypedField,
        indent: String,
        nullableBuilder: Boolean = false,
    ): String {
        val access = if (nullableBuilder) "?." else "."
        val ref = "$builderVar$access${field.name.escapeKotlinIdentifier()}"
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

    private fun EndpointShape.requireBodyElementType(): String = bodyElementType ?: error("bodyFieldShapes present but no bodyElementType for $name")

    private fun EndpointShape.requireBodyPrimitiveType(): IrType = bodyPrimitiveType ?: error("BodyKind.Primitive but no bodyPrimitiveType for $name")
}

internal data class EndpointShape(
    val name: String,
    val pathFields: List<NamedTypedField>,
    val queryFields: List<NamedTypedField>,
    val headerFields: List<NamedTypedField>,
    val bodyKind: BodyKind,
    val bodyElementType: String?,
    val bodyPrimitiveType: IrType?,
    val bodyFieldShapes: List<BodyFieldShape>,
    val responseVariants: List<ResponseVariantShape>,
    val modelImports: List<String>,
) {
    data class NamedTypedField(val name: String, val type: IrType, val isNullable: Boolean = false)

    data class ResponseVariantShape(
        val status: String,
        val className: String,
        val bodyKind: BodyKind,
        val bodyType: IrType?,
        val bodyElementType: String?,
        val headerFields: List<NamedTypedField>,
    )

    enum class BodyKind { None, Object, List, Primitive }

    sealed interface BodyFieldShape {
        val name: String

        /** Pinned by [type], the field's type with every refined type replaced by its inner primitive. */
        data class Primitive(
            override val name: String,
            val type: IrType,
            val refinement: Refinement?,
        ) : BodyFieldShape

        /** Pinned as a whole value, or field by field with the record's own builder. */
        data class Nested(
            override val name: String,
            val record: RecordRef,
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
            val queryFields = endpoint.queries.fields
                .map { NamedTypedField(it.identifier.value, it.reference.convert(), it.reference.isNullable) }
            val headerFields = endpoint.headers.fields
                .map { NamedTypedField(it.identifier.value, it.reference.convert(), it.reference.isNullable) }
            val bodyRef = endpoint.requests.firstOrNull()?.content?.reference
            val (bodyKind, bodyElementType) = classifyBody(bodyRef)
            val bodyPrimitiveType = bodyRef
                ?.takeIf { bodyKind == BodyKind.Primitive }
                ?.convert()
                ?.let { if (it is IrType.Nullable) it.type else it }

            val bodyFieldShapes: List<BodyFieldShape> = bodyElementType
                ?.let { extractBodyFields(it, types, refined) }
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
                        headerFields = response.headers.fields.map {
                            NamedTypedField(it.identifier.value, it.reference.convert(), it.reference.isNullable)
                        },
                    )
                }
                .distinctBy { it.status }

            val refs = buildList {
                endpoint.path.filterIsInstance<Endpoint.Segment.Param>().forEach { add(it.reference) }
                endpoint.queries.fields.forEach { add(it.reference) }
                endpoint.headers.fields.forEach { add(it.reference) }
                if (bodyRef != null) add(bodyRef)
                endpoint.responses.forEach { response ->
                    response.content?.reference?.let { add(it) }
                    response.headers.fields.forEach { add(it.reference) }
                }
            }
            val modelImports = modelImportsFor(refs)

            return EndpointShape(
                name = Name.of(endpoint.identifier.value).pascalCase(),
                pathFields = pathFields,
                queryFields = queryFields,
                headerFields = headerFields,
                bodyKind = bodyKind,
                bodyElementType = bodyElementType,
                bodyPrimitiveType = bodyPrimitiveType,
                bodyFieldShapes = bodyFieldShapes,
                responseVariants = responseVariants,
                modelImports = modelImports,
            )
        }

        private fun classifyBody(reference: Reference?): Pair<BodyKind, String?> = when (reference) {
            null, is Reference.Unit -> BodyKind.None to null
            is Reference.Custom -> BodyKind.Object to reference.value
            is Reference.Iterable -> (reference.reference as? Reference.Custom)
                ?.let { BodyKind.List to it.value }
                ?: (BodyKind.None to null)
            is Reference.Primitive -> BodyKind.Primitive to null
            else -> BodyKind.None to null
        }

        internal fun modelImportsFor(refs: List<Reference>): List<String> = refs.flatMap(::collectCustomNames).distinct()

        internal fun collectCustomNames(reference: Reference): List<String> = when (reference) {
            is Reference.Custom -> listOf(reference.value)
            is Reference.Iterable -> collectCustomNames(reference.reference)
            is Reference.Dict -> collectCustomNames(reference.reference)
            else -> emptyList()
        }

        internal fun extractBodyFields(
            typeName: String,
            types: Map<String, Type>,
            refined: Map<String, Refined>,
        ): List<BodyFieldShape> = types[typeName]?.shape?.fields.orEmpty().map { field ->
            RecordRef.of(field.reference, types)
                ?.let { BodyFieldShape.Nested(field.identifier.value, it) }
                ?: BodyFieldShape.Primitive(
                    name = field.identifier.value,
                    type = mapWithRefinedUnwrap(field.reference, refined),
                    refinement = Refinement.of(field.reference, refined),
                )
        }

        private fun mapWithRefinedUnwrap(reference: Reference, refined: Map<String, Refined>): IrType = when (reference) {
            is Reference.Custom -> refined[reference.value]?.reference?.copy(isNullable = reference.isNullable)?.convert()
                ?: reference.convert()
            is Reference.Iterable -> IrType.Array(mapWithRefinedUnwrap(reference.reference, refined))
                .let { if (reference.isNullable) IrType.Nullable(it) else it }
            is Reference.Dict -> IrType.Dict(IrType.String, mapWithRefinedUnwrap(reference.reference, refined))
                .let { if (reference.isNullable) IrType.Nullable(it) else it }
            else -> reference.convert()
        }
    }
}
