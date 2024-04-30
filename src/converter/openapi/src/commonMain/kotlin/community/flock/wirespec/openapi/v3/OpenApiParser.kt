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
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.openapi.Common.className
import kotlinx.serialization.json.Json
import community.flock.kotlinx.openapi.bindings.v3.Type as OpenapiType

class OpenApiParser(private val openApi: OpenAPIObject) {

    companion object {
        fun parse(json: String, strict: Boolean = false): AST =
            OpenAPI(json = Json { prettyPrint = true; ignoreUnknownKeys = strict })
                .decodeFromString(json)
                .let { OpenApiParser(it).parse() }

        fun parse(openApi: OpenAPIObject): AST = OpenApiParser(openApi).parse()
    }


    private fun parse(): List<Node> =
        parseEndpoint() + parseParameters() + parseRequestBody() + parseResponseBody() + parseComponents()

    private fun parseEndpoint(): List<Node> = openApi.paths
        .flatMap { (key, path) ->
            path.toOperationList().map { (method, operation) ->
                val parameters = path.resolveParameters() + operation.resolveParameters()
                val segments = key.toSegments(parameters)
                val name = operation.toName() ?: (key.toName() + method.name)
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

                                        null -> TODO("Not yet implemented")
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
                    res.resolve().let {
                        it.content?.map { (contentType, media) ->
                            Endpoint.Response(
                                status = status.value,
                                headers = it.headers?.map { entry ->
                                    entry.value.resolve().toField(entry.key, className(name, "ResponseHeader"))
                                }.orEmpty(),
                                content = Endpoint.Content(
                                    type = contentType.value,
                                    reference = when (val schema = media.schema) {
                                        is ReferenceObject -> schema.toReference()
                                        is SchemaObject -> schema.toReference(
                                            className(name, status.value, contentType.value, "ResponseBody")
                                        )

                                        null -> Reference.Any(false)
                                    },
                                    isNullable = media.schema?.resolve()?.nullable ?: false
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
        val name = req.operation.toName() ?: (req.path.toName() + req.method.name)
        parameters.flatMap { parameter ->
            parameter.schema?.flatten(className(name, "Parameter", parameter.name)) ?: emptyList()
        }
    }

    private fun parseRequestBody() = openApi.flatMapRequests { req ->
        val name = req.operation.toName() ?: (req.path.toName() + req.method.name)
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
        val name = res.operation.toName() ?: (res.path.toName() + res.method.name)
        when (val response = res.response) {
            is ResponseObject -> {
                response.content.orEmpty().flatMap { (mediaType, mediaObject) ->
                    when (val schema = mediaObject.schema) {
                        is SchemaObject -> when (schema.type) {
                            null, OpenapiType.OBJECT -> schema
                                .flatten(className(name, res.statusCode.value, mediaType.value, "ResponseBody"))

                            OpenapiType.ARRAY -> schema.items
                                ?.flatten(className(name, res.statusCode.value, mediaType.value, "ResponseBody"))
                                .orEmpty()

                            else -> emptyList()
                        }

                        else -> emptyList()
                    }
                }
            }

            is ReferenceObject -> emptyList()
        }
    }


    private fun parseComponents(): List<Node> = openApi.components?.schemas.orEmpty()
        .filter {
            when (val s = it.value) {
                is SchemaObject -> s.additionalProperties == null
                else -> false
            }
        }
        .flatMap { it.value.flatten(className(it.key)) }

    private fun String.isParam() = this[0] == '{' && this[length - 1] == '}'

    private fun OperationObject.toName() = this.operationId?.let { className(it) }
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

    private fun Path.toSegments(parameters: List<ParameterObject>) =
        value.split("/").drop(1).filter { it.isNotBlank() }.map { segment ->
            when (segment.isParam()) {
                true -> {
                    val param = segment.substring(1, segment.length - 1)
                    val name = toName()
                    parameters
                        .find { it.name == param }
                        ?.schema
                        ?.resolve()
                        ?.toReference(className(name, "Parameter", param))
                        ?.let {
                            Endpoint.Segment.Param(
                                Field.Identifier(param),
                                it
                            )
                        }
                        ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
                }

                false -> Endpoint.Segment.Literal(segment)
            }
        }

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

    private fun ReferenceObject.resolveHeaderObject(): Pair<ReferenceObject, HeaderObject> =
        openApi.components?.headers
            ?.get(getReference())
            ?.let {
                when (it) {
                    is HeaderObject -> this to it
                    is ReferenceObject -> it.resolveHeaderObject()
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

    private fun HeaderOrReferenceObject.resolve(): HeaderObject =
        when (this) {
            is HeaderObject -> this
            is ReferenceObject -> this.resolveHeaderObject().second
        }

    private fun SchemaOrReferenceOrBooleanObject.resolve(): SchemaObject =
        when (this) {
            is SchemaObject -> this
            is ReferenceObject -> this.resolveSchemaObject().second
            is BooleanObject -> TODO("Not yet implemented")
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

    private fun SchemaObject.flatten(name: String): List<Node> =
        when {
            additionalProperties != null -> when (additionalProperties) {
                is BooleanObject -> emptyList()
                else -> additionalProperties
                    ?.resolve()
                    ?.takeIf { it.properties != null }
                    ?.flatten(name)
                    ?: emptyList()
            }
            oneOf != null || anyOf != null -> listOf(
                Union(
                    name = name,
                    entries = oneOf!!
                        .mapIndexed { index, it ->
                        when (it) {
                            is ReferenceObject -> it.toReference()
                            is SchemaObject -> it.toReference(className(name, index.toString()))
                        }

                        }.toSet()
                )
            )
                .plus(oneOf!!.flatMapIndexed() { index, it ->
                    when (it) {
                        is ReferenceObject -> emptyList()
                        is SchemaObject -> it.flatten(className(name, index.toString()))
                    }
                })
            allOf != null -> listOf(
                Type(
                    name,
                    Type.Shape(allOf.orEmpty().flatMap { it.resolve().toField(name) }.distinctBy { it.identifier })
                )
            )
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
                        value.flatten(className(name, key))
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

    private fun SchemaOrReferenceObject.flatten(name: String): List<Node> {
        return when (this) {
            is SchemaObject -> this.flatten(name)
            is ReferenceObject -> emptyList()
        }
    }

    private fun ReferenceObject.toReference(): Reference =
        resolveSchemaObject().let { (referencingObject, schema) ->
            when {
                schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
                    is BooleanObject -> Reference.Any(isIterable = false, isMap = true)
                    is ReferenceObject -> additionalProperties.toReference().toMap()
                    is SchemaObject -> additionalProperties.toReference(getReference()).toMap()
                }

                schema.enum != null -> Reference.Custom(
                    className(referencingObject.getReference()),
                    isIterable = false,
                    isMap = false
                )

                schema.type.isPrimitive() -> Reference.Primitive(
                    schema.type!!.toPrimitive(),
                    isIterable = false,
                    isMap = false
                )

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
            is BooleanObject -> Reference.Any(isIterable = false, isMap = true)
            is ReferenceObject -> additionalProperties.toReference().toMap()
            is SchemaObject -> additionalProperties
                .takeIf { it.type.isPrimitive() || it.properties != null }
                ?.run { toReference(name).toMap() }
                ?: Reference.Any(isIterable = false, isMap = true)
        }

        enum != null -> Reference.Custom(name, false, additionalProperties != null)
        else -> when (val type = type) {
            OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Reference.Primitive(
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
        OpenapiType.STRING -> Reference.Primitive.Type.String
        OpenapiType.INTEGER -> Reference.Primitive.Type.Integer
        OpenapiType.NUMBER -> Reference.Primitive.Type.Number
        OpenapiType.BOOLEAN -> Reference.Primitive.Type.Boolean
        else -> error("Type is not a primitive")
    }

    private fun SchemaObject.toField(name: String) = properties.orEmpty().map { (key, value) ->
        when (value) {
            is SchemaObject -> {
                Field(
                    identifier = Field.Identifier(key),
                    reference = when {
                        value.enum != null -> value.toReference(className(name, key))
                        value.type == OpenapiType.ARRAY -> value.toReference(className(name, key, "Array"))
                        else -> value.toReference(className(name, key))
                    },
                    isNullable = !(this.required?.contains(key) ?: false)
                )
            }

            is ReferenceObject -> {
                Field(
                    Field.Identifier(key),
                    Reference.Custom(className(value.getReference()), false),
                    !(this.required?.contains(key) ?: false)
                )
            }
        }
    }

    private fun ParameterObject.toField(name: String) =
        when (val s = schema) {
            is ReferenceObject -> s.toReference()
            is SchemaObject -> s.toReference(name)
            null -> TODO("Not yet implemented")
        }
            .let { Field(Field.Identifier(this.name), it, !(this.required ?: false)) }

    private fun HeaderObject.toField(identifier: String, name: String) =
        when (val s = schema) {
            is ReferenceObject -> s.toReference()
            is SchemaObject -> s.toReference(name)
            null -> TODO("Not yet implemented")
        }
            .let { Field(Field.Identifier(identifier), it, !(this.required ?: false)) }

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
    is Reference.Custom -> copy(isIterable = true)
    is Reference.Any -> copy(isIterable = true)
    is Reference.Primitive -> copy(isIterable = true)
    is Reference.Unit -> copy(isIterable = true)
}

private fun Reference.toMap() = when (this) {
    is Reference.Custom -> copy(isMap = true)
    is Reference.Any -> copy(isMap = true)
    is Reference.Primitive -> copy(isMap = true)
    is Reference.Unit -> copy(isMap = true)
}
