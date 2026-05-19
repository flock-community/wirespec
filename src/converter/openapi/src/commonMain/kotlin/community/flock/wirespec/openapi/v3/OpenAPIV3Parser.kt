package community.flock.wirespec.openapi.v3

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.kotlinx.openapi.bindings.BooleanValue
import community.flock.kotlinx.openapi.bindings.OpenAPIV3
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Boolean
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV30HeaderOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Link
import community.flock.kotlinx.openapi.bindings.OpenAPIV30LinkOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Links
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Model
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Operation
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV30ParameterLocation
import community.flock.kotlinx.openapi.bindings.OpenAPIV30ParameterOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30PathItem
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Reference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30RequestBody
import community.flock.kotlinx.openapi.bindings.OpenAPIV30RequestBodyOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV30ResponseOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV30SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30SchemaOrReferenceOrBoolean
import community.flock.kotlinx.openapi.bindings.OpenAPIV30SingleType
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Type
import community.flock.kotlinx.openapi.bindings.OpenAPIV30TypeArray
import community.flock.kotlinx.openapi.bindings.OpenAPIV30TypeDefinition
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Model
import community.flock.kotlinx.openapi.bindings.Path
import community.flock.kotlinx.openapi.bindings.StatusCode
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.openapi.common.LinkInfo
import community.flock.wirespec.openapi.common.className
import community.flock.wirespec.openapi.common.flatMapRequests
import community.flock.wirespec.openapi.common.flatMapResponses
import community.flock.wirespec.openapi.common.getReference
import community.flock.wirespec.openapi.common.isParam
import community.flock.wirespec.openapi.common.jsonDefault
import community.flock.wirespec.openapi.common.parseOpenApi
import community.flock.wirespec.openapi.common.resolveEndpointNameCollisions
import community.flock.wirespec.openapi.common.sanitize
import community.flock.wirespec.openapi.common.toAnnotation
import community.flock.wirespec.openapi.common.toDescriptionAnnotationList
import community.flock.wirespec.openapi.common.toDict
import community.flock.wirespec.openapi.common.toIterable
import community.flock.wirespec.openapi.common.toName
import community.flock.wirespec.openapi.common.toOperationList
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object OpenAPIV3Parser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = parseOpenApi(moduleContent) { source ->
        // 3.1 and 3.2 documents are converted to a 3.0-equivalent JSON shape so the
        // existing V30-typed parser can ingest them. This is what surfaces 3.1
        // type-array nullability (`type: ["string", "null"]`) as `nullable: true`.
        val normalized = OpenAPIV3Normalizer.normalize(source)
        OpenAPIV3(jsonDefault(strict))
            .decodeFromString(normalized)
            .parse()
    }

    fun OpenAPIV3Model.parse(): NonEmptyList<Definition> = when (this) {
        is OpenAPIV30Model -> parse()
        else -> error(
            "OpenAPIV3Parser only consumes 3.0 documents directly. " +
                "Call OpenAPIV3Parser.parse(ModuleContent, Boolean) on the raw source so 3.1/3.2 inputs are normalized first.",
        )
    }

    fun OpenAPIV30Model.parse(): NonEmptyList<Definition> = listOf(
        parseEndpoints(),
        parseParameters(),
        parseRequestBody(),
        parseResponseBody(),
        parseDefinitions(),
    ).reduce(List<Definition>::plus)
        .resolveEndpointNameCollisions()
        .toNonEmptyListOrNull()
        .let { requireNotNull(it) { "Cannot yield empty AST for OpenAPI v3" } }
}

internal fun OpenAPIV30TypeDefinition?.toV30Type(): OpenAPIV30Type? = when (this) {
    is OpenAPIV30SingleType -> value
    is OpenAPIV30TypeArray -> values.firstOrNull { it != OpenAPIV30Type.NULL }
    null -> null
}

private fun OpenAPIV30Schema.typeOrNull(): OpenAPIV30Type? = type.toV30Type()

