package community.flock.wirespec.openapi.v2

import arrow.core.filterIsInstance
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
import community.flock.wirespec.compiler.core.parse.nodes.Definition
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference
import community.flock.wirespec.openapi.Common.className
import community.flock.kotlinx.openapi.bindings.v2.Type as OpenapiType

class OpenApiParser(private val openApi: SwaggerObject) {

    companion object {
        fun parse(json: String): List<Definition> = OpenAPI
            .decodeFromString(json)
            .let { OpenApiParser(it).parse() }

        fun parse(openApi: SwaggerObject) = OpenApiParser(openApi).parse()
    }

    fun parse(): List<Definition> =
        parseEndpoints() + parseRequestBody() + parseResponseBody() + parseDefinitions()

    private fun parseEndpoints(): List<Definition> = openApi.paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList().flatMap { (method, operation) ->
                val parameters = pathItem.resolveParameters() + operation.resolveParameters()
                val segments = path.toSegments(parameters)
                val name = operation.toName(segments, method)
                val query = parameters
                    .filter { it.`in` == ParameterLocation.QUERY }
                    .map { it.toField(name) }
                val headers = parameters
                    .filter { it.`in` == ParameterLocation.HEADER }
                    .map { it.toField(name) }
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
                                            className(name, "RequestBody")
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
                                            className(name, status.value, "ResponseBody")
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
        val parameters = req.pathItem.resolveParameters() + (req.operation.resolveParameters())
        val segments = req.path.toSegments(parameters)
        val name = req.operation.toName(segments, req.method)
        val enums: List<Definition> = parameters.flatMap { parameter ->
            when {
                parameter.enum != null -> listOf(
                    Enum(className(name, "Parameter", parameter.name), parameter.enum!!.map { it.content }.toSet())
                )

                else -> emptyList()
            }
        }
        val types: List<Definition> = req.operation.parameters
            ?.map { it.resolve() }
            ?.filter { it.`in` == ParameterLocation.BODY }
            ?.flatMap { param ->
                when (val schema = param.schema) {
                    is SchemaObject -> when (schema.type) {
                        null, OpenapiType.OBJECT -> schema
                            .flatten(className(name, "RequestBody"))

                        OpenapiType.ARRAY -> schema.items
                            ?.flatten(className(name, "RequestBody")).orEmpty()

                        else -> emptyList()
                    }

                    else -> emptyList()
                }
            }
            ?: emptyList()

        enums + types
    }

    private fun parseResponseBody() = openApi.flatMapResponses { res ->
        val response = res.response.resolve()
        val parameters = res.pathItem.resolveParameters() + (res.operation.resolveParameters())
        val segments = res.path.toSegments(parameters)
        val name = res.operation.toName(segments, res.method)
        when (val schema = response.schema) {
            is SchemaObject -> when (schema.type) {
                null, OpenapiType.OBJECT -> schema
                    .flatten(className(name, res.statusCode.value, "ResponseBody"))

                OpenapiType.ARRAY -> schema.items
                    ?.flatten(className(name, res.statusCode.value, "ResponseBody"))
                    .orEmpty()

                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun parseDefinitions() = openApi.definitions.orEmpty()
        .filterIsInstance<String, SchemaObject>()
        .filter { it.value.additionalProperties == null }
        .flatMap { it.value.flatten(className(it.key)) }

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
            ?: error("Cannot resolve ref: $ref")

    private fun SchemaOrReferenceObject.resolve(): SchemaObject =
        when (this) {
            is SchemaObject -> this
            is ReferenceObject -> this.resolveSchemaObject().resolve()
        }

    private fun SchemaOrReferenceOrBooleanObject.resolve(): SchemaObject =
        when (this) {
            is SchemaObject -> this
            is ReferenceObject -> this.resolveSchemaObject().resolve()
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
                val fields = properties.orEmpty()
                    .flatMap { (key, value) -> value.flatten(className(name, key)) }

                val schema = listOf(
                    Type(name, Type.Shape(toField(name)))
                )
                schema + fields
            }

            OpenapiType.ARRAY -> when (val it = this.items) {
                is ReferenceObject ->
                    emptyList()

                is SchemaObject ->
                    it.flatten(className(name, "Array"))

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

    private fun ReferenceObject.toReference(): Reference =
        resolveSchemaObject().let { refOrSchema ->
            val schema = refOrSchema.resolve()
            when {
                schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
                    is BooleanObject -> Reference.Any(false, true)
                    is ReferenceObject -> additionalProperties.toReference().toMap()
                    is SchemaObject -> additionalProperties.toReference(getReference()).toMap()
                }

                schema.enum != null -> Reference.Custom(className(this.getReference()), false, false)
                schema.type.isPrimitive() -> Reference.Primitive(schema.type!!.toPrimitive(), false, false)
                else -> when (schema.type) {
                    OpenapiType.ARRAY -> when (val items = schema.items) {
                        is ReferenceObject -> Reference.Custom(className(items.getReference()), true)
                        is SchemaObject -> Reference.Custom(className(this.getReference(), "Array"), true)
                        null -> error("items cannot be null when type is array: ${this.ref}")
                    }

                    else -> when (refOrSchema) {
                        is SchemaObject -> Reference.Custom(className(this.getReference()), false)
                        is ReferenceObject -> Reference.Custom(className(refOrSchema.getReference()), false)
                    }

                }
            }
        }


    private fun SchemaObject.toReference(name: String): Reference = when {
        additionalProperties != null -> when (val additionalProperties = additionalProperties!!) {
            is BooleanObject -> Reference.Any(false, true)
            is ReferenceObject -> additionalProperties.toReference().toMap()
            is SchemaObject -> additionalProperties
                .takeIf { it.type != null }
                ?.run { toReference(name).toMap() }
                ?: Reference.Any(false, true)
        }

        enum != null -> Reference.Custom(name, false, additionalProperties != null)
        else -> when (val type = type) {
            OpenapiType.STRING, OpenapiType.INTEGER, OpenapiType.NUMBER, OpenapiType.BOOLEAN ->
                Reference.Primitive(type.toPrimitive(), false, additionalProperties != null)

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

            OpenapiType.FILE -> TODO("Type file not implemented")
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

    private fun ReferenceObject.getReference() = this.ref.value.split("/")[2]

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
                    value.toReference(),
                    !(this.required?.contains(key) ?: false)
                )
            }
        }
    }

    private fun ParameterObject.toField(name: String) = this
        .resolve()
        .let {
            when {
                enum != null -> Reference.Custom(className(name, "Parameter", it.name), false)
                else -> when (val type = it.type) {
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
        }.let { Field(Field.Identifier(this.name), it, !(this.required ?: false)) }

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
        return this?.operationId?.let { className(it) } ?: segments
            .joinToString("") {
                when (it) {
                    is Endpoint.Segment.Literal -> className(it.value)
                    is Endpoint.Segment.Param -> className(it.identifier.value)
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

private fun OpenapiType?.isPrimitive() = when (this) {
    OpenapiType.STRING -> true
    OpenapiType.NUMBER -> true
    OpenapiType.INTEGER -> true
    OpenapiType.BOOLEAN -> true
    OpenapiType.ARRAY -> false
    OpenapiType.OBJECT -> false
    OpenapiType.FILE -> false
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
