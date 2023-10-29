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
import community.flock.wirespec.compiler.core.parse.nodes.Definition
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.openapi.Common.className
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
        parseEndpoint() + parseParameters() + parseRequestBody() + parseResponseBody() + parseComponents()

    private fun parseEndpoint(): List<Definition> = openApi.paths
        .flatMap { (key, path) ->
            path.toOperationList().map { (method, operation) ->
                val parameters = path.resolveParameters() + operation.resolveParameters()
                val segments = key.toSegments(parameters)
                val name = operation.toName(segments, method)
                val query = parameters
                    .filter { it.`in` == ParameterLocation.QUERY }
                    .map { it.toField(className(name, "Parameter", it.name)) }
                val headers = parameters
                    .filter { it.`in` == ParameterLocation.HEADER }
                    .map { it.toField(className(name, "Parameter", it.name)) }
                val cookies = parameters
                    .filter { it.`in` == ParameterLocation.COOKIE }
                    .map { it.toField(className(name, "Parameter", it.name)) }
                val requests = operation.requestBody?.resolve()
                    ?.let { requestBody ->
                        requestBody.content?.map { (mediaType, mediaObject) ->
                            Endpoint.Request(
                                Endpoint.Content(
                                    type = mediaType.value,
                                    reference = when (val schema = mediaObject.schema) {
                                        is ReferenceObject -> schema.toReference()
                                        is SchemaObject -> schema.toReference(
                                            className(name, "RequestBody")
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
                                        className(name, status.value, "ResponseBody")
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

    private fun parseParameters() = openApi.flatMapRequests { req ->
        val parameters = req.pathItem.resolveParameters() + req.operation.resolveParameters()
        val segments = req.path.toSegments(parameters)
        val name = req.operation.toName(segments, req.method)
        parameters.flatMap { parameter -> parameter.schema?.flatten(className(name, "Parameter", parameter.name)) ?: emptyList() }
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
                            .flatten(className(name, "RequestBody"))

                        OpenapiType.ARRAY -> schema.items
                            ?.flatten(className(name, "RequestBody")).orEmpty()

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
            is ResponseObject -> {
                response.content.orEmpty().flatMap { (_, mediaObject) ->
                    when (val schema = mediaObject.schema) {
                        is SchemaObject -> when (schema.type) {
                            null, OpenapiType.OBJECT -> schema
                                .flatten(className(name, res.statusCode.value, "ResponseBody"))

                            OpenapiType.ARRAY -> schema.items
                                ?.flatten(className(name, res.statusCode.value, "ResponseBody")).orEmpty()

                            else -> emptyList()
                        }

                        else -> emptyList()
                    }
                }
            }

            is ReferenceObject -> emptyList()
        }
    }


    private fun parseComponents(): List<Definition> = openApi.components?.schemas.orEmpty()
        .filter {
            when (val s = it.value) {
                is SchemaObject -> s.additionalProperties == null
                else -> false
            }
        }
        .flatMap { it.value.flatten(className(it.key)) }

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
        operationId?.let { className(it) } ?: segments
            .joinToString("") {
                when (it) {
                    is Endpoint.Segment.Literal -> className(it.value)
                    is Endpoint.Segment.Param -> className(it.identifier.value)
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
    ): List<Definition> =
        when {
            additionalProperties != null -> when (additionalProperties) {
                is BooleanObject -> emptyList()
                else -> additionalProperties
                    ?.resolve()
                    ?.takeIf { properties != null }
                    ?.flatten(name)
                    ?: emptyList()
            }

            oneOf != null -> TODO("oneOf is not implemented")
            anyOf != null -> TODO("anyOf is not implemented")
            allOf != null -> listOf(Type(name, Type.Shape(allOf.orEmpty().flatMap { it.resolve().toField(name) })))
                .plus(allOf!!.flatMap {
                    when (it) {
                        is ReferenceObject -> emptyList()
                        is SchemaObject -> it.properties.orEmpty().flatMap { (key, value) ->
                            when (value) {
                                is ReferenceObject -> emptyList()
                                is SchemaObject -> value.flatten(className(name, key))
                            }
                        }
                    }
                })

            enum != null -> enum!!
                .map { it.content }
                .toSet()
                .let { listOf(Enum(name, it)) }

            else -> when (type) {
                null, OpenapiType.OBJECT -> {
                    val fields = properties.orEmpty().flatMap { (key, value) ->
                        when (value) {
                            is SchemaObject -> value.flatten(className(name, key))
                            is ReferenceObject -> emptyList()
                        }
                    }
                    val schema = listOf(
                        Type(name, Type.Shape(this.toField(name)))
                    )

                    schema + fields
                }

                OpenapiType.ARRAY -> items
                    ?.let {
                        when (it) {
                            is ReferenceObject -> emptyList()
                            is SchemaObject -> it.flatten(className(name, "array"))
                        }
                    }
                    ?: emptyList()


                else -> emptyList()
            }
        }

    private fun SchemaOrReferenceObject.flatten(
        name: String,
    ): List<Definition> {
        return when (this) {
            is ReferenceObject -> emptyList()
            is SchemaObject -> this.flatten(name)
        }
    }

    private fun ReferenceObject.toReference(): Reference =
        resolveSchemaObject().let { (referencingObject, schema) ->
            when {
                schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
                    is BooleanObject -> Reference.Any(false, true)
                    is ReferenceObject -> additionalProperties.toReference().toMap()
                    is SchemaObject -> additionalProperties.toReference(getReference()).toMap()
                }

                schema.enum != null -> Reference.Custom(className(referencingObject.getReference()), false, false)
                schema.type.isPrimitive() -> Reference.Primitive(schema.type!!.toPrimitive(), false, false)
                else -> when (schema.type) {
                    OpenapiType.ARRAY -> when (val items = schema.items) {
                        is ReferenceObject -> Reference.Custom(className(items.getReference()), true)
                        is SchemaObject -> Reference.Custom(className(referencingObject.getReference(), "Array"), true)
                        null -> error("items cannot be null when type is array: ${this.ref}")
                    }

                    else -> Reference.Custom(className(referencingObject.getReference()), false)

                }
            }
        }


    private fun SchemaObject.toReference(name: String): Reference = when {
        additionalProperties != null -> when (val additionalProperties = additionalProperties!!) {
            is BooleanObject -> Reference.Any(false, true)
            is ReferenceObject -> Reference.Any(false, true)
            is SchemaObject -> additionalProperties
                .takeIf { properties != null }
                ?.run {  toReference(name).toMap() }
                ?: Reference.Any(false, true)
        }

        enum != null -> Reference.Custom(name, false, additionalProperties != null)
        else -> when (val type = type) {
            OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Primitive(
                type.toPrimitive(),
                false,
                additionalProperties != null
            )

            null, OpenapiType.OBJECT ->
                when {
                    additionalProperties is BooleanObject -> Reference.Any(false, additionalProperties != null)
                    else -> Reference.Custom(name, false, additionalProperties != null)
                }

            OpenapiType.ARRAY -> {
                when (val it = items) {
                    is ReferenceObject -> it.toReference().toIterable()
                    is SchemaObject -> it.toReference(name).toIterable()
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

    private fun ReferenceObject.getReference() = this.ref.value
        .split("/").getOrNull(3)
        ?: error("Wrong reference: ${this.ref.value}")

    private fun OpenapiType.toPrimitive() = when (this) {
        OpenapiType.STRING -> Primitive.Type.String
        OpenapiType.INTEGER -> Primitive.Type.Integer
        OpenapiType.NUMBER -> Primitive.Type.Integer
        OpenapiType.BOOLEAN -> Primitive.Type.Boolean
        else -> error("Type is not a primitive")
    }

    private fun SchemaObject.toField(name: String) = properties.orEmpty().map { (key, value) ->
        when (value) {
            is SchemaObject -> {
                Field(
                    identifier = Field.Identifier(key),
                    reference = when(value.type){
                        OpenapiType.ARRAY -> value.toReference(className(name, key, "Array"))
                        else -> value.toReference(className(name, key))
                    },
                    isNullable = !(this.required?.contains(key) ?: false)
                )
            }

            is ReferenceObject -> {
                Field(
                    Field.Identifier(key),
                    Reference.Custom(value.getReference(), false),
                    !(this.required?.contains(key) ?: false)
                )
            }
        }
    }

    private fun ParameterObject.toField(name: String) =
        when (val s = schema) {
            is ReferenceObject -> s.toReference()
            is SchemaObject -> s.toReference(name)
            null -> TODO()
        }
            .let { Field(Field.Identifier(this.name), it, !(this.required ?: false)) }


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
    is Reference.Custom -> this.copy(isIterable = true)
    is Reference.Any -> this.copy(isIterable = true)
    is Reference.Primitive -> this.copy(isIterable = true)
}

private fun Reference.toMap() = when (this) {
    is Reference.Custom -> this.copy(isMap = true)
    is Reference.Any -> this.copy(isMap = true)
    is Reference.Primitive -> this.copy(isMap = true)
}