private fun OpenAPIV30Model.parseEndpoints(): List<Definition> = paths.orEmpty()
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList()
            .map { (method, operation) -> method to operation as OpenAPIV30Operation }
            .map { (method, operation) ->
                val parameters = resolveParameters(pathItem.parameters) + resolveParameters(operation.parameters)
                val segments = toSegments(path, parameters, operation, method)
                val name = operation.toName() ?: (path.toName() + method.name)
                val query = parameters
                    .filter { it.`in` == OpenAPIV30ParameterLocation.QUERY }
                    .map { toField(it, className(name, "Parameter", it.name)) }
                val headers = parameters
                    .filter { it.`in` == OpenAPIV30ParameterLocation.HEADER }
                    .map { toField(it, className(name, "Parameter", it.name)) }
                val requests = operation.requestBody?.let { resolve(it) }
                    ?.let { requestBody ->
                        val isNullable = false
                        requestBody.content?.map { (mediaType, mediaObject) ->
                            val reference = when (val schema = mediaObject.schema) {
                                is OpenAPIV30Reference -> toReference(schema, isNullable)
                                is OpenAPIV30Schema -> toReference(schema, isNullable, className(name, "RequestBody"))
                                null -> null
                            }
                            reference?.let {
                                Endpoint.Request(
                                    Endpoint.Content(
                                        type = mediaType.value,
                                        reference = reference,
                                    ),
                                )
                            } ?: Endpoint.Request(null)
                        }
                    }
                    ?: listOf(Endpoint.Request(null))

                val responses = operation.responses.orEmpty().flatMap { (status, res) ->
                    resolve(res).let { response ->
                        if (response.content.isNullOrEmpty()) {
                            listOf(
                                Endpoint.Response(
                                    annotations = response.description.toDescriptionAnnotationList() +
                                        toLinkAnnotationList(response.links),
                                    status = status.value,
                                    headers = response.headers?.map { entry ->
                                        toField(resolve(entry.value), entry.key, className(name, "ResponseHeader"))
                                    }.orEmpty(),
                                    content = null,
                                ),
                            )
                        } else {
                            response.content?.map { (contentType, media) ->
                                val isNullable = media.schema?.let { resolve(it) }?.nullable ?: false
                                Endpoint.Response(
                                    annotations = response.description.toDescriptionAnnotationList() +
                                        toLinkAnnotationList(response.links),
                                    status = status.value,
                                    headers = response.headers?.map { entry ->
                                        toField(resolve(entry.value), entry.key, className(name, "ResponseHeader"))
                                    }.orEmpty(),
                                    content = Endpoint.Content(
                                        type = contentType.value,
                                        reference = when (val schema = media.schema) {
                                            is OpenAPIV30Reference -> toReference(schema, isNullable)
                                            is OpenAPIV30Schema -> toReference(
                                                schema,
                                                isNullable,
                                                className(name, status.value, "ResponseBody"),
                                            )

                                            null -> Reference.Any(isNullable)
                                        },
                                    ),
                                )
                            }
                        }
                    }
                        ?: listOf(
                            Endpoint.Response(
                                status = status.value,
                                headers = emptyList(),
                                content = null,
                                annotations = emptyList(),
                            ),
                        )
                }

                Endpoint(
                    comment = null,
                    annotations = operation.description.toDescriptionAnnotationList(),
                    identifier = DefinitionIdentifier(name),
                    method = method,
                    path = segments,
                    queries = query,
                    headers = headers,
                    requests = requests,
                    responses = responses,
                )
            }
    }

private fun OpenAPIV30Model.parseParameters(): List<Definition> = flatMapRequests {
    val parameters = resolveParameters((pathItem as OpenAPIV30PathItem).parameters) + resolveParameters((operation as OpenAPIV30Operation).parameters)
    val name = operation.toName() ?: (path.toName() + method.name)
    parameters.flatMap { parameter ->
        parameter.schema?.let { flatten(it, className(name, "Parameter", parameter.name)) } ?: emptyList()
    }
}

