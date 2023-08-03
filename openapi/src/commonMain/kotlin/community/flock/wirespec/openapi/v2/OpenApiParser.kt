package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.v2.BooleanObject
import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.kotlinx.openapi.bindings.v2.OperationObject
import community.flock.kotlinx.openapi.bindings.v2.ParameterLocation
import community.flock.kotlinx.openapi.bindings.v2.ParameterObject
import community.flock.kotlinx.openapi.bindings.v2.ParameterOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.Path
import community.flock.kotlinx.openapi.bindings.v2.PathItemObject
import community.flock.kotlinx.openapi.bindings.v2.ReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.ResponseObject
import community.flock.kotlinx.openapi.bindings.v2.ResponseOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.SchemaObject
import community.flock.kotlinx.openapi.bindings.v2.SchemaOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.SchemaOrReferenceOrBooleanObject
import community.flock.kotlinx.openapi.bindings.v2.StatusCode
import community.flock.kotlinx.openapi.bindings.v2.SwaggerObject
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.openapi.Common
import community.flock.kotlinx.openapi.bindings.v2.Type as OpenapiType

class OpenApiParser(private val openApi: SwaggerObject) {

    companion object {
        fun parse(json: String): List<Definition> = OpenAPI
            .decodeFromString(json)
            .let { OpenApiParser(it).parse() }

        fun parse(openApi: SwaggerObject) = OpenApiParser(openApi).parse()
    }

    fun parse(): List<Definition> = parseEndpoints() + parseRequestBody() + parseResponseBody() + parseDefinitions()

    private fun parseEndpoints(): List<Definition> =
        openApi.paths.flatMap { (path, pathItem) ->
            pathItem.toOperationList().flatMap { (method, operation) ->
                val parameters = pathItem.resolveParameters() + operation.resolveParameters()
                val segments = path.toSegments(parameters)
                val name = operation.toName(segments, method)
                val query = parameters
                    .filter { it.`in` == ParameterLocation.QUERY }
                    .map { it.toField() }
                val headers = parameters
                    .filter { it.`in` == ParameterLocation.HEADER }
                    .map { it.toField() }
                val requests = parameters
                    .filter { it.`in` == ParameterLocation.BODY }
                    .flatMap { requestBody ->
                        (openApi.consumes ?: operation.consumes).orEmpty().map { type ->
                            Endpoint.Request(
                                Endpoint.Content(
                                    type = type,
                                    reference = when (val schema = requestBody.schema) {
                                        is ReferenceObject -> schema.toReference()
                                        is SchemaObject -> schema.toReference(
                                            Common.className(
                                                name,
                                                "RequestBody",
                                            )
                                        )

                                        null -> TODO()
                                    },
                                    isNullable = requestBody.required ?: false
                                )
                            )
                        }
                    }
                    .ifEmpty { listOf(Endpoint.Request(null)) }
                val responses = operation.responses.orEmpty().flatMap { (status, res) ->
                    (openApi.produces ?: operation.produces).orEmpty().map { type ->
                        Endpoint.Response(
                            status = status.value,
                            content = res.resolve().schema?.let { schema ->
                                Endpoint.Content(
                                    type = type,
                                    reference = when (schema) {
                                        is ReferenceObject -> schema.toReference()
                                        is SchemaObject -> schema.toReference(
                                            Common.className(
                                                name,
                                                status.value,
                                                "ResponseBody",
                                            )
                                        )
                                    },
                                    isNullable = false
                                )
                            }
                        )
                    }
                }

                listOf(
                    Endpoint(
                        name = name,
                        method = method,
                        path = segments,
                        query = query,
                        headers = headers,
                        cookies = emptyList(),
                        requests = requests,
                        responses = responses,
                    )
                )

            }
        }

