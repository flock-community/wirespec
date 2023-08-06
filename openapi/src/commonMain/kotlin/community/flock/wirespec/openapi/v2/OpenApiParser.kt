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
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.openapi.Common
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
                                            Common.className(name, "RequestBody")
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
                                            Common.className(name, status.value, "ResponseBody")
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
                        null, OpenapiType.OBJECT -> schema
                            .flatten(Common.className(name, "RequestBody"))

                        OpenapiType.ARRAY -> schema.items
                            ?.flatten(Common.className(name, "RequestBody")).orEmpty()

                        else -> emptyList()
                    }

                    else -> emptyList()
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
                null, OpenapiType.OBJECT -> schema
                    .flatten(Common.className(name, res.statusCode.value, "ResponseBody"))

                OpenapiType.ARRAY -> schema.items
                    ?.flatten(Common.className(name, res.statusCode.value, "ResponseBody"))
                    .orEmpty()

                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun parseDefinitions() = openApi.definitions.orEmpty()
        .filter { it.value.additionalProperties == null }
        .flatMap { it.value.flatten(Common.className(it.key)) }

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
    ): List<Definition> = when {
        additionalProperties != null -> when (additionalProperties) {
            is BooleanObject -> emptyList()
            else -> additionalProperties!!.resolve().flatten(name)
        }

        allOf != null -> listOf(Type(name, Type.Shape(allOf.orEmpty().flatMap { it.resolve().toField(name) })))
            .plus(allOf!!.flatMap {
                when (it) {
                    is ReferenceObject -> emptyList()
                    is SchemaObject -> it.properties.orEmpty().flatMap { (key, value) ->
                        when (value) {
                            is ReferenceObject -> emptyList()
                            is SchemaObject -> value.flatten(Common.className(name, key))
                        }
                    }
                }
            })

        enum != null -> enum!!
            .map {
                when (it) {
                    is JsonPrimitive -> it.content
                    is JsonArray -> TODO()
                    is JsonObject -> TODO()
                }
            }
            .toSet()
            .let { listOf(Enum(name, it)) }

        else -> when (type) {
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

                val schema = listOf(
                    Type(name, Type.Shape(toField(name)))
                )
                schema + fields
            }

            OpenapiType.ARRAY -> when (val it = this.items) {
                is ReferenceObject ->
                    emptyList()

                is SchemaObject ->
                    it.flatten(Common.className(name, "Array"))

                null ->
                    emptyList()
            }

            else -> emptyList()
        }
    }

    private fun SchemaOrReferenceObject.flatten(
        name: String,
    ): List<Definition> {
        return when (this) {
            is SchemaObject -> this.flatten(name)
            is ReferenceObject -> emptyList()
        }
    }

    private fun ReferenceObject.toReference(): Reference.Custom =
        resolveSchemaObject().let { (referencingObject, schema) ->
            when {
                schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
                    is BooleanObject -> TODO("additionalProperties = true not implemented")
                    is ReferenceObject -> additionalProperties.toReference().copy(isMap = true)
                    is SchemaObject -> Reference.Custom(Common.className(referencingObject.getReference()), false, true)
                }
                schema.enum != null -> Reference.Custom(Common.className(referencingObject.getReference()), false, false)

                else -> when (schema.type) {
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


    private fun SchemaObject.toReference(name: String): Reference = when {
        enum != null -> Reference.Custom(name, false, additionalProperties != null)

        else -> when (val type = type) {
            OpenapiType.STRING, OpenapiType.INTEGER, OpenapiType.NUMBER, OpenapiType.BOOLEAN -> Primitive(
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
                val resolve = items?.resolve()
                when (val resolvedType = resolve?.type) {
                    OpenapiType.STRING -> when {
                        resolve.enum != null -> when(val it = items){
                            is SchemaObject -> it.toReference(name).let {
                                when (it) {
                                    is Reference.Custom -> it.copy(isIterable = true)
                                    is Primitive -> it.copy(isIterable = true)
                                    is Reference.Any -> it.copy(isIterable = true)
                                }
                            }
                            is ReferenceObject -> it.toReference().copy(isIterable = true)
                            null -> TODO()
                        }


                        else ->
                            Primitive(
                                resolvedType.toPrimitive(),
                                true,
                                additionalProperties != null
                            )
                    }


                    OpenapiType.INTEGER, OpenapiType.NUMBER, OpenapiType.BOOLEAN -> Primitive(
                        resolvedType.toPrimitive(),
                        true,
                        additionalProperties != null
                    )

                    else -> when (val it = items) {
                        is ReferenceObject -> it.toReference().copy(isIterable = true)
                        is SchemaObject -> it.toReference(name).let {
                            when (it) {
                                is Reference.Custom -> it.copy(isIterable = true)
                                is Primitive -> it.copy(isIterable = true)
                                is Reference.Any -> it.copy(isIterable = true)
                            }
                        }

                        null -> error("When schema is of type array items cannot be null for name: $name")
                    }
                }
            }

            OpenapiType.FILE -> TODO("Type file not implemented")
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

    private fun ReferenceObject.getReference() = this.ref.value.split("/")[2]

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
                    Field.Identifier(key),
                    value.toReference(Common.className(name, key)),
                    !(this.required?.contains(key) ?: false),
                )
            }

            is ReferenceObject -> {
                Field(
                    Field.Identifier(key),
                    value.toReference(),
                    !(this.required?.contains(key) ?: false)
                )
            }
        }
    }

    private fun ParameterObject.toField() = this
        .resolve()
        .let {
            when (val type = it.type) {
                OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> type
                    .toPrimitive()
                    .let { Primitive(it, false) }

                OpenapiType.ARRAY -> it.items
                    ?.resolve()
                    ?.type
                    ?.toPrimitive()
                    ?.let { Primitive(it, true) }
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
                            Primitive(it, false)
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