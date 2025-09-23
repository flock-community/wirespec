package community.flock.wirespec.openapi.v3

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.kotlinx.openapi.bindings.BooleanValue
import community.flock.kotlinx.openapi.bindings.OpenAPIV3
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV3HeaderOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Model
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Operation
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV3ParameterLocation
import community.flock.kotlinx.openapi.bindings.OpenAPIV3PathItem
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Reference
import community.flock.kotlinx.openapi.bindings.OpenAPIV3RequestBody
import community.flock.kotlinx.openapi.bindings.OpenAPIV3RequestBodyOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV3ResponseOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV3SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV3SchemaOrReferenceOrBoolean
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Type
import community.flock.kotlinx.openapi.bindings.Path
import community.flock.kotlinx.openapi.bindings.StatusCode
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.openapi.className
import community.flock.wirespec.openapi.filterNotNullValues
import kotlinx.serialization.json.Json

object OpenAPIV3Parser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = AST(
        nonEmptyListOf(
            Module(
                moduleContent.fileUri,
                OpenAPIV3(
                    json = Json {
                        prettyPrint = true
                        ignoreUnknownKeys = !strict
                    },
                ).decodeFromJsonString(moduleContent.content).parse()
                    .let { requireNotNull(it) { "Cannot yield empty AST for OpenAPI v3" } },
            ),
        ),
    )

    fun OpenAPIV3Model.parse(): NonEmptyList<Definition>? = listOf(
        parseEndpoint(),
        parseParameters(),
        parseRequestBody(),
        parseResponseBody(),
        parseComponents(),
    ).reduce(List<Definition>::plus).toNonEmptyListOrNull()
}