    private fun parseRequestBody() = openApi.flatMapRequests { req ->
        req.operation.parameters
            ?.map { it.resolve() }
            ?.filter { it.`in` == ParameterLocation.BODY }
            ?.flatMap { param ->
                val parameters =
                    req.pathItem.resolveParameters() + (req.operation.resolveParameters())
                val segments = req.path.toSegments(parameters)
                val name = req.operation.toName(segments, req.method)
                when (val schema = param.schema) {
                    is SchemaObject -> when (schema.type) {
                        null, community.flock.kotlinx.openapi.bindings.v2.Type.OBJECT -> schema
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
            ?: emptyList()
    }

    private fun parseResponseBody() = openApi.flatMapResponses { res ->
        val response = res.response.resolve()
        val parameters = res.pathItem.resolveParameters() + (res.operation.resolveParameters())
        val segments = res.path.toSegments(parameters)
        val name = res.operation.toName(segments, res.method)
        when (val schema = response.schema) {
            is SchemaObject -> when (schema.type) {
                null, OpenapiType.OBJECT -> (
                        schema.additionalProperties?.resolve()
                            ?.flatten(Common.className(name, res.statusCode.value, "ResponseBody"))
                            ?: schema.flatten(Common.className(name, res.statusCode.value, "ResponseBody")))
                    .map { Type(it.name, Type.Shape(it.properties)) }

                else -> emptyList()
            }

            is ReferenceObject -> emptyList()
            null -> emptyList()
        }
    }

    private fun parseDefinitions() = openApi.definitions
        ?.flatMap { it.value.flatten(Common.className(it.key)) }
        ?.map { Type(it.name, Type.Shape(it.properties)) }
        ?: emptyList()

    private fun OperationObject.resolveParameters(): List<ParameterObject> = parameters.orEmpty()
        .map {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> it.resolveParameterObject()
            }
        }

    private fun PathItemObject.resolveParameters(): List<ParameterObject> = parameters.orEmpty()
        .map {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> it.resolveParameterObject()
            }
        }

    private fun ReferenceObject.resolveParameterObject() =
        openApi.parameters
            ?.get(getReference())
            ?: error("Cannot resolve ref: $ref")

    private fun ReferenceObject.resolveResponseObject() =
        openApi.responses
            ?.get(getReference())
            ?: error("Cannot resolve ref: $ref")

    private fun ReferenceObject.resolveSchemaObject() =
        openApi.definitions
            ?.get(getReference())
            ?.let { this to it }
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

    private fun ResponseOrReferenceObject.resolve(): ResponseObject =
        when (this) {
            is ResponseObject -> this
            is ReferenceObject -> this.resolveResponseObject()
        }

    private fun ParameterOrReferenceObject.resolve(): ParameterObject =
        when (this) {
            is ParameterObject -> this
            is ReferenceObject -> this.resolveParameterObject()
        }

    private fun SchemaObject.flatten(
        name: String,
    ): List<SimpleSchema> = when (type) {
        null, OpenapiType.OBJECT -> {

            val fields = properties
                ?.flatMap { (key, value) ->
                    when (value) {
                        is SchemaObject -> when (value.type) {
                            OpenapiType.ARRAY -> emptyList()
                            else -> value.flatten(Common.className(name, key))
                        }

                        is ReferenceObject -> emptyList()

                    }
                }
                ?: emptyList()

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

                                is ReferenceObject -> {
                                    Field(
                                        Field.Identifier(key),
                                        Reference.Custom(value.getReference(), false),
                                        !(this.required?.contains(key) ?: false)
                                    )
                                }
                            }
                        } ?: emptyList()
                    )
                )

                else -> emptyList()
            }
            schema + fields
        }

        OpenapiType.ARRAY -> items
            ?.let {
                when (it) {
                    is ReferenceObject -> emptyList()
                    is SchemaObject -> it.items?.flatten(Common.className(name, "array"))
                }
            }
            ?: emptyList()

        else -> emptyList()
    }

    private fun SchemaOrReferenceObject.flatten(
        name: String,
    ): List<SimpleSchema> {
        return when (this) {
            is SchemaObject -> this.flatten(name)

            is ReferenceObject -> this
                .resolveSchemaObject()
                .second
                .flatten(name)
        }
    }

    private data class SimpleSchema(val name: String, val properties: List<Field>)

    private fun ReferenceObject.toReference(): Reference.Custom =
        resolveSchemaObject().let { (referencingObject, schema) ->
            when (val additionalProperties = schema.additionalProperties) {
                is BooleanObject -> TODO("additionalProperties = true not implemented")
                is ReferenceObject -> Reference.Custom(
                    Common.className(additionalProperties.getReference()),
                    false,
                    true
                )

                is SchemaObject -> Reference.Custom(Common.className(referencingObject.getReference()), false, true)
                null -> when (schema.type) {
                    OpenapiType.ARRAY -> when (val items = schema.items) {
                        is ReferenceObject -> Reference.Custom(Common.className(items.getReference()), true)
                        is SchemaObject -> Reference.Custom(
                            Common.className(referencingObject.getReference(), "Array"),
                            true
                        )

                        null -> error("items cannot be null when type is array: ${this.ref}")
                    }

                    else -> Reference.Custom(Common.className(referencingObject.getReference()), false)

                }
            }
        }


    private fun SchemaObject.toReference(name: String): Reference = when (val type = this.type) {
        OpenapiType.STRING, OpenapiType.INTEGER, OpenapiType.NUMBER, OpenapiType.BOOLEAN -> Reference.Primitive(
            type.toPrimitive(),
            false
        )

        null, OpenapiType.OBJECT -> Reference.Custom(name, false, additionalProperties != null)

        OpenapiType.ARRAY -> {
            val resolve = items?.resolve()
            when (val t = resolve?.type) {
                OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Reference.Primitive(
                    t.toPrimitive(),
                    true
                )

                else -> when (val it = items) {
                    is ReferenceObject -> it.toReference().copy(isIterable = true)
                    is SchemaObject -> it.toReference(name)
                    null -> error("When schema is of type array items cannot be null for name: $name")
                }
            }
        }

        OpenapiType.FILE -> TODO("Type file not implemented")
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

    private fun ReferenceObject.getReference() = this.ref.value.split("/")[2]

    private fun OpenapiType.toPrimitive() = when (this) {
        OpenapiType.STRING -> Reference.Primitive.Type.String
        OpenapiType.INTEGER -> Reference.Primitive.Type.Integer
        OpenapiType.NUMBER -> Reference.Primitive.Type.Integer
        OpenapiType.BOOLEAN -> Reference.Primitive.Type.Boolean
        else -> error("Type is not a primitive")
    }

    private fun ParameterObject.toField() = this
        .resolve()
        .let {
            when (val type = it.type) {
                OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> type
                    .toPrimitive()
                    .let { Reference.Primitive(it, false) }

                OpenapiType.ARRAY -> it.items
                    ?.resolve()
                    ?.type
                    ?.toPrimitive()
                    ?.let { Reference.Primitive(it, true) }
                    ?: TODO()

                OpenapiType.OBJECT -> TODO()
                OpenapiType.FILE -> TODO()
                null -> TODO()
            }
        }
        .let { Field(Field.Identifier(name), it, !(this.required ?: false)) }

    private fun Path.toSegments(parameters: List<ParameterObject>) = value.split("/").drop(1).map { segment ->
        val isParam = segment.isNotEmpty() && segment[0] == '{' && segment[segment.length - 1] == '}'
        when {
            isParam -> {
                val param = segment.substring(1, segment.length - 1)
                parameters
                    .find { it.name == param }
                    ?.let { it.type?.toPrimitive() }
                    ?.let {
                        Endpoint.Segment.Param(
                            Field.Identifier(param),
                            Reference.Primitive(it, false)
                        )
                    }
                    ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
            }

            else -> Endpoint.Segment.Literal(segment)
        }
    }

    private fun OperationObject?.toName(segments: List<Endpoint.Segment>, method: Endpoint.Method): String {
        return this?.operationId?.let { Common.className(it) } ?: segments
            .joinToString("") {
                when (it) {
                    is Endpoint.Segment.Literal -> Common.className(it.value)
                    is Endpoint.Segment.Param -> Common.className(it.identifier.value)
                }
            }
            .let { it + method.name }
    }

    private data class FlattenRequest(
        val path: Path,
        val pathItem: PathItemObject,
        val method: Endpoint.Method,
        val operation: OperationObject,
        val type: String
    )

    private fun <T> SwaggerObject.flatMapRequests(f: (req: FlattenRequest) -> List<T>) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList()
                .flatMap { (method, operation) ->
                    (consumes ?: operation.consumes).orEmpty().map { type ->
                        FlattenRequest(
                            path,
                            pathItem,
                            method,
                            operation,
                            type
                        )
                    }
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
        val type: String
    )

    private fun <T> SwaggerObject.flatMapResponses(f: (res: FlattenResponse) -> List<T>) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList()
                .flatMap { (method, operation) ->
                    operation
                        .responses.orEmpty().flatMap { (statusCode, response) ->
                            (produces ?: operation.produces).orEmpty().map { type ->
                                FlattenResponse(
                                    path,
                                    pathItem,
                                    method,
                                    operation,
                                    statusCode,
                                    response,
                                    type
                                )
                            }
                        }
                }
        }
        .flatMap { f(it) }
}

private fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
    mapNotNull { (key, value) -> value?.let { key to it } }.toMap()