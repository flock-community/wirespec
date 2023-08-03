package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.v3.BooleanObject
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
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.openapi.Common
import community.flock.kotlinx.openapi.bindings.v3.Type as OpenapiType

class OpenApiParser(private val openApi: OpenAPIObject) {

    companion object {
        fun parse(json: String): List<Definition> =
            OpenAPI
                .decodeFromString(json)
                .let { OpenApiParser(it).parse() }

        fun parse(openApi: OpenAPIObject): List<Definition> =
            OpenApiParser(openApi)
                .parse()
    }


    private fun parse(): List<Definition> =
        parseEndpoint() + parseRequestBody() + parseResponseBody() + parseComponents()

    private fun parseEndpoint(): List<Definition> = openApi.paths
        .flatMap { (key, path) ->
            path.toOperationList().map { (method, operation) ->
                val parameters = path.resolveParameters() + operation.resolveParameters().orEmpty()
                val segments = key.toSegments(parameters)
                val name = operation.toName(segments, method)
                val query = parameters
                    .filter { it.`in` == ParameterLocation.QUERY }
                    .map { it.toField() }
                val headers = parameters
                    .filter { it.`in` == ParameterLocation.HEADER }
                    .map { it.toField() }
                val cookies = parameters
                    .filter { it.`in` == ParameterLocation.COOKIE }
                    .map { it.toField() }
                val requests = operation.requestBody?.resolve()
                    ?.let { requestBody ->
                        requestBody.content?.map { (mediaType, mediaObject) ->
                            Endpoint.Request(
                                Endpoint.Content(
                                    type = mediaType.value,
                                    reference = when (val schema = mediaObject.schema) {
                                        is ReferenceObject -> schema.toReference()
                                        is SchemaObject -> Reference.Custom(
                                            Common.className(name, "RequestBody"),
                                            true
                                        )

                                        null -> TODO()
                                    },
                                    isNullable = requestBody.required ?: false
                                )
                            )
                        }
                    }
                    ?: listOf(
                        Endpoint.Request(null)
                    )

                val responses = operation.responses.orEmpty().flatMap { (status, res) ->
                    res.resolve().content?.map { (contentType, media) ->
                        Endpoint.Response(
                            status = status.value,
                            content = Endpoint.Content(
                                type = contentType.value,
                                reference = when (val schema = media.schema) {
                                    is ReferenceObject -> schema.toReference()
                                    is SchemaObject -> schema.toReference(
                                        Common.className(
                                            name,
                                            status.value,
                                            "ResponseBody",
                                        )
                                    )

                                    null -> TODO()
                                },
                                isNullable = media.schema?.resolve()?.nullable ?: false
                            )
                        )
                    }
                        ?: listOf(
                            Endpoint.Response(
                                status = status.value,
                                content = null
                            )
                        )
                }

                Endpoint(
                    name = name,
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

    private fun parseRequestBody() = openApi.flatMapRequests { req ->
        val parameters = req.pathItem.resolveParameters() + req.operation.resolveParameters()
        val segments = req.path.toSegments(parameters)
        val name = req.operation.toName(segments, req.method)
        req.operation.requestBody?.resolve()?.content.orEmpty()
            .flatMap { (_, mediaObject) ->
                when (val schema = mediaObject.schema) {
                    is SchemaObject -> when (schema.type) {
                        null, OpenapiType.OBJECT -> schema
                            .flatten(Common.className(name, "RequestBody"))
                            .map {
                                Type(
                                    it.name,
                                    Type.Shape(it.properties)
                                )
                            }

                        else -> emptyList()
                    }

                    is ReferenceObject -> emptyList()
                    null -> emptyList()
                }
            }
    }

    private fun parseResponseBody() = openApi.flatMapResponses { res ->
        val parameters = res.pathItem.resolveParameters() + (res.operation.resolveParameters())
        val segments = res.path.toSegments(parameters)
        val name = res.operation.toName(segments, res.method)
        when (val response = res.response) {
            is ReferenceObject -> emptyList()
            is ResponseObject -> {
                response.content.orEmpty().flatMap { (_, mediaObject) ->
                    when (val schema = mediaObject.schema) {
                        is SchemaObject -> when (schema.type) {
                            null, OpenapiType.OBJECT -> (
                                    schema.additionalProperties?.resolve()
                                        ?.flatten(Common.className(name, res.statusCode.value, "ResponseBody"))
                                        ?: schema.flatten(Common.className(name, res.statusCode.value, "ResponseBody")))
                                .map {
                                    Type(
                                        it.name,
                                        Type.Shape(it.properties)
                                    )
                                }

                            else -> emptyList()
                        }

                        is ReferenceObject -> emptyList()
                        null -> emptyList()
                    }
                }
            }
        }
    }


    private fun parseComponents(): List<Definition> = openApi.components?.schemas
        ?.flatMap { it.value.flatten(Common.className(it.key)) }
        ?.map { Type(it.name, Type.Shape(it.properties)) }
        ?: emptyList()

    private fun Path.toSegments(parameters: List<ParameterObject>) = value.split("/").drop(1).map { segment ->
        val isParam = segment[0] == '{' && segment[segment.length - 1] == '}'
        when {
            isParam -> {
                val param = segment.substring(1, segment.length - 1)
                parameters
                    .find { it.name == param }
                    ?.schema
                    ?.resolve()
                    ?.let { it.type?.toPrimitive() }
                    ?.let {
                        Endpoint.Segment.Param(
                            Field.Identifier(param),
                            Primitive(it, false)
                        )
                    }
                    ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
            }

            else -> Endpoint.Segment.Literal(segment)
        }
    }

    private fun OperationObject.toName(segments: List<Endpoint.Segment>, method: Endpoint.Method) =
        operationId?.let { Common.className(it) } ?: segments
            .joinToString("") {
                when (it) {
                    is Endpoint.Segment.Literal -> Common.className(it.value)
                    is Endpoint.Segment.Param -> Common.className(it.identifier.value)
                }
            }
            .let { it + method.name }

    private fun OperationObject.resolveParameters(): List<ParameterObject> = parameters
        ?.mapNotNull {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> it.resolveParameterObject()
            }
        }
        ?: emptyList()

    private fun PathItemObject.resolveParameters(): List<ParameterObject> = parameters
        ?.mapNotNull {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> it.resolveParameterObject()
            }
        }
        ?: emptyList()


    private fun ReferenceObject.resolveParameterObject(): ParameterObject? =
        openApi.components?.parameters
            ?.get(getReference())
            ?.let {
                when (it) {
                    is ParameterObject -> it
                    is ReferenceObject -> it.resolveParameterObject()
                }
            }

    private fun ReferenceObject.resolveSchemaObject(): Pair<ReferenceObject, SchemaObject> =
        openApi.components?.schemas
            ?.get(getReference())
            ?.let {
                when (it) {
                    is SchemaObject -> this to it
                    is ReferenceObject -> it.resolveSchemaObject()
                }
            }
            ?: error("Cannot resolve ref: $ref")

    private fun ReferenceObject.resolveRequestBodyObject(): Pair<ReferenceObject, RequestBodyObject> =
        openApi.components?.requestBodies
            ?.get(getReference())
            ?.let {
                when (it) {
                    is RequestBodyObject -> this to it
                    is ReferenceObject -> it.resolveRequestBodyObject()
                }
            }
            ?: error("Cannot resolve ref: $ref")

    private fun ReferenceObject.resolveResponseObject(): Pair<ReferenceObject, ResponseObject> =
        openApi.components?.responses
            ?.get(getReference())
            ?.let {
                when (it) {
                    is ResponseObject -> this to it
                    is ReferenceObject -> it.resolveResponseObject()
                }
            }
            ?: error("Cannot resolve ref: $ref")

    private fun SchemaOrReferenceObject.resolve(): SchemaObject =
        when (this) {
            is SchemaObject -> this
            is ReferenceObject -> this.resolveSchemaObject().second
        }

    private fun SchemaOrReferenceOrBooleanObject.resolve(): SchemaObject =
        when (this) {
            is SchemaObject -> this
            is ReferenceObject -> this.resolveSchemaObject().second
            is BooleanObject -> TODO()
        }

    private fun RequestBodyOrReferenceObject.resolve(): RequestBodyObject =
        when (this) {
            is RequestBodyObject -> this
            is ReferenceObject -> this.resolveRequestBodyObject().second
        }

    private fun ResponseOrReferenceObject.resolve(): ResponseObject =
        when (this) {
            is ResponseObject -> this
            is ReferenceObject -> this.resolveResponseObject().second
        }

    private fun SchemaObject.flatten(
        name: String,
    ): List<SimpleSchema> =
        when (type) {
            null, OpenapiType.OBJECT -> {

                val fields = properties.orEmpty().flatMap { (key, value) ->
                    when (value) {
                        is SchemaObject -> value.flatten(Common.className(name, key))
                        is ReferenceObject -> emptyList()
                    }
                }

                val schema = when (additionalProperties) {
                    null -> listOf(
                        SimpleSchema(
                            name = name,
                            properties = properties?.map { (key, value) ->
                                when (value) {
                                    is SchemaObject -> {
                                        Field(
                                            Field.Identifier(key),
                                            value.toReference(Common.className(name, key)),
                                            !(this.required?.contains(key) ?: false)
                                        )
                                    }

                                    is ReferenceObject -> Field(
                                        Field.Identifier(key),
                                        value.toReference(),
                                        !(this.required?.contains(key) ?: false)
                                    )
                                }
                            } ?: emptyList()
                        ))

                    else -> emptyList()
                }

                schema + fields
            }

            OpenapiType.ARRAY -> items
                ?.let {
                    when (it) {
                        is ReferenceObject -> emptyList()
                        is SchemaObject -> it.flatten(Common.className(name, "array"))
                    }
                }
                ?: emptyList()


            else -> emptyList()
        }

    private fun SchemaOrReferenceObject.flatten(
        name: String,
    ): List<SimpleSchema> {
        return when (this) {
            is SchemaObject -> this
                .flatten(name)

            is ReferenceObject -> this
                .resolveSchemaObject()
                .second
                .flatten(name)
        }
    }

    private data class SimpleSchema(val name: String, val properties: List<Field>)

    private fun ReferenceObject.toReference(): Reference.Custom {
        val (referencingObject, schema) = resolveSchemaObject()

        if (schema.additionalProperties != null) {
            return when (val additionalProperties = schema.additionalProperties) {
                is BooleanObject -> TODO()
                is ReferenceObject -> Reference.Custom(
                    Common.className(additionalProperties.getReference()),
                    false,
                    true
                )

                is SchemaObject -> Reference.Custom(Common.className(referencingObject.getReference()), false, true)
                null -> TODO()
            }
        }
        return when (schema.type) {
            OpenapiType.ARRAY -> when (val items = schema.items) {
                is ReferenceObject -> Reference.Custom(Common.className(items.getReference()), true)
                is SchemaObject -> Reference.Custom(Common.className(referencingObject.getReference(), "array"), true)
                else -> TODO()
            }

            else -> Reference.Custom(Common.className(referencingObject.getReference()), false)
        }
    }

    private fun SchemaObject.toReference(name: String): Reference = when (val t = type) {
        OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Primitive(
            t.toPrimitive(),
            false
        )

        null, OpenapiType.OBJECT -> Reference.Custom(name, false, additionalProperties != null)

        OpenapiType.ARRAY -> {
            val resolve = items?.resolve()
            when (val type = resolve?.type) {
                OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Primitive(
                    type.toPrimitive(),
                    true
                )

                else -> when (val it = items) {
                    is ReferenceObject -> it.toReference().copy(isIterable = true)
                    is SchemaObject -> it.toReference(name)
                    null -> error("When schema is of type array items cannot be null for name: $name")
                }
            }
        }
    }

    private fun PathItemObject.toOperationList() = Endpoint.Method.values()
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

    private fun ReferenceObject.getReference() = this.ref.value.split("/")[3]

    private fun OpenapiType.toPrimitive() = when (this) {
        OpenapiType.STRING -> Primitive.Type.String
        OpenapiType.INTEGER -> Primitive.Type.Integer
        OpenapiType.NUMBER -> Primitive.Type.Integer
        OpenapiType.BOOLEAN -> Primitive.Type.Boolean
        else -> error("Type is not a primitive")
    }

    private fun ParameterObject.toField() =
        when (schema) {
            is ReferenceObject -> Reference.Custom((schema as ReferenceObject).getReference(), false)
            is SchemaObject -> {
                when (val type = (schema as SchemaObject).type) {
                    OpenapiType.STRING, OpenapiType.INTEGER, OpenapiType.NUMBER, OpenapiType.BOOLEAN -> Primitive(
                        type.toPrimitive(),
                        false
                    )

                    OpenapiType.ARRAY -> TODO()
                    OpenapiType.OBJECT -> TODO()
                    null -> TODO()
                }
            }

            null -> TODO()
        }
            .let { Field(Field.Identifier(name), it, !(this.required ?: false)) }


    private data class FlattenRequest(
        val path: Path,
        val pathItem: PathItemObject,
        val method: Endpoint.Method,
        val operation: OperationObject
    )

    private fun <T> OpenAPIObject.flatMapRequests(f: (req: FlattenRequest) -> List<T>) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList().map { (method, operation) ->
                FlattenRequest(path, pathItem, method, operation)
            }
        }
        .flatMap { f(it) }

    private data class FlattenResponse(
        val path: Path,
        val pathItem: PathItemObject,
        val method: Endpoint.Method,
        val operation: OperationObject,
        val statusCode: StatusCode,
        val response: ResponseOrReferenceObject,
    )

    private fun <T> OpenAPIObject.flatMapResponses(f: (res: FlattenResponse) -> List<T>) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList()
                .flatMap { (method, operation) ->
                    operation.responses?.map { (statusCode, response) ->
                        FlattenResponse(
                            path,
                            pathItem,
                            method,
                            operation,
                            statusCode,
                            response,
                        )
                    }.orEmpty()
                }
        }
        .flatMap { f(it) }


}

private fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
    mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