private fun OpenAPIV30Model.parseRequestBody(): List<Definition> = flatMapRequests {
    val name = (operation as OpenAPIV30Operation).toName() ?: (path.toName() + method.name)
    operation.requestBody?.let { resolve(it) }?.content.orEmpty()
        .flatMap { (_, mediaObject) ->
            when (val schema = mediaObject.schema) {
                is OpenAPIV30Schema -> when (schema.typeOrNull()) {
                    null, OpenAPIV30Type.OBJECT -> flatten(schema, className(name, "RequestBody"))

                    OpenAPIV30Type.ARRAY -> schema.items?.let { flatten(it, className(name, "RequestBody")) }.orEmpty()

                    else -> emptyList()
                }

                is OpenAPIV30Reference, null -> emptyList()
            }
        }
}

private fun OpenAPIV30Model.flatMapResponse(
    response: OpenAPIV30Response,
    name: String,
    statusCode: StatusCode,
): List<Definition> = response.content.orEmpty()
    .flatMap { (_, mediaObject) ->
        when (val schema = mediaObject.schema) {
            is OpenAPIV30Schema -> when (schema.typeOrNull()) {
                null, OpenAPIV30Type.OBJECT -> flatten(
                    schema,
                    className(name, statusCode.value, "ResponseBody"),
                )

                OpenAPIV30Type.ARRAY -> schema.items?.let {
                    flatten(
                        it,
                        className(name, statusCode.value, "ResponseBody"),
                    )
                }.orEmpty()

                else -> emptyList()
            }

            else -> emptyList()
        }
    }

private fun OpenAPIV30Model.parseResponseBody(): List<Definition> = flatMapResponses {
    val name = (operation as OpenAPIV30Operation).toName() ?: (path.toName() + method.name)
    when (val response = response as OpenAPIV30ResponseOrReference) {
        is OpenAPIV30Response -> flatMapResponse(response, name, statusCode)
        is OpenAPIV30Reference -> flatMapResponse(resolveOpenAPIV30Response(response).second, name, statusCode)
    }
}

private fun OpenAPIV30Model.parseDefinitions(): List<Definition> = components?.schemas.orEmpty()
    .filter {
        when (val s = it.value) {
            is OpenAPIV30Schema -> when (s.additionalProperties) {
                is OpenAPIV30Boolean -> true
                is OpenAPIV30Reference -> false
                is OpenAPIV30Schema -> true
                null -> true
            }

            is OpenAPIV30Reference -> false
        }
    }
    .flatMap { flatten(it.value, className(it.key)) }

private fun OpenAPIV30Model.toSegments(
    path: Path,
    parameters: List<OpenAPIV30Parameter>,
    operation: OpenAPIV30Operation,
    method: Endpoint.Method,
) = path.value.split("/").drop(1).filter { it.isNotBlank() }.map { segment ->
    when (segment.isParam()) {
        true -> {
            val param = segment.substring(1, segment.length - 1)
            val name = operation.toName() ?: (path.toName() + method.name)
            parameters
                .find { it.name == param }
                ?.schema
                ?.let { resolve(it) }
                ?.let { toReference(it, false, className(name, "Parameter", param)) }
                ?.let {
                    Endpoint.Segment.Param(
                        identifier = FieldIdentifier(param),
                        reference = it,
                    )
                }
                ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
        }

        false -> Endpoint.Segment.Literal(segment)
    }
}

private fun OpenAPIV30Model.resolveParameters(parameters: List<OpenAPIV30ParameterOrReference>?): List<OpenAPIV30Parameter> = parameters.orEmpty()
    .mapNotNull {
        when (it) {
            is OpenAPIV30Parameter -> it
            is OpenAPIV30Reference -> resolveOpenAPIV30Parameter(it)
        }
    }

private fun OpenAPIV30Model.resolveOpenAPIV30Parameter(reference: OpenAPIV30Reference): OpenAPIV30Parameter? = components?.parameters
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV30Parameter -> it
            is OpenAPIV30Reference -> resolveOpenAPIV30Parameter(it)
        }
    }

