package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.v3.BooleanObject
import community.flock.kotlinx.openapi.bindings.v3.HeaderObject
import community.flock.kotlinx.openapi.bindings.v3.HeaderOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.kotlinx.openapi.bindings.v3.OpenAPIObject
import community.flock.kotlinx.openapi.bindings.v3.OperationObject
import community.flock.kotlinx.openapi.bindings.v3.ParameterLocation
import community.flock.kotlinx.openapi.bindings.v3.ParameterObject
import community.flock.kotlinx.openapi.bindings.v3.Path
import community.flock.kotlinx.openapi.bindings.v3.PathItemObject
import community.flock.kotlinx.openapi.bindings.v3.ReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.RequestBodyObject
import community.flock.kotlinx.openapi.bindings.v3.RequestBodyOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.ResponseObject
import community.flock.kotlinx.openapi.bindings.v3.ResponseOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaOrReferenceOrBooleanObject
import community.flock.kotlinx.openapi.bindings.v3.StatusCode
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Field.Reference
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.openapi.Common.className
import community.flock.wirespec.openapi.Common.filterNotNullValues
import kotlinx.serialization.json.Json
import community.flock.kotlinx.openapi.bindings.v3.Type as OpenapiType

object OpenApiV3Parser {

    fun parse(json: String, strict: Boolean = false): AST =
        OpenAPI(json = Json { prettyPrint = true; ignoreUnknownKeys = strict })
            .decodeFromString(json)
            .parse()

    fun OpenAPIObject.parse(): AST = listOf(
        parseEndpoint(),
        parseParameters(),
        parseRequestBody(),
        parseResponseBody(),
        parseComponents(),
    ).reduce(AST::plus)

    private fun OpenAPIObject.parseEndpoint(): AST = paths
        .flatMap { (key, path) ->
            path.toOperationList().map { (method, operation) ->
                val parameters = resolveParameters(path) + resolveParameters(operation)
                val segments = toSegments(key, parameters)
                val name = operation.toName() ?: (key.toName() + method.name)
                val query = parameters
                    .filter { it.`in` == ParameterLocation.QUERY }
                    .map { toField(it, className(name, "Parameter", it.name)) }
                val headers = parameters
                    .filter { it.`in` == ParameterLocation.HEADER }
                    .map { toField(it, className(name, "Parameter", it.name)) }
                val cookies = parameters
                    .filter { it.`in` == ParameterLocation.COOKIE }
                    .map { toField(it, className(name, "Parameter", it.name)) }
                val requests = operation.requestBody?.let { resolve(it) }
                    ?.let { requestBody ->
                        requestBody.content?.map { (mediaType, mediaObject) ->
                            Endpoint.Request(
                                Endpoint.Content(
                                    type = mediaType.value,
                                    reference = when (val schema = mediaObject.schema) {
                                        is ReferenceObject -> toReference(schema)
                                        is SchemaObject -> toReference(schema, className(name, "RequestBody"))
                                        null -> TODO("Not yet implemented")
                                    },
                                    isNullable = !(requestBody.required ?: false)
                                )
                            )
                        }
                    }
                    ?: listOf(Endpoint.Request(null))

                val responses = operation.responses.orEmpty().flatMap { (status, res) ->
                    resolve(res).let { response ->
                        response.content?.map { (contentType, media) ->
                            Endpoint.Response(
                                status = status.value,
                                headers = response.headers?.map { entry ->
                                    toField(resolve(entry.value), entry.key, className(name, "ResponseHeader"))
                                }.orEmpty(),
                                content = Endpoint.Content(
                                    type = contentType.value,
                                    reference = when (val schema = media.schema) {
                                        is ReferenceObject -> toReference(schema)
                                        is SchemaObject -> toReference(
                                            schema,
                                            className(name, status.value, contentType.value, "ResponseBody")
                                        )

                                        null -> Reference.Any(false)
                                    },
                                    isNullable = media.schema?.let { resolve(it) }?.nullable ?: false
                                )
                            )
                        }
                    }
                        ?: listOf(
                            Endpoint.Response(
                                status = status.value,
                                headers = emptyList(),
                                content = null
                            )
                        )
                }

                Endpoint(
                    comment = null,
                    identifier = Identifier(name),
                    method = method,
                    path = segments,
                    query = query,
                    headers = headers,
                    cookies = cookies,
                    requests = requests,
                    responses = responses,
                )
            }
        }