private fun OpenAPIV3Model.parseEndpoint(): List<Definition> = paths
    .flatMap { (key, path) ->
        path.toOperationList().map { (method, operation) ->
            val parameters = resolveParameters(path) + resolveParameters(operation)
            val segments = toSegments(key, parameters, operation, method)
            val name = operation.toName() ?: (key.toName() + method.name)
            val query = parameters
                .filter { it.`in` == OpenAPIV3ParameterLocation.QUERY }
                .map { toField(it, className(name, "Parameter", it.name)) }
            val headers = parameters
                .filter { it.`in` == OpenAPIV3ParameterLocation.HEADER }
                .map { toField(it, className(name, "Parameter", it.name)) }
            val requests = operation.requestBody?.let { resolve(it) }
                ?.let { requestBody ->
                    val isNullable = false
                    requestBody.content?.map { (mediaType, mediaObject) ->
                        val reference = when (val schema = mediaObject.schema) {
                            is OpenAPIV3Reference -> toReference(schema, isNullable)
                            is OpenAPIV3Schema -> toReference(schema, isNullable, className(name, "RequestBody"))
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
                                status = status.value,
                                headers = response.headers?.map { entry ->
                                    toField(resolve(entry.value), entry.key, className(name, "ResponseHeader"))
                                }.orEmpty(),
                                content = Endpoint.Content(
                                    type = contentType.value,
                                    reference = when (val schema = media.schema) {
                                        is OpenAPIV3Reference -> toReference(schema, isNullable)
                                        is OpenAPIV3Schema -> toReference(
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
                        ),
                    )
            }

            Endpoint(
                comment = null,
                annotations = emptyList(), identifier = DefinitionIdentifier(name),
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
    val parameters = resolveParameters(pathItem) + resolveParameters(operation)
    val name = operation.toName() ?: (path.toName() + method.name)
    parameters.flatMap { parameter ->
        parameter.schema?.let { flatten(it, className(name, "Parameter", parameter.name)) } ?: emptyList()
    }
}

private fun OpenAPIV3Model.parseRequestBody(): List<Definition> = flatMapRequests {
    val name = operation.toName() ?: (path.toName() + method.name)
    operation.requestBody?.let { resolve(it) }?.content.orEmpty()
        .flatMap { (_, mediaObject) ->
            when (val schema = mediaObject.schema) {
                is OpenAPIV3Schema -> when (schema.type) {
                    null, OpenAPIV3Type.OBJECT -> flatten(schema, className(name, "RequestBody"))

                    OpenAPIV3Type.ARRAY -> schema.items?.let { flatten(it, className(name, "RequestBody")) }.orEmpty()

                    else -> emptyList()
                }

                is OpenAPIV3Reference, null -> emptyList()
            }
        }
}

private fun OpenAPIV3Model.flatMapResponse(
    response: OpenAPIV3Response,
    name: String,
    statusCode: StatusCode,
): List<Definition> = response.content.orEmpty()
    .flatMap { (_, mediaObject) ->
        when (val schema = mediaObject.schema) {
            is OpenAPIV3Schema -> when (schema.type) {
                null, OpenAPIV3Type.OBJECT -> flatten(
                    schema,
                    className(name, statusCode.value, "ResponseBody"),
                )

                OpenAPIV3Type.ARRAY -> schema.items?.let {
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

private fun OpenAPIV3Model.parseResponseBody(): List<Definition> = flatMapResponses {
    val name = operation.toName() ?: (path.toName() + method.name)
    when (val response = response) {
        is OpenAPIV3Response -> flatMapResponse(response, name, statusCode)
        is OpenAPIV3Reference -> flatMapResponse(resolveOpenAPIV3Response(response).second, name, statusCode)
    }
}

private fun OpenAPIV3Model.parseComponents(): List<Definition> = components?.schemas.orEmpty()
    .filter {
        when (val s = it.value) {
            is OpenAPIV3Schema -> s.additionalProperties == null
            else -> false
        }
    }
    .flatMap { flatten(it.value, className(it.key)) }

private fun String.isParam() = this[0] == '{' && this[length - 1] == '}'

private fun OpenAPIV3Operation.toName() = operationId?.let { className(it) }

private fun Path.toName(): String = value
    .split("/")
    .drop(1)
    .filter { it.isNotBlank() }
    .joinToString("") {
        when (it.isParam()) {
            true -> className(it.substring(1, it.length - 1))
            false -> className(it)
        }
    }

private fun OpenAPIV3Model.toSegments(
    path: Path,
    parameters: List<OpenAPIV3Parameter>,
    operation: OpenAPIV3Operation,
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

private fun OpenAPIV3Model.resolveParameters(operation: OpenAPIV3Operation): List<OpenAPIV3Parameter> = operation.parameters
    ?.mapNotNull {
        when (it) {
            is OpenAPIV3Parameter -> it
            is OpenAPIV3Reference -> resolveOpenAPIV3Parameter(it)
        }
    }
    ?: emptyList()

private fun OpenAPIV3Model.resolveParameters(pathItem: OpenAPIV3PathItem): List<OpenAPIV3Parameter> = pathItem.parameters
    ?.mapNotNull {
        when (it) {
            is OpenAPIV3Parameter -> it
            is OpenAPIV3Reference -> resolveOpenAPIV3Parameter(it)
        }
    }
    ?: emptyList()

private fun OpenAPIV3Model.resolveOpenAPIV3Parameter(reference: OpenAPIV3Reference): OpenAPIV3Parameter? = components?.parameters
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV3Parameter -> it
            is OpenAPIV3Reference -> resolveOpenAPIV3Parameter(it)
        }
    }

private fun OpenAPIV3Model.resolveOpenAPIV3Schema(reference: OpenAPIV3Reference): Pair<OpenAPIV3Reference, OpenAPIV3Schema> = components?.schemas
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV3Schema -> reference to it
            is OpenAPIV3Reference -> resolveOpenAPIV3Schema(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV3Model.resolveOpenAPIV3Header(reference: OpenAPIV3Reference): Pair<OpenAPIV3Reference, OpenAPIV3Header> = components?.headers
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV3Header -> reference to it
            is OpenAPIV3Reference -> resolveOpenAPIV3Header(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV3Model.resolveOpenAPIV3RequestBody(reference: OpenAPIV3Reference): Pair<OpenAPIV3Reference, OpenAPIV3RequestBody> = components?.requestBodies
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV3RequestBody -> reference to it
            is OpenAPIV3Reference -> resolveOpenAPIV3RequestBody(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV3Model.resolveOpenAPIV3Response(reference: OpenAPIV3Reference): Pair<OpenAPIV3Reference, OpenAPIV3Response> = components?.responses
    ?.get(reference.getReference())
    ?.let {
        when (it) {
            is OpenAPIV3Response -> reference to it
            is OpenAPIV3Reference -> resolveOpenAPIV3Response(it)
        }
    }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV3Model.resolve(schemaOrReference: OpenAPIV3SchemaOrReference): OpenAPIV3Schema = when (schemaOrReference) {
    is OpenAPIV3Schema -> schemaOrReference
    is OpenAPIV3Reference -> resolveOpenAPIV3Schema(schemaOrReference).second
}

private fun OpenAPIV3Model.resolve(headerOrReference: OpenAPIV3HeaderOrReference): OpenAPIV3Header = when (headerOrReference) {
    is OpenAPIV3Header -> headerOrReference
    is OpenAPIV3Reference -> resolveOpenAPIV3Header(headerOrReference).second
}

private fun OpenAPIV3Model.resolve(schemaOrReferenceOrBoolean: OpenAPIV3SchemaOrReferenceOrBoolean): OpenAPIV3Schema = when (schemaOrReferenceOrBoolean) {
    is OpenAPIV3Schema -> schemaOrReferenceOrBoolean
    is OpenAPIV3Reference -> resolveOpenAPIV3Schema(schemaOrReferenceOrBoolean).second
    is BooleanValue -> TODO("Not yet implemented")
}

private fun OpenAPIV3Model.resolve(requestBodyOrReference: OpenAPIV3RequestBodyOrReference): OpenAPIV3RequestBody = when (requestBodyOrReference) {
    is OpenAPIV3RequestBody -> requestBodyOrReference
    is OpenAPIV3Reference -> resolveOpenAPIV3RequestBody(requestBodyOrReference).second
}

private fun OpenAPIV3Model.resolve(responseOrOpenAPIV3Reference: OpenAPIV3ResponseOrReference): OpenAPIV3Response = when (responseOrOpenAPIV3Reference) {
    is OpenAPIV3Response -> responseOrOpenAPIV3Reference
    is OpenAPIV3Reference -> resolveOpenAPIV3Response(responseOrOpenAPIV3Reference).second
}

private fun OpenAPIV3Model.flatten(schemaObject: OpenAPIV3Schema, name: String): List<Definition> = when {
    schemaObject.additionalProperties != null -> when (schemaObject.additionalProperties) {
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
            annotations = emptyList(),
            identifier = DefinitionIdentifier(name.sanitize()),
            entries = schemaObject.oneOf
                .orEmpty()
                .mapIndexed { index, it ->
                    when (it) {
                        is OpenAPIV3Reference -> toReference(it, false)
                        is OpenAPIV3Schema -> toReference(it, false, className(name, index.toString()))
                    }
                }
                .toSet(),

        ),
    )
        .plus(
            schemaObject.oneOf.orEmpty().flatMapIndexed { index, it ->
                when (it) {
                    is OpenAPIV3Reference -> emptyList()
                    is OpenAPIV3Schema -> flatten(it, className(name, index.toString()))
                }
            },
        )

    schemaObject.allOf != null -> listOf(
        Type(
            comment = null,
            annotations = emptyList(),
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
                        is OpenAPIV3Reference -> resolveOpenAPIV3Schema(it).second.properties.orEmpty()
                        is OpenAPIV3Schema -> it.properties.orEmpty()
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
            .let { listOf(Enum(comment = null, annotations = emptyList(), identifier = DefinitionIdentifier(name), entries = it)) }

    else -> when (schemaObject.type) {
        null, OpenAPIV3Type.OBJECT -> {
            val fields = schemaObject.properties.orEmpty().flatMap { (key, value) ->
                flatten(value, className(name, key))
            }
            val schema = listOf(
                Type(
                    comment = null,
                    annotations = emptyList(),
                    identifier = DefinitionIdentifier(name),
                    shape = Type.Shape(toField(schemaObject, name)),
                    extends = emptyList(),
                ),
            )

            schema + fields
        }

        OpenAPIV3Type.ARRAY ->
            schemaObject.items
                ?.let {
                    when (it) {
                        is OpenAPIV3Reference -> emptyList()
                        is OpenAPIV3Schema -> flatten(it, className(name, "array"))
                    }
                }
                ?: emptyList()

        else -> emptyList()
    }
}

private fun OpenAPIV3Model.flatten(schemaOrReference: OpenAPIV3SchemaOrReference, name: String) = when (schemaOrReference) {
    is OpenAPIV3Schema -> flatten(schemaOrReference, name)
    is OpenAPIV3Reference -> emptyList()
}

private fun OpenAPIV3Model.toReference(reference: OpenAPIV3Reference, isNullable: Boolean): Reference = resolveOpenAPIV3Schema(reference).let { (referencingObject, schema) ->
    when {
        schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanValue -> Reference.Dict(
                reference = Reference.Any(isNullable = isNullable),
                isNullable = false,
            )

            is OpenAPIV3Reference -> toReference(additionalProperties, schema.nullable ?: false).toDict(isNullable)
            is OpenAPIV3Schema -> toReference(
                additionalProperties,
                schema.nullable ?: false,
                reference.getReference(),
            ).toDict(false)
        }

        schema.enum != null -> Reference.Custom(
            value = className(referencingObject.getReference()).sanitize(),
            isNullable = isNullable,
        )

        schema.type.isPrimitive() -> Reference.Primitive(
            type = schema.toPrimitive(),
            isNullable = isNullable,
        )

        schema.type == OpenAPIV3Type.ARRAY -> when (val items = schema.items) {
            is OpenAPIV3Reference -> toReference(items, schema.nullable ?: false).toIterable(isNullable)
            is OpenAPIV3Schema -> Reference.Custom(
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

private fun OpenAPIV3Model.toReference(
    schema: OpenAPIV3Schema,
    isNullable: Boolean,
    name: String = "",
): Reference = when {
    schema.type == OpenAPIV3Type.ARRAY -> {
        when (val items = schema.items) {
            is OpenAPIV3Reference -> toReference(items, schema.nullable ?: false).toIterable(isNullable)
            is OpenAPIV3Schema -> toReference(items, schema.nullable ?: false, name).toIterable(isNullable)
            null -> error("property 'items' of '$name' cannot be null when 'type' is array: $schema ")
        }
    }

    schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
        is BooleanValue -> Reference.Dict(
            reference = Reference.Any(isNullable = schema.nullable ?: false),
            isNullable = isNullable,
        )

        is OpenAPIV3Reference -> toReference(additionalProperties, schema.nullable ?: false).toDict(isNullable)
        is OpenAPIV3Schema ->
            additionalProperties
                .takeIf { it.type.isPrimitive() || it.properties != null }
                ?.let { toReference(it, schema.nullable ?: false, name).toDict(isNullable) }
                ?: Reference.Dict(
                    reference = Reference.Any(isNullable = schema.nullable ?: false),
                    isNullable = isNullable,
                )
    }

    schema.enum != null -> Reference.Custom(value = name.sanitize(), isNullable = isNullable)
        .let { if (schema.additionalProperties != null) Reference.Dict(reference = it, isNullable = false) else it }

    else -> when (schema.type) {
        OpenAPIV3Type.STRING, OpenAPIV3Type.NUMBER, OpenAPIV3Type.INTEGER, OpenAPIV3Type.BOOLEAN -> Reference.Primitive(
            type = schema.toPrimitive(),
            isNullable = isNullable,
        ).let { if (schema.additionalProperties != null) Reference.Dict(it, isNullable = false) else it }

        null, OpenAPIV3Type.OBJECT ->
            when {
                schema.additionalProperties is BooleanValue -> Reference.Any(isNullable = false)
                    .let { if (schema.additionalProperties != null) Reference.Dict(it, isNullable = false) else it }

                else -> Reference.Custom(
                    value = name.sanitize(),
                    isNullable = isNullable,
                ).let { if (schema.additionalProperties != null) Reference.Dict(it, isNullable = false) else it }
            }

        OpenAPIV3Type.ARRAY -> {
            when (val it = schema.items) {
                is OpenAPIV3Reference -> toReference(it, schema.nullable ?: false).toIterable(isNullable)
                is OpenAPIV3Schema -> toReference(it, schema.nullable ?: false, name).toIterable(isNullable)
                null -> error("When schema is of type array items cannot be null for name: $name")
            }
        }
    }
}

private fun OpenAPIV3PathItem.toOperationList() = Endpoint.Method.entries
    .associateWith {
        when (it) {
            Endpoint.Method.GET -> get
            Endpoint.Method.POST -> post
            Endpoint.Method.PUT -> put
            Endpoint.Method.DELETE -> delete
            Endpoint.Method.OPTIONS -> options
            Endpoint.Method.HEAD -> head
            Endpoint.Method.PATCH -> patch
            Endpoint.Method.TRACE -> trace
        }
    }
    .filterNotNullValues()

private fun OpenAPIV3Reference.getReference() = ref.value
    .split("/").getOrNull(3)
    ?: error("Wrong reference: ${ref.value}")

private fun OpenAPIV3Schema.toPrimitive() = when (this.type) {
    OpenAPIV3Type.STRING -> when {
        pattern != null -> Reference.Primitive.Type.String(
            constraint = Reference.Primitive.Type.Constraint.RegExp(
                pattern!!,
            ),
        )

        else -> Reference.Primitive.Type.String(null)
    }

    OpenAPIV3Type.INTEGER -> Reference.Primitive.Type.Integer(
        if (format == "int32") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64,
        null,
    )

    OpenAPIV3Type.NUMBER -> Reference.Primitive.Type.Number(
        if (format == "float") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64,
        null,
    )

    OpenAPIV3Type.BOOLEAN -> Reference.Primitive.Type.Boolean
    else -> error("Type is not a primitive")
}

private fun OpenAPIV3Model.toField(schema: OpenAPIV3Schema, name: String) = schema.properties.orEmpty().map { (key, value) ->
    val isNullable = !(schema.required?.contains(key) ?: false)
    when (value) {
        is OpenAPIV3Schema -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = emptyList(),
                reference = when {
                    value.enum != null -> toReference(value, isNullable, className(name, key))
                    value.type == OpenAPIV3Type.ARRAY -> toReference(
                        value,
                        isNullable,
                        className(name, key, "Array"),
                    )

                    else -> toReference(value, isNullable, className(name, key))
                },
            )
        }

        is OpenAPIV3Reference -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = emptyList(),
                reference = toReference(value, isNullable),
            )
        }
    }
}

private fun OpenAPIV3Model.toField(parameter: OpenAPIV3Parameter, name: String): Field {
    val isNullable = !(parameter.required ?: false)
    return when (val s = parameter.schema) {
        is OpenAPIV3Reference -> toReference(s, isNullable)
        is OpenAPIV3Schema -> toReference(s, isNullable, name + if (s.type == OpenAPIV3Type.ARRAY) "Array" else "")
        null -> Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = isNullable)
    }.let {
        Field(
            identifier = FieldIdentifier(parameter.name),
            annotations = emptyList(),
            reference = it,
        )
    }
}

private fun OpenAPIV3Model.toField(header: OpenAPIV3Header, identifier: String, name: String): Field {
    val isNullable = !(header.required ?: false)
    return when (val s = header.schema) {
        is OpenAPIV3Reference -> toReference(s, isNullable)
        is OpenAPIV3Schema -> toReference(s, isNullable, name)
        null -> Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = isNullable)
    }.let {
        Field(
            identifier = FieldIdentifier(identifier),
            annotations = emptyList(),
            reference = it,
        )
    }
}

private data class FlattenRequest(
    val path: Path,
    val pathItem: OpenAPIV3PathItem,
    val method: Endpoint.Method,
    val operation: OpenAPIV3Operation,
)

private fun OpenAPIV3Model.flatMapRequests(f: FlattenRequest.() -> List<Definition>) = paths
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList().map { (method, operation) ->
            FlattenRequest(path = path, pathItem = pathItem, method = method, operation = operation)
        }
    }
    .flatMap(f)

private data class FlattenResponse(
    val path: Path,
    val pathItem: OpenAPIV3PathItem,
    val method: Endpoint.Method,
    val operation: OpenAPIV3Operation,
    val statusCode: StatusCode,
    val response: OpenAPIV3ResponseOrReference,
)

private fun OpenAPIV3Model.flatMapResponses(f: FlattenResponse.() -> List<Definition>) = paths
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList()
            .flatMap { (method, operation) ->
                operation.responses?.map { (statusCode, response) ->
                    FlattenResponse(
                        path = path,
                        pathItem = pathItem,
                        method = method,
                        operation = operation,
                        statusCode = statusCode,
                        response = response,
                    )
                }.orEmpty()
            }
    }
    .flatMap(f)

private fun String.sanitize() = this
    .split(".", " ", "-")
    .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
    .joinToString("")
    .asSequence()
    .filter { it.isLetterOrDigit() || it in listOf('_') }
    .joinToString("")

private fun OpenAPIV3Type?.isPrimitive() = when (this) {
    OpenAPIV3Type.STRING -> true
    OpenAPIV3Type.NUMBER -> true
    OpenAPIV3Type.INTEGER -> true
    OpenAPIV3Type.BOOLEAN -> true
    OpenAPIV3Type.ARRAY -> false
    OpenAPIV3Type.OBJECT -> false
    null -> false
}

private fun Reference.toIterable(isNullable: Boolean) = Reference.Iterable(reference = this, isNullable = isNullable)

private fun Reference.toDict(isNullable: Boolean) = Reference.Dict(reference = this, isNullable = isNullable)