private fun OpenAPIV30Model.resolveOpenAPIV30Schema(reference: OpenAPIV30Reference): Pair<OpenAPIV30Reference, OpenAPIV30Schema> = components?.schemas
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV30Schema -> reference to it
            is OpenAPIV30Reference -> resolveOpenAPIV30Schema(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV30Model.resolveOpenAPIV30Header(reference: OpenAPIV30Reference): Pair<OpenAPIV30Reference, OpenAPIV30Header> = components?.headers
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV30Header -> reference to it
            is OpenAPIV30Reference -> resolveOpenAPIV30Header(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV30Model.resolveOpenAPIV30RequestBody(reference: OpenAPIV30Reference): Pair<OpenAPIV30Reference, OpenAPIV30RequestBody> = components?.requestBodies
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV30RequestBody -> reference to it
            is OpenAPIV30Reference -> resolveOpenAPIV30RequestBody(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV30Model.resolveOpenAPIV30Response(reference: OpenAPIV30Reference): Pair<OpenAPIV30Reference, OpenAPIV30Response> = components?.responses
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV30Response -> reference to it
            is OpenAPIV30Reference -> resolveOpenAPIV30Response(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV30Model.resolve(schemaOrReference: OpenAPIV30SchemaOrReference): OpenAPIV30Schema = when (schemaOrReference) {
    is OpenAPIV30Schema -> schemaOrReference
    is OpenAPIV30Reference -> resolveOpenAPIV30Schema(schemaOrReference).second
}

private fun OpenAPIV30Model.resolve(headerOrReference: OpenAPIV30HeaderOrReference): OpenAPIV30Header = when (headerOrReference) {
    is OpenAPIV30Header -> headerOrReference
    is OpenAPIV30Reference -> resolveOpenAPIV30Header(headerOrReference).second
}

private fun OpenAPIV30Model.resolve(schemaOrReferenceOrBoolean: OpenAPIV30SchemaOrReferenceOrBoolean): OpenAPIV30Schema = when (schemaOrReferenceOrBoolean) {
    is OpenAPIV30Schema -> schemaOrReferenceOrBoolean
    is OpenAPIV30Reference -> resolveOpenAPIV30Schema(schemaOrReferenceOrBoolean).second
    is BooleanValue -> TODO("Not yet implemented")
}

private fun OpenAPIV30Model.resolve(requestBodyOrReference: OpenAPIV30RequestBodyOrReference): OpenAPIV30RequestBody = when (requestBodyOrReference) {
    is OpenAPIV30RequestBody -> requestBodyOrReference
    is OpenAPIV30Reference -> resolveOpenAPIV30RequestBody(requestBodyOrReference).second
}

private fun OpenAPIV30Model.resolve(responseOrOpenAPIV30Reference: OpenAPIV30ResponseOrReference): OpenAPIV30Response = when (responseOrOpenAPIV30Reference) {
    is OpenAPIV30Response -> responseOrOpenAPIV30Reference
    is OpenAPIV30Reference -> resolveOpenAPIV30Response(responseOrOpenAPIV30Reference).second
}

private fun OpenAPIV30Model.resolveOpenAPIV30Link(reference: OpenAPIV30Reference): OpenAPIV30Link = components?.links
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV30Link -> it
            is OpenAPIV30Reference -> resolveOpenAPIV30Link(it)
        }
    }
    ?: error("Cannot resolve link ref: ${reference.ref}")

private fun OpenAPIV30Model.resolve(linkOrReference: OpenAPIV30LinkOrReference): OpenAPIV30Link = when (linkOrReference) {
    is OpenAPIV30Link -> linkOrReference
    is OpenAPIV30Reference -> resolveOpenAPIV30Link(linkOrReference)
}

private fun OpenAPIV30Model.toLinkAnnotationList(links: OpenAPIV30Links?): List<Annotation> = links?.entries
    ?.map { entry -> resolve(entry.value).toLinkInfo(entry.key).toAnnotation() }
    .orEmpty()

private fun OpenAPIV30Link.toLinkInfo(name: String): LinkInfo = LinkInfo(
    name = name,
    operationId = operationId,
    operationRef = operationRef,
    parameters = parameters.orEmpty().mapValues { it.value.asLinkExpression() },
    requestBody = requestBody?.asLinkExpression(),
    description = description,
    serverUrl = server?.url,
)

private fun JsonElement.asLinkExpression(): String = (this as? JsonPrimitive)?.contentOrNull ?: toString()

private fun OpenAPIV30Model.flatten(schemaObject: OpenAPIV30Schema, name: String): List<Definition> = when {
    schemaObject.additionalProperties.exists() -> when (schemaObject.additionalProperties) {
        is BooleanValue -> emptyList()
        else ->
            schemaObject.additionalProperties
                ?.let { resolve(it) }
                ?.takeIf { it.properties != null }
                ?.let { flatten(it, name) }
                ?: emptyList()
    }

    schemaObject.oneOf != null || schemaObject.anyOf != null -> listOf(
        Union(
            comment = null,
            annotations = schemaObject.description.toDescriptionAnnotationList(),
            identifier = DefinitionIdentifier(name.sanitize()),
            entries = schemaObject.oneOf
                .orEmpty()
                .mapIndexed { index, it ->
                    when (it) {
                        is OpenAPIV30Reference -> toReference(it, false)
                        is OpenAPIV30Schema -> toReference(it, false, className(name, index.toString()))
                    }
                }
                .toSet(),

        ),
    )
        .plus(
            schemaObject.oneOf.orEmpty().flatMapIndexed { index, it ->
                when (it) {
                    is OpenAPIV30Reference -> emptyList()
                    is OpenAPIV30Schema -> flatten(it, className(name, index.toString()))
                }
            },
        )

    schemaObject.allOf != null -> listOf(
        Type(
            comment = null,
            annotations = schemaObject.description.toDescriptionAnnotationList(),
            identifier = DefinitionIdentifier(name.sanitize()),
            shape = Type.Shape(
                schemaObject.allOf.orEmpty().flatMap { toField(resolve(it), name) }
                    .distinctBy { it.identifier },
            ),
            extends = emptyList(),
        ),
    )
        .plus(
            schemaObject.allOf!!
                .flatMap {
                    when (it) {
                        is OpenAPIV30Reference -> resolveOpenAPIV30Schema(it).second.properties.orEmpty()
                        is OpenAPIV30Schema -> it.properties.orEmpty()
                    }
                        .flatMap { (key, value) ->
                            flatten(value, className(name, key))
                        }
                },
        )

    schemaObject.enum != null ->
        schemaObject.enum!!
            .map { it.content }
            .toSet()
            .let {
                listOf(
                    Enum(
                        comment = null,
                        annotations = schemaObject.description.toDescriptionAnnotationList(),
                        identifier = DefinitionIdentifier(name),
                        entries = it,
                    ),
                )
            }

    else -> when (schemaObject.typeOrNull()) {
        null, OpenAPIV30Type.OBJECT -> {
            val fields = schemaObject.properties.orEmpty().flatMap { (key, value) ->
                flatten(value, className(name, key))
            }
            val schema = listOf(
                Type(
                    comment = null,
                    annotations = schemaObject.description.toDescriptionAnnotationList(),
                    identifier = DefinitionIdentifier(name),
                    shape = Type.Shape(toField(schemaObject, name)),
                    extends = emptyList(),
                ),
            )

            schema + fields
        }

        OpenAPIV30Type.ARRAY ->
            schemaObject.items
                ?.let {
                    when (it) {
                        is OpenAPIV30Reference -> emptyList()
                        is OpenAPIV30Schema -> flatten(it, className(name, "array"))
                    }
                }
                ?: emptyList()

        else -> emptyList()
    }
}

private fun OpenAPIV30Model.flatten(schemaOrReference: OpenAPIV30SchemaOrReference, name: String): List<Definition> = when (schemaOrReference) {
    is OpenAPIV30Schema -> flatten(schemaOrReference, name)
    is OpenAPIV30Reference -> emptyList()
}

private fun OpenAPIV30Model.toReference(reference: OpenAPIV30Reference, isNullable: Boolean): Reference = resolveOpenAPIV30Schema(reference).let { (referencingObject, schema) ->
    when {
        schema.additionalProperties.exists() -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanValue -> Reference.Dict(
                reference = Reference.Any(isNullable = isNullable),
                isNullable = false,
            )

            is OpenAPIV30Reference -> toReference(additionalProperties, schema.nullable ?: false).toDict(isNullable)
            is OpenAPIV30Schema -> toReference(
                additionalProperties,
                schema.nullable ?: false,
                reference.getReference(),
            ).toDict(false)
        }

        schema.enum != null -> Reference.Custom(
            value = className(referencingObject.getReference()).sanitize(),
            isNullable = isNullable,
        )

        schema.typeOrNull().isPrimitive() -> Reference.Primitive(
            type = schema.toPrimitive(),
            isNullable = isNullable,
        )

        schema.typeOrNull() == OpenAPIV30Type.ARRAY -> when (val items = schema.items) {
            is OpenAPIV30Reference -> toReference(items, schema.nullable ?: false).toIterable(isNullable)
            is OpenAPIV30Schema -> Reference.Custom(
                className(referencingObject.getReference(), "Array").sanitize(),
                schema.nullable ?: false,
            ).toIterable(isNullable)

            null -> error("items cannot be null when type is array: ${reference.ref}")
        }

        else -> Reference.Custom(
            value = className(referencingObject.getReference()).sanitize(),
            isNullable = isNullable,
        )
    }
}

private fun OpenAPIV30Model.toReference(
    schema: OpenAPIV30Schema,
    isNullable: Boolean,
    name: String = "",
): Reference = when {
    schema.typeOrNull() == OpenAPIV30Type.ARRAY -> {
        when (val items = schema.items) {
            is OpenAPIV30Reference -> toReference(items, schema.nullable ?: false).toIterable(isNullable)
            is OpenAPIV30Schema -> toReference(items, schema.nullable ?: false, name).toIterable(isNullable)
            null -> error("property 'items' of '$name' cannot be null when 'type' is array: $schema ")
        }
    }

    schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
        is BooleanValue -> Reference.Dict(
            reference = Reference.Any(isNullable = schema.nullable ?: false),
            isNullable = isNullable,
        )

        is OpenAPIV30Reference -> toReference(additionalProperties, schema.nullable ?: false).toDict(isNullable)
        is OpenAPIV30Schema ->
            additionalProperties
                .takeIf { it.typeOrNull().isPrimitive() || it.properties != null }
                ?.let { toReference(it, schema.nullable ?: false, name).toDict(isNullable) }
                ?: Reference.Dict(
                    reference = Reference.Any(isNullable = schema.nullable ?: false),
                    isNullable = isNullable,
                )
    }

    schema.enum != null -> Reference.Custom(value = name.sanitize(), isNullable = isNullable)
        .let { if (schema.additionalProperties != null) Reference.Dict(reference = it, isNullable = false) else it }

    else -> when (schema.typeOrNull()) {
        OpenAPIV30Type.STRING, OpenAPIV30Type.NUMBER, OpenAPIV30Type.INTEGER, OpenAPIV30Type.BOOLEAN -> Reference.Primitive(
            type = schema.toPrimitive(),
            isNullable = isNullable,
        ).let { if (schema.additionalProperties != null) Reference.Dict(it, isNullable = false) else it }

        null, OpenAPIV30Type.OBJECT ->
            when {
                schema.additionalProperties is BooleanValue -> Reference.Any(isNullable = false)
                    .let { if (schema.additionalProperties != null) Reference.Dict(it, isNullable = false) else it }

                else -> Reference.Custom(
                    value = name.sanitize(),
                    isNullable = isNullable,
                ).let { if (schema.additionalProperties != null) Reference.Dict(it, isNullable = false) else it }
            }

        OpenAPIV30Type.ARRAY -> {
            when (val it = schema.items) {
                is OpenAPIV30Reference -> toReference(it, schema.nullable ?: false).toIterable(isNullable)
                is OpenAPIV30Schema -> toReference(it, schema.nullable ?: false, name).toIterable(isNullable)
                null -> error("When schema is of type array items cannot be null for name: $name")
            }
        }

        OpenAPIV30Type.NULL -> Reference.Any(isNullable = true)
    }
}