    private fun OpenAPIObject.parseParameters(): AST = flatMapRequests {
        val parameters = resolveParameters(pathItem) + resolveParameters(operation)
        val name = operation.toName() ?: (path.toName() + method.name)
        parameters.flatMap { parameter ->
            parameter.schema?.let { flatten(it, className(name, "Parameter", parameter.name)) } ?: emptyList()
        }
    }

    private fun OpenAPIObject.parseRequestBody(): AST = flatMapRequests {
        val name = operation.toName() ?: (path.toName() + method.name)
        operation.requestBody?.let { resolve(it) }?.content.orEmpty()
            .flatMap { (_, mediaObject) ->
                when (val schema = mediaObject.schema) {
                    is SchemaObject -> when (schema.type) {
                        null, OpenapiType.OBJECT -> flatten(schema, className(name, "RequestBody"))

                        OpenapiType.ARRAY -> schema.items?.let { flatten(it, className(name, "RequestBody")) }.orEmpty()

                        else -> emptyList()
                    }

                    is ReferenceObject, null -> emptyList()
                }
            }
    }

    private fun OpenAPIObject.parseResponseBody(): AST = flatMapResponses {
        val name = operation.toName() ?: (path.toName() + method.name)
        when (val response = response) {
            is ResponseObject -> {
                response.content.orEmpty().flatMap { (mediaType, mediaObject) ->
                    when (val schema = mediaObject.schema) {
                        is SchemaObject -> when (schema.type) {
                            null, OpenapiType.OBJECT -> flatten(
                                schema,
                                className(name, statusCode.value, mediaType.value, "ResponseBody")
                            )

                            OpenapiType.ARRAY -> schema.items?.let {
                                flatten(
                                    it,
                                    className(name, statusCode.value, mediaType.value, "ResponseBody")
                                )
                            }.orEmpty()

                            else -> emptyList()
                        }

                        else -> emptyList()
                    }
                }
            }

            is ReferenceObject -> emptyList()
        }
    }

    private fun OpenAPIObject.parseComponents(): AST = components?.schemas.orEmpty()
        .filter {
            when (val s = it.value) {
                is SchemaObject -> s.additionalProperties == null
                else -> false
            }
        }
        .flatMap { flatten(it.value, className(it.key)) }

    private fun String.isParam() = this[0] == '{' && this[length - 1] == '}'

    private fun OperationObject.toName() = operationId?.let { className(it) }

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

    private fun OpenAPIObject.toSegments(path: Path, parameters: List<ParameterObject>) =
        path.value.split("/").drop(1).filter { it.isNotBlank() }.map { segment ->
            when (segment.isParam()) {
                true -> {
                    val param = segment.substring(1, segment.length - 1)
                    val name = path.toName()
                    parameters
                        .find { it.name == param }
                        ?.schema
                        ?.let { resolve(it) }
                        ?.let { toReference(it, className(name, "Parameter", param)) }
                        ?.let {
                            Endpoint.Segment.Param(
                                Identifier(param),
                                it
                            )
                        }
                        ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
                }

                false -> Endpoint.Segment.Literal(segment)
            }
        }

    private fun OpenAPIObject.resolveParameters(operation: OperationObject): List<ParameterObject> =
        operation.parameters
            ?.mapNotNull {
                when (it) {
                    is ParameterObject -> it
                    is ReferenceObject -> resolveParameterObject(it)
                }
            }
            ?: emptyList()

