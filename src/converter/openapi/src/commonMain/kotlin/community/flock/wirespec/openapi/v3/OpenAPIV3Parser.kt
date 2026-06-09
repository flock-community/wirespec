package community.flock.wirespec.openapi.v3

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.kotlinx.openapi.bindings.BooleanValue
import community.flock.kotlinx.openapi.bindings.Header
import community.flock.kotlinx.openapi.bindings.HeaderOrReference
import community.flock.kotlinx.openapi.bindings.Link
import community.flock.kotlinx.openapi.bindings.LinkOrReference
import community.flock.kotlinx.openapi.bindings.MediaType
import community.flock.kotlinx.openapi.bindings.MediaTypeObject
import community.flock.kotlinx.openapi.bindings.OpenAPIV3
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV30ParameterLocation
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV30SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV30SingleType
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Type
import community.flock.kotlinx.openapi.bindings.OpenAPIV30TypeArray
import community.flock.kotlinx.openapi.bindings.OpenAPIV31Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV31Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV31Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV31Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV31SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV31SingleType
import community.flock.kotlinx.openapi.bindings.OpenAPIV31Type
import community.flock.kotlinx.openapi.bindings.OpenAPIV31TypeArray
import community.flock.kotlinx.openapi.bindings.OpenAPIV32Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV32Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV32Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV32Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV32SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV32SingleType
import community.flock.kotlinx.openapi.bindings.OpenAPIV32Type
import community.flock.kotlinx.openapi.bindings.OpenAPIV32TypeArray
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Model
import community.flock.kotlinx.openapi.bindings.Parameter
import community.flock.kotlinx.openapi.bindings.ParameterOrReference
import community.flock.kotlinx.openapi.bindings.Path
import community.flock.kotlinx.openapi.bindings.RequestBody
import community.flock.kotlinx.openapi.bindings.RequestBodyOrReference
import community.flock.kotlinx.openapi.bindings.Response
import community.flock.kotlinx.openapi.bindings.ResponseOrReference
import community.flock.kotlinx.openapi.bindings.Schema
import community.flock.kotlinx.openapi.bindings.SchemaOrReference
import community.flock.kotlinx.openapi.bindings.SchemaOrReferenceOrBoolean
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
import community.flock.kotlinx.openapi.bindings.Reference as OpenAPIReference