private fun OpenAPIV30Schema.toPrimitive() = when (this.typeOrNull()) {
    OpenAPIV30Type.STRING -> when {
        pattern != null -> Reference.Primitive.Type.String(
            constraint = Reference.Primitive.Type.Constraint.RegExp(
                pattern!!,
            ),
        )
        format == "binary" -> Reference.Primitive.Type.Bytes
        else -> Reference.Primitive.Type.String(null)
    }

    OpenAPIV30Type.INTEGER -> Reference.Primitive.Type.Integer(
        if (format == "int32") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64,
        null,
    )

    OpenAPIV30Type.NUMBER -> Reference.Primitive.Type.Number(
        if (format == "float") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64,
        null,
    )

    OpenAPIV30Type.BOOLEAN -> Reference.Primitive.Type.Boolean
    else -> error("Type is not a primitive")
}

private fun OpenAPIV30Model.toField(schema: OpenAPIV30Schema, name: String) = schema.properties.orEmpty().map { (key, value) ->
    val isNullable = !(schema.required?.contains(key) ?: false)
    when (value) {
        is OpenAPIV30Schema -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = value.description.toDescriptionAnnotationList(),
                reference = when {
                    value.enum != null -> toReference(value, isNullable || value.nullable == true, className(name, key))
                    value.typeOrNull() == OpenAPIV30Type.ARRAY -> toReference(
                        value,
                        isNullable || value.nullable == true,
                        className(name, key, "Array"),
                    )

                    else -> toReference(value, isNullable || value.nullable == true, className(name, key))
                },
            )
        }

        is OpenAPIV30Reference -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = emptyList(),
                reference = toReference(value, isNullable),
            )
        }
    }
}