    private fun OpenAPIObject.resolveParameters(pathItem: PathItemObject): List<ParameterObject> = pathItem.parameters
        ?.mapNotNull {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> resolveParameterObject(it)
            }
        }
        ?: emptyList()

    private fun OpenAPIObject.resolveParameterObject(reference: ReferenceObject): ParameterObject? =
        components?.parameters
            ?.get(reference.getReference())
            ?.let {
                when (it) {
                    is ParameterObject -> it
                    is ReferenceObject -> resolveParameterObject(it)
                }
            }

    private fun OpenAPIObject.resolveSchemaObject(reference: ReferenceObject): Pair<ReferenceObject, SchemaObject> =
        components?.schemas
            ?.get(reference.getReference())
            ?.let {
                when (it) {
                    is SchemaObject -> reference to it
                    is ReferenceObject -> resolveSchemaObject(it)
                }
            }
            ?: error("Cannot resolve ref: ${reference.ref}")

    private fun OpenAPIObject.resolveHeaderObject(reference: ReferenceObject): Pair<ReferenceObject, HeaderObject> =
        components?.headers
            ?.get(reference.getReference())
            ?.let {
                when (it) {
                    is HeaderObject -> reference to it
                    is ReferenceObject -> resolveHeaderObject(it)
                }
            }
            ?: error("Cannot resolve ref: ${reference.ref}")

    private fun OpenAPIObject.resolveRequestBodyObject(reference: ReferenceObject): Pair<ReferenceObject, RequestBodyObject> =
        components?.requestBodies
            ?.get(reference.getReference())
            ?.let {
                when (it) {
                    is RequestBodyObject -> reference to it
                    is ReferenceObject -> resolveRequestBodyObject(it)
                }
            }
            ?: error("Cannot resolve ref: ${reference.ref}")

    private fun OpenAPIObject.resolveResponseObject(reference: ReferenceObject): Pair<ReferenceObject, ResponseObject> =
        components?.responses
            ?.get(reference.getReference())
            ?.let {
                when (it) {
                    is ResponseObject -> reference to it
                    is ReferenceObject -> resolveResponseObject(it)
                }
            }
            ?: error("Cannot resolve ref: ${reference.ref}")

    private fun OpenAPIObject.resolve(schemaOrReference: SchemaOrReferenceObject): SchemaObject =
        when (schemaOrReference) {
            is SchemaObject -> schemaOrReference
            is ReferenceObject -> resolveSchemaObject(schemaOrReference).second
        }

    private fun OpenAPIObject.resolve(headerOrReference: HeaderOrReferenceObject): HeaderObject =
        when (headerOrReference) {
            is HeaderObject -> headerOrReference
            is ReferenceObject -> resolveHeaderObject(headerOrReference).second
        }

    private fun OpenAPIObject.resolve(schemaOrReferenceOrBoolean: SchemaOrReferenceOrBooleanObject): SchemaObject =
        when (schemaOrReferenceOrBoolean) {
            is SchemaObject -> schemaOrReferenceOrBoolean
            is ReferenceObject -> resolveSchemaObject(schemaOrReferenceOrBoolean).second
            is BooleanObject -> TODO("Not yet implemented")
        }

    private fun OpenAPIObject.resolve(requestBodyOrReference: RequestBodyOrReferenceObject): RequestBodyObject =
        when (requestBodyOrReference) {
            is RequestBodyObject -> requestBodyOrReference
            is ReferenceObject -> resolveRequestBodyObject(requestBodyOrReference).second
        }

    private fun OpenAPIObject.resolve(responseOrReferenceObject: ResponseOrReferenceObject): ResponseObject =
        when (responseOrReferenceObject) {
            is ResponseObject -> responseOrReferenceObject
            is ReferenceObject -> resolveResponseObject(responseOrReferenceObject).second
        }

    private fun OpenAPIObject.flatten(schemaObject: SchemaObject, name: String): AST =
        when {
            schemaObject.additionalProperties != null -> when (schemaObject.additionalProperties) {
                is BooleanObject -> emptyList()
                else -> schemaObject.additionalProperties
                    ?.let { resolve(it) }
                    ?.takeIf { it.properties != null }
                    ?.let { flatten(it, name) }
                    ?: emptyList()
            }

            schemaObject.oneOf != null || schemaObject.anyOf != null -> listOf(
                Union(
                    comment = null,
                    identifier = Identifier(name),
                    entries = schemaObject.oneOf!!
                        .mapIndexed { index, it ->
                            when (it) {
                                is ReferenceObject -> toReference(it)
                                is SchemaObject -> toReference(it, className(name, index.toString()))
                            }

                        }.toSet()
                )
            )
                .plus(schemaObject.oneOf!!.flatMapIndexed { index, it ->
                    when (it) {
                        is ReferenceObject -> emptyList()
                        is SchemaObject -> flatten(it, className(name, index.toString()))
                    }
                })

            schemaObject.allOf != null -> listOf(
                Type(
                    comment = null,
                    identifier = Identifier(name),
                    shape = Type.Shape(schemaObject.allOf.orEmpty().flatMap { toField(resolve(it), name) }
                        .distinctBy { it.identifier })
                )
            )
                .plus(
                    schemaObject.allOf!!
                        .flatMap {
                            when (it) {
                                is ReferenceObject -> resolveSchemaObject(it).second.properties.orEmpty()
                                is SchemaObject -> it.properties.orEmpty()
                            }
                                .flatMap { (key, value) ->
                                    flatten(value, className(name, key))
                                }
                        })

            schemaObject.enum != null -> schemaObject.enum!!
                .map { it.content }
                .toSet()
                .let { listOf(Enum(comment = null, identifier = Identifier(name), entries = it)) }

            else -> when (schemaObject.type) {
                null, OpenapiType.OBJECT -> {
                    val fields = schemaObject.properties.orEmpty().flatMap { (key, value) ->
                        flatten(value, className(name, key))
                    }
                    val schema = listOf(
                        Type(
                            comment = null,
                            identifier = Identifier(name),
                            shape = Type.Shape(toField(schemaObject, name))
                        )
                    )

                    schema + fields
                }

                OpenapiType.ARRAY -> schemaObject.items
                    ?.let {
                        when (it) {
                            is ReferenceObject -> emptyList()
                            is SchemaObject -> flatten(it, className(name, "array"))
                        }
                    }
                    ?: emptyList()


                else -> emptyList()
            }
        }

    private fun OpenAPIObject.flatten(schemaOrReference: SchemaOrReferenceObject, name: String) =
        when (schemaOrReference) {
            is SchemaObject -> flatten(schemaOrReference, name)
            is ReferenceObject -> emptyList()
        }

    private fun OpenAPIObject.toReference(reference: ReferenceObject): Reference =
        resolveSchemaObject(reference).let { (referencingObject, schema) ->
            when {
                schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
                    is BooleanObject -> Reference.Any(isIterable = false, isDictionary = true)
                    is ReferenceObject -> toReference(additionalProperties).toMap()
                    is SchemaObject -> toReference(additionalProperties, reference.getReference()).toMap()
                }

                schema.enum != null -> Reference.Custom(
                    className(referencingObject.getReference()),
                    isIterable = false,
                    isDictionary = false
                )

                schema.type.isPrimitive() -> Reference.Primitive(
                    schema.type!!.toPrimitive(),
                    isIterable = false,
                    isDictionary = false
                )

                else -> when (schema.type) {
                    OpenapiType.ARRAY -> when (val items = schema.items) {
                        is ReferenceObject -> Reference.Custom(className(items.getReference()), true)
                        is SchemaObject -> Reference.Custom(className(referencingObject.getReference(), "Array"), true)
                        null -> error("items cannot be null when type is array: ${reference.ref}")
                    }

                    else -> Reference.Custom(className(referencingObject.getReference()), false)

                }
            }
        }

    private fun OpenAPIObject.toReference(schema: SchemaObject, name: String): Reference = when {
        schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanObject -> Reference.Any(isIterable = false, isDictionary = true)
            is ReferenceObject -> toReference(additionalProperties).toMap()
            is SchemaObject -> additionalProperties
                .takeIf { it.type.isPrimitive() || it.properties != null }
                ?.let { toReference(it, name).toMap() }
                ?: Reference.Any(isIterable = false, isDictionary = true)
        }

        schema.enum != null -> Reference.Custom(name, false, schema.additionalProperties != null)
        else -> when (val type = schema.type) {
            OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Reference.Primitive(
                type.toPrimitive(),
                false,
                schema.additionalProperties != null
            )

            null, OpenapiType.OBJECT ->
                when {
                    schema.additionalProperties is BooleanObject -> Reference.Any(
                        false,
                        schema.additionalProperties != null
                    )

                    else -> Reference.Custom(name, false, schema.additionalProperties != null)
                }

            OpenapiType.ARRAY -> {
                when (val it = schema.items) {
                    is ReferenceObject -> toReference(it).toIterable()
                    is SchemaObject -> toReference(it, name).toIterable()
                    null -> error("When schema is of type array items cannot be null for name: $name")
                }
            }
        }
    }

    private fun PathItemObject.toOperationList() = Endpoint.Method.entries
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

    private fun ReferenceObject.getReference() = ref.value
        .split("/").getOrNull(3)
        ?: error("Wrong reference: ${ref.value}")

    private fun OpenapiType.toPrimitive() = when (this) {
        OpenapiType.STRING -> Reference.Primitive.Type.String
        OpenapiType.INTEGER -> Reference.Primitive.Type.Integer
        OpenapiType.NUMBER -> Reference.Primitive.Type.Number
        OpenapiType.BOOLEAN -> Reference.Primitive.Type.Boolean
        else -> error("Type is not a primitive")
    }

    private fun OpenAPIObject.toField(schema: SchemaObject, name: String) =
        schema.properties.orEmpty().map { (key, value) ->
            when (value) {
                is SchemaObject -> {
                    Field(
                        identifier = Identifier(key),
                        reference = when {
                            value.enum != null -> toReference(value, className(name, key))
                            value.type == OpenapiType.ARRAY -> toReference(value, className(name, key, "Array"))
                            else -> toReference(value, className(name, key))
                        },
                        isNullable = !(schema.required?.contains(key) ?: false)
                    )
                }

                is ReferenceObject -> {
                    Field(
                        Identifier(key),
                        Reference.Custom(className(value.getReference()), false),
                        !(schema.required?.contains(key) ?: false)
                    )
                }
            }
        }

    private fun OpenAPIObject.toField(parameter: ParameterObject, name: String) =
        when (val s = parameter.schema) {
            is ReferenceObject -> toReference(s)
            is SchemaObject -> toReference(s, name)
            null -> TODO("Not yet implemented")
        }.let { Field(Identifier(parameter.name), it, !(parameter.required ?: false)) }

    private fun OpenAPIObject.toField(header: HeaderObject, identifier: String, name: String) =
        when (val s = header.schema) {
            is ReferenceObject -> toReference(s)
            is SchemaObject -> toReference(s, name)
            null -> TODO("Not yet implemented")
        }.let { Field(Identifier(identifier), it, !(header.required ?: false)) }

    private data class FlattenRequest(
        val path: Path,
        val pathItem: PathItemObject,
        val method: Endpoint.Method,
        val operation: OperationObject
    )

    private fun OpenAPIObject.flatMapRequests(f: FlattenRequest.() -> AST) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList().map { (method, operation) ->
                FlattenRequest(path = path, pathItem = pathItem, method = method, operation = operation)
            }
        }
        .flatMap(f)

    private data class FlattenResponse(
        val path: Path,
        val pathItem: PathItemObject,
        val method: Endpoint.Method,
        val operation: OperationObject,
        val statusCode: StatusCode,
        val response: ResponseOrReferenceObject,
    )

    private fun OpenAPIObject.flatMapResponses(f: FlattenResponse.() -> AST) = paths
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
}

private fun OpenapiType?.isPrimitive() = when (this) {
    OpenapiType.STRING -> true
    OpenapiType.NUMBER -> true
    OpenapiType.INTEGER -> true
    OpenapiType.BOOLEAN -> true
    OpenapiType.ARRAY -> false
    OpenapiType.OBJECT -> false
    null -> false
}

private fun Reference.toIterable() = when (this) {
    is Reference.Custom -> copy(isIterable = true)
    is Reference.Any -> copy(isIterable = true)
    is Reference.Primitive -> copy(isIterable = true)
    is Reference.Unit -> copy(isIterable = true)
}

private fun Reference.toMap() = when (this) {
    is Reference.Custom -> copy(isDictionary = true)
    is Reference.Any -> copy(isDictionary = true)
    is Reference.Primitive -> copy(isDictionary = true)
    is Reference.Unit -> copy(isDictionary = true)
}