object OpenAPIV3Parser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = parseOpenApi(moduleContent) {
        OpenAPIV3(jsonDefault(strict))
            .decodeFromString(it)
            .parse()
    }

    fun OpenAPIV3Model.parse(): NonEmptyList<Definition> = listOf(
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

private fun OpenAPIV3Model.parseEndpoints(): List<Definition> = paths.orEmpty()
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList()
            .map { (method, operation) ->
                val parameters = resolveParameters(pathItem.parameters) + resolveParameters(operation.parameters)
                val segments = toSegments(path, parameters, operation.toName() ?: (path.toName() + method.name))
                val name = operation.toName() ?: (path.toName() + method.name)
                val query = parameters
                    .filter { it.location == OpenAPIV30ParameterLocation.QUERY }
                    .map { toField(it, className(name, "Parameter", it.name)) }
                val headers = parameters
                    .filter { it.location == OpenAPIV30ParameterLocation.HEADER }
                    .map { toField(it, className(name, "Parameter", it.name)) }
                val requests = operation.requestBody?.let { resolve(it) }
                    ?.let { requestBody ->
                        val isNullable = false
                        requestBody.content?.map { (mediaType, mediaObject) ->
                            val reference = when (val schema = mediaObject.schema) {
                                is OpenAPIReference -> toReference(schema, isNullable)
                                is Schema -> toReference(schema, isNullable, className(name, "RequestBody"))
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
                        val content = response.content
                        if (content.isNullOrEmpty()) {
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
                            content.map { (contentType, media) ->
                                val isNullable = media.schema?.let { resolve(it) }?.isNullable ?: false
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
                                            is OpenAPIReference -> toReference(schema, isNullable)
                                            is Schema -> toReference(
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

private fun OpenAPIV3Model.parseParameters(): List<Definition> = flatMapRequests {
    val parameters = resolveParameters(pathItem.parameters) + resolveParameters(operation.parameters)
    val name = operation.toName() ?: (path.toName() + method.name)
    parameters.flatMap { parameter ->
        parameter.schema?.let { flattenSchemaOrRef(it, className(name, "Parameter", parameter.name)) } ?: emptyList()
    }
}

private fun OpenAPIV3Model.parseRequestBody(): List<Definition> = flatMapRequests {
    val name = operation.toName() ?: (path.toName() + method.name)
    operation.requestBody?.let { resolve(it) }?.content.orEmpty()
        .flatMap { (_, mediaObject) ->
            when (val schema = mediaObject.schema) {
                is Schema -> when (schema.primitiveType) {
                    null, OpenAPIV30Type.OBJECT -> flatten(schema, className(name, "RequestBody"))

                    OpenAPIV30Type.ARRAY -> schema.items?.let { flattenSchemaOrRef(it, className(name, "RequestBody")) }.orEmpty()

                    else -> emptyList()
                }

                is OpenAPIReference, null -> emptyList()
            }
        }
}

private fun OpenAPIV3Model.flatMapResponse(
    response: Response,
    name: String,
    statusCode: StatusCode,
): List<Definition> = response.content.orEmpty()
    .flatMap { (_, mediaObject) ->
        when (val schema = mediaObject.schema) {
            is Schema -> when (schema.primitiveType) {
                null, OpenAPIV30Type.OBJECT -> flatten(
                    schema,
                    className(name, statusCode.value, "ResponseBody"),
                )

                OpenAPIV30Type.ARRAY -> schema.items?.let {
                    flattenSchemaOrRef(
                        it,
                        className(name, statusCode.value, "ResponseBody"),
                    )
                }.orEmpty()

                else -> emptyList()
            }

            else -> emptyList()
        }
    }

private fun OpenAPIV3Model.parseResponseBody(): List<Definition> = flatMapResponses {
    val name = operation.toName() ?: (path.toName() + method.name)
    when (val res = response) {
        is OpenAPIReference -> flatMapResponse(resolveResponse(res).second, name, statusCode)
        is Response -> flatMapResponse(res, name, statusCode)
    }
}

private fun OpenAPIV3Model.parseDefinitions(): List<Definition> = components?.schemas.orEmpty()
    .filter {
        when (val s = it.value) {
            is Schema -> when (s.additionalProperties) {
                is BooleanValue -> true
                is OpenAPIReference -> false
                is Schema -> true
                null -> true
            }

            is OpenAPIReference -> false
        }
    }
    .flatMap { flattenSchemaOrRef(it.value, className(it.key)) }

private fun OpenAPIV3Model.toSegments(
    path: Path,
    parameters: List<Parameter>,
    name: String,
) = path.value.split("/").drop(1).filter { it.isNotBlank() }.map { segment ->
    when (segment.isParam()) {
        true -> {
            val param = segment.substring(1, segment.length - 1)
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

private fun OpenAPIV3Model.resolveParameters(parameters: List<ParameterOrReference>?): List<Parameter> = parameters.orEmpty()
    .mapNotNull {
        when (it) {
            is OpenAPIReference -> resolveParameter(it)
            is Parameter -> it
        }
    }

private fun OpenAPIV3Model.resolveParameter(reference: OpenAPIReference): Parameter? = components?.parameters
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIReference -> resolveParameter(it)
            is Parameter -> it
        }
    }

private fun OpenAPIV3Model.resolveSchemaRef(reference: OpenAPIReference): Pair<OpenAPIReference, Schema> = components?.schemas
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is Schema -> reference to it
            is OpenAPIReference -> resolveSchemaRef(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV3Model.resolveHeader(reference: OpenAPIReference): Pair<OpenAPIReference, Header> = components?.headers
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is Header -> reference to it
            is OpenAPIReference -> resolveHeader(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV3Model.resolveRequestBody(reference: OpenAPIReference): Pair<OpenAPIReference, RequestBody> = components?.requestBodies
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is RequestBody -> reference to it
            is OpenAPIReference -> resolveRequestBody(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV3Model.resolveResponse(reference: OpenAPIReference): Pair<OpenAPIReference, Response> = components?.responses
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is Response -> reference to it
            is OpenAPIReference -> resolveResponse(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV3Model.resolve(schemaOrReference: SchemaOrReference): Schema = when (schemaOrReference) {
    is Schema -> schemaOrReference
    is OpenAPIReference -> resolveSchemaRef(schemaOrReference).second
}

private fun OpenAPIV3Model.resolve(headerOrReference: HeaderOrReference): Header = when (headerOrReference) {
    is Header -> headerOrReference
    is OpenAPIReference -> resolveHeader(headerOrReference).second
}

private fun OpenAPIV3Model.resolve(schemaOrReferenceOrBoolean: SchemaOrReferenceOrBoolean): Schema = when (schemaOrReferenceOrBoolean) {
    is Schema -> schemaOrReferenceOrBoolean
    is OpenAPIReference -> resolveSchemaRef(schemaOrReferenceOrBoolean).second
    is BooleanValue -> TODO("Not yet implemented")
}

private fun OpenAPIV3Model.resolve(requestBodyOrReference: RequestBodyOrReference): RequestBody = when (requestBodyOrReference) {
    is RequestBody -> requestBodyOrReference
    is OpenAPIReference -> resolveRequestBody(requestBodyOrReference).second
}

private fun OpenAPIV3Model.resolve(responseOrReference: ResponseOrReference): Response = when (responseOrReference) {
    is Response -> responseOrReference
    is OpenAPIReference -> resolveResponse(responseOrReference).second
}

private fun OpenAPIV3Model.resolveLink(reference: OpenAPIReference): Link = components?.links
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is Link -> it
            is OpenAPIReference -> resolveLink(it)
        }
    }
    ?: error("Cannot resolve link ref: ${reference.ref}")

private fun OpenAPIV3Model.resolve(linkOrReference: LinkOrReference): Link = when (linkOrReference) {
    is Link -> linkOrReference
    is OpenAPIReference -> resolveLink(linkOrReference)
}

private fun OpenAPIV3Model.toLinkAnnotationList(links: Map<String, LinkOrReference>?): List<Annotation> = links?.entries
    ?.map { entry -> resolve(entry.value).toLinkInfo(entry.key).toAnnotation() }
    .orEmpty()

private fun Link.toLinkInfo(name: String): LinkInfo = LinkInfo(
    name = name,
    operationId = operationId,
    operationRef = operationRef,
    parameters = parameters.orEmpty().mapValues { it.value.asLinkExpression() },
    requestBody = requestBody?.asLinkExpression(),
    description = description,
    serverUrl = server?.url,
)

private fun JsonElement.asLinkExpression(): String = (this as? JsonPrimitive)?.contentOrNull ?: toString()

private fun OpenAPIV3Model.flatten(schemaObject: Schema, name: String): List<Definition> {
    val oneOf = schemaObject.oneOf
    val anyOf = schemaObject.anyOf
    return when {
        schemaObject.additionalProperties.exists() -> when (schemaObject.additionalProperties) {
            is BooleanValue -> emptyList()
            else ->
                schemaObject.additionalProperties
                    ?.let { resolve(it) }
                    ?.takeIf { it.properties != null }
                    ?.let { flatten(it, name) }
                    ?: emptyList()
        }

        oneOf != null || anyOf != null -> listOf(
            Union(
                comment = null,
                annotations = schemaObject.description.toDescriptionAnnotationList(),
                identifier = DefinitionIdentifier(name.sanitize()),
                entries = oneOf
                    .orEmpty()
                    .mapIndexed { index, it ->
                        when (it) {
                            is OpenAPIReference -> toReference(it, false)
                            is Schema -> toReference(it, false, className(name, index.toString()))
                        }
                    }
                    .toSet(),

            ),
        )
            .plus(
                oneOf.orEmpty().flatMapIndexed { index, it ->
                    when (it) {
                        is OpenAPIReference -> emptyList()
                        is Schema -> flatten(it, className(name, index.toString()))
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
                            is OpenAPIReference -> resolveSchemaRef(it).second.properties.orEmpty()
                            is Schema -> it.properties.orEmpty()
                        }
                            .flatMap { (key, value) ->
                                flattenSchemaOrRef(value, className(name, key))
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

        else -> when (schemaObject.primitiveType) {
            null, OpenAPIV30Type.OBJECT -> {
                val fields = schemaObject.properties.orEmpty().flatMap { (key, value) ->
                    flattenSchemaOrRef(value, className(name, key))
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
                            is OpenAPIReference -> emptyList()
                            is Schema -> flatten(it, className(name, "array"))
                        }
                    }
                    ?: emptyList()

            else -> emptyList()
        }
    }
}

private fun OpenAPIV3Model.flattenSchemaOrRef(schemaOrReference: SchemaOrReference, name: String): List<Definition> = when (schemaOrReference) {
    is Schema -> flatten(schemaOrReference, name)
    is OpenAPIReference -> emptyList()
}

private fun OpenAPIV3Model.toReference(reference: OpenAPIReference, isNullable: Boolean): Reference = resolveSchemaRef(reference).let { (referencingObject, schema) ->
    val nullable = schema.isNullable
    when {
        schema.additionalProperties.exists() -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanValue -> Reference.Dict(
                reference = Reference.Any(isNullable = isNullable),
                isNullable = false,
            )

            is OpenAPIReference -> toReference(additionalProperties, nullable).toDict(isNullable)
            is Schema -> toReference(
                additionalProperties,
                nullable,
                reference.getReference(),
            ).toDict(false)
        }

        schema.enum != null -> Reference.Custom(
            value = className(referencingObject.getReference()).sanitize(),
            isNullable = isNullable,
        )

        schema.primitiveType.isPrimitive() -> Reference.Primitive(
            type = schema.toPrimitive(),
            isNullable = isNullable,
        )

        schema.primitiveType == OpenAPIV30Type.ARRAY -> when (val items = schema.items) {
            is OpenAPIReference -> toReference(items, nullable).toIterable(isNullable)
            is Schema -> Reference.Custom(
                className(referencingObject.getReference(), "Array").sanitize(),
                nullable,
            ).toIterable(isNullable)

            null -> error("items cannot be null when type is array: ${reference.ref}")
        }

        else -> Reference.Custom(
            value = className(referencingObject.getReference()).sanitize(),
            isNullable = isNullable,
        )
    }
}

private fun OpenAPIV3Model.toReference(
    schema: Schema,
    isNullable: Boolean,
    name: String = "",
): Reference {
    val nullable = schema.isNullable
    return when {
        schema.primitiveType == OpenAPIV30Type.ARRAY -> {
            when (val items = schema.items) {
                is OpenAPIReference -> toReference(items, nullable).toIterable(isNullable)
                is Schema -> toReference(items, nullable, name).toIterable(isNullable)
                null -> error("property 'items' of '$name' cannot be null when 'type' is array: $schema ")
            }
        }

        schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanValue -> Reference.Dict(
                reference = Reference.Any(isNullable = nullable),
                isNullable = isNullable,
            )

            is OpenAPIReference -> toReference(additionalProperties, nullable).toDict(isNullable)
            is Schema ->
                additionalProperties
                    .takeIf { it.primitiveType.isPrimitive() || it.properties != null }
                    ?.let { toReference(it, nullable, name).toDict(isNullable) }
                    ?: Reference.Dict(
                        reference = Reference.Any(isNullable = nullable),
                        isNullable = isNullable,
                    )
        }

        schema.enum != null -> Reference.Custom(value = name.sanitize(), isNullable = isNullable)
            .let { if (schema.additionalProperties != null) Reference.Dict(reference = it, isNullable = false) else it }

        else -> when (schema.primitiveType) {
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
                    is OpenAPIReference -> toReference(it, nullable).toIterable(isNullable)
                    is Schema -> toReference(it, nullable, name).toIterable(isNullable)
                    null -> error("When schema is of type array items cannot be null for name: $name")
                }
            }

            OpenAPIV30Type.NULL -> Reference.Any(isNullable = true)
        }
    }
}

private fun Schema.toPrimitive() = when (this.primitiveType) {
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

private fun OpenAPIV3Model.toField(schema: Schema, name: String) = schema.properties.orEmpty().map { (key, value) ->
    val isNullable = !(schema.required?.contains(key) ?: false)
    when (value) {
        is Schema -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = value.description.toDescriptionAnnotationList(),
                reference = when {
                    value.enum != null -> toReference(value, isNullable, className(name, key))
                    value.primitiveType == OpenAPIV30Type.ARRAY -> toReference(
                        value,
                        isNullable,
                        className(name, key, "Array"),
                    )

                    else -> toReference(value, isNullable, className(name, key))
                },
            )
        }

        is OpenAPIReference -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = emptyList(),
                reference = toReference(value, isNullable),
            )
        }
    }
}

private fun OpenAPIV3Model.toField(parameter: Parameter, name: String): Field {
    val isNullable = !(parameter.required ?: false)
    return when (val s = parameter.schema) {
        is OpenAPIReference -> toReference(s, isNullable)
        is Schema -> toReference(s, isNullable, name + if (s.primitiveType == OpenAPIV30Type.ARRAY) "Array" else "")
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

private fun OpenAPIV3Model.toField(header: Header, identifier: String, name: String): Field {
    val isNullable = !(header.required ?: false)
    return when (val s = header.schema) {
        is OpenAPIReference -> toReference(s, isNullable)
        is Schema -> toReference(s, isNullable, name)
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

// The kotlin-openapi-bindings 0.3.1 split the single V3 model into version-specific
// 3.0 / 3.1 / 3.2 type families that only share a handful of common interfaces
// (Parameter, Schema, Response, Header, ...). The accessors below normalize the
// version-specific properties that are NOT exposed on those common interfaces, so the
// parser works for every OpenAPI 3.x version instead of only 3.0.

private fun OpenAPIV31Type.toV30() = OpenAPIV30Type.valueOf(name)
private fun OpenAPIV32Type.toV30() = OpenAPIV30Type.valueOf(name)

private val Schema.primitiveType: OpenAPIV30Type?
    get() = when (this) {
        is OpenAPIV30Schema -> when (val t = type) {
            is OpenAPIV30SingleType -> t.value
            is OpenAPIV30TypeArray -> t.values.firstOrNull { it != OpenAPIV30Type.NULL }
            null -> null
        }

        is OpenAPIV31Schema -> when (val t = type) {
            is OpenAPIV31SingleType -> t.value.toV30()
            is OpenAPIV31TypeArray -> t.values.firstOrNull { it != OpenAPIV31Type.NULL }?.toV30()
            null -> null
        }

        is OpenAPIV32Schema -> when (val t = type) {
            is OpenAPIV32SingleType -> t.value.toV30()
            is OpenAPIV32TypeArray -> t.values.firstOrNull { it != OpenAPIV32Type.NULL }?.toV30()
            null -> null
        }

        else -> null
    }

private val Schema.isNullable: Boolean
    get() = when (this) {
        is OpenAPIV30Schema -> nullable ?: false
        is OpenAPIV31Schema -> (type as? OpenAPIV31TypeArray)?.values?.contains(OpenAPIV31Type.NULL) ?: false
        is OpenAPIV32Schema -> (type as? OpenAPIV32TypeArray)?.values?.contains(OpenAPIV32Type.NULL) ?: false
        else -> false
    }

private fun OpenAPIV30SchemaOrReference.toCommon(): SchemaOrReference = when (this) {
    is Schema -> this
    is OpenAPIReference -> this
}

private fun OpenAPIV31SchemaOrReference.toCommon(): SchemaOrReference = when (this) {
    is Schema -> this
    is OpenAPIReference -> this
}

private fun OpenAPIV32SchemaOrReference.toCommon(): SchemaOrReference = when (this) {
    is Schema -> this
    is OpenAPIReference -> this
}

private val Schema.oneOf: List<SchemaOrReference>?
    get() = when (this) {
        is OpenAPIV30Schema -> oneOf?.map { it.toCommon() }
        is OpenAPIV31Schema -> oneOf?.map { it.toCommon() }
        is OpenAPIV32Schema -> oneOf?.map { it.toCommon() }
        else -> null
    }

private val Schema.anyOf: List<SchemaOrReference>?
    get() = when (this) {
        is OpenAPIV30Schema -> anyOf?.map { it.toCommon() }
        is OpenAPIV31Schema -> anyOf?.map { it.toCommon() }
        is OpenAPIV32Schema -> anyOf?.map { it.toCommon() }
        else -> null
    }

private val Response.content: Map<MediaType, MediaTypeObject>?
    get() = when (this) {
        is OpenAPIV30Response -> content
        is OpenAPIV31Response -> content
        is OpenAPIV32Response -> content
        else -> null
    }

private val Header.schema: SchemaOrReference?
    get() = when (this) {
        is OpenAPIV30Header -> schema
        is OpenAPIV31Header -> schema
        is OpenAPIV32Header -> schema
        else -> null
    }

private val Header.required: Boolean?
    get() = when (this) {
        is OpenAPIV30Header -> required
        is OpenAPIV31Header -> required
        is OpenAPIV32Header -> required
        else -> null
    }

private val Parameter.required: Boolean?
    get() = when (this) {
        is OpenAPIV30Parameter -> required
        is OpenAPIV31Parameter -> required
        is OpenAPIV32Parameter -> required
        else -> null
    }

private val Parameter.description: String?
    get() = when (this) {
        is OpenAPIV30Parameter -> description
        is OpenAPIV31Parameter -> description
        is OpenAPIV32Parameter -> description
        else -> null
    }

private val Parameter.location: OpenAPIV30ParameterLocation
    get() = when (this) {
        is OpenAPIV30Parameter -> `in`
        is OpenAPIV31Parameter -> OpenAPIV30ParameterLocation.valueOf(`in`.name)
        is OpenAPIV32Parameter -> OpenAPIV30ParameterLocation.valueOf(`in`.name)
        else -> error("Unsupported parameter type: $this")
    }

private fun SchemaOrReferenceOrBoolean?.exists() = when (this) {
    is Schema -> true
    is BooleanValue -> this.value
    is OpenAPIReference -> true
    null -> false
}