private fun OpenAPIV30Model.toField(parameter: OpenAPIV30Parameter, name: String): Field {
    val isNullable = !(parameter.required ?: false)
    return when (val s = parameter.schema) {
        is OpenAPIV30Reference -> toReference(s, isNullable)
        is OpenAPIV30Schema -> toReference(s, isNullable, name + if (s.typeOrNull() == OpenAPIV30Type.ARRAY) "Array" else "")
        null -> Reference.Primitive(
            type = Reference.Primitive.Type.String(null),
            isNullable = isNullable,
        )
    }.let {
        Field(
            identifier = FieldIdentifier(parameter.name),
            annotations = parameter.description.toDescriptionAnnotationList(),
            reference = it,
        )
    }
}

private fun OpenAPIV30Model.toField(header: OpenAPIV30Header, identifier: String, name: String): Field {
    val isNullable = !(header.required ?: false)
    return when (val s = header.schema) {
        is OpenAPIV30Reference -> toReference(s, isNullable)
        is OpenAPIV30Schema -> toReference(s, isNullable, name)
        null -> Reference.Primitive(
            type = Reference.Primitive.Type.String(null),
            isNullable = isNullable,
        )
    }.let {
        Field(
            identifier = FieldIdentifier(identifier),
            annotations = header.description.toDescriptionAnnotationList(),
            reference = it,
        )
    }
}

private fun OpenAPIV30Type?.isPrimitive() = when (this) {
    OpenAPIV30Type.STRING -> true
    OpenAPIV30Type.NUMBER -> true
    OpenAPIV30Type.INTEGER -> true
    OpenAPIV30Type.BOOLEAN -> true
    OpenAPIV30Type.ARRAY -> false
    OpenAPIV30Type.OBJECT -> false
    OpenAPIV30Type.NULL -> false
    null -> false
}

private fun OpenAPIV30SchemaOrReferenceOrBoolean?.exists() = when (this) {
    is OpenAPIV30SchemaOrReference -> true
    is BooleanValue -> this.value
    is OpenAPIV30Reference -> true
    else -> false
}
