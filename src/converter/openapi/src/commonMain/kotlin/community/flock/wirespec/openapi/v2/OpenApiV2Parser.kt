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
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Field.Reference
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.openapi.Common.className
import kotlinx.serialization.json.Json
import community.flock.kotlinx.openapi.bindings.v2.Type as OpenapiType

class OpenApiV2Parser(private val openApi: SwaggerObject) {

    companion object {
        fun parse(json: String, ignoreUnknown: Boolean = false): AST =
            OpenAPI(json = Json { prettyPrint = true; ignoreUnknownKeys = ignoreUnknown })
                .decodeFromString(json)
                .let { OpenApiV2Parser(it).parse() }

        fun parse(openApi: SwaggerObject): AST = OpenApiV2Parser(openApi).parse()
    }

    fun parse(): List<Node> =
        parseEndpoints() + parseParameters() + parseRequestBody() + parseResponseBody() + parseDefinitions()

    private fun parseEndpoints(): List<Node> = openApi.paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList().flatMap { (method, operation) ->
                val parameters = pathItem.resolveParameters() + operation.resolveParameters()
                val segments = path.toSegments(parameters)
                val name = operation.toName() ?: (path.toName() + method.name)
                val query = parameters
                    .filter { it.`in` == ParameterLocation.QUERY }
                    .map { it.toField(name) }
                val headers = parameters
                    .filter { it.`in` == ParameterLocation.HEADER }
                    .map { it.toField(name) }
                val requests = parameters
                    .filter { it.`in` == ParameterLocation.BODY }
                    .flatMap { requestBody ->
                        (openApi.consumes.orEmpty() + operation.consumes.orEmpty())
                            .distinct()
                            .ifEmpty { listOf("application/json") }
                            .map { type ->
                                Endpoint.Request(
                                    Endpoint.Content(
                                        type = type,
                                        reference = when (val schema = requestBody.schema) {
                                            is ReferenceObject -> schema.toReference()
                                            is SchemaObject -> schema.toReference(
                                                className(name, "RequestBody")
                                            )

                                            null -> TODO("Not yet implemented")
                                        },
                                        isNullable = !(requestBody.required ?: false)
                                    )
                                )
                            }
                    }
                    .ifEmpty { listOf(Endpoint.Request(null)) }
                val responses = operation.responses.orEmpty()
                    .flatMap { (status, res) ->
                        (openApi.produces.orEmpty() + operation.produces.orEmpty())
                            .distinct()
                            .ifEmpty { listOf("application/json") }.map { type ->
                                Endpoint.Response(
                                    status = status.value,
                                    headers = emptyList(),
                                    content = res.resolve().schema?.let { schema ->
                                        Endpoint.Content(
                                            type = type,
                                            reference = when (schema) {
                                                is ReferenceObject -> schema.toReference()
                                                is SchemaObject -> schema.toReference(
                                                    className(name, status.value, type, "ResponseBody")
                                                )
                                            },
                                            isNullable = false
                                        )
                                    }
                                )
                            }
                    }
                    .distinctBy { it.status to it.content }

                listOf(
                    Endpoint(
                        comment = null,
                        identifier = Identifier(name),
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

    private fun parseParameters() = openApi.flatMapRequests { req ->
        val parameters = req.pathItem.resolveParameters() + req.operation.resolveParameters()
        val name = req.operation.toName() ?: (req.path.toName() + req.method.name)
        parameters
            .filter { it.`in` != ParameterLocation.BODY }
            .flatMap { parameter ->
                parameter.schema?.flatten(className(name, "Parameter", parameter.name)) ?: emptyList()
            }
    }

    private fun parseRequestBody() = openApi.flatMapRequests { req ->
        val parameters = req.pathItem.resolveParameters() + (req.operation.resolveParameters())
        val name = req.operation.toName() ?: (req.path.toName() + req.method.name)
        val enums: List<Definition> = parameters.flatMap { parameter ->
            when {
                parameter.enum != null -> listOf(
                    Enum(
                        comment = null,
                        identifier = Identifier(className(name, "Parameter", parameter.name)),
                        entries = parameter.enum!!.map { it.content }.toSet()
                    )
                )

                else -> emptyList()
            }
        }
        val types: List<Node> = req.operation.parameters
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
        val name = res.operation.toName() ?: (res.path.toName() + res.method.name)
        when (val schema = response.schema) {
            is SchemaObject -> when (schema.type) {
                null, OpenapiType.OBJECT -> schema
                    .flatten(className(name, res.statusCode.value, res.type, "ResponseBody"))

                OpenapiType.ARRAY -> schema.items
                    ?.flatten(className(name, res.statusCode.value, res.type, "ResponseBody"))
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
            is BooleanObject -> TODO("Not yet implemented")
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

    private fun SchemaObject.flatten(name: String): List<Node> = when {
        additionalProperties != null -> when (additionalProperties) {
            is BooleanObject -> emptyList()
            else -> additionalProperties
                ?.resolve()
                ?.takeIf { it.properties != null }
                ?.flatten(name)
                ?: emptyList()
        }

        allOf != null -> listOf(
            Type(
                comment = null,
                identifier = Identifier(name),
                shape = Type.Shape(allOf
                    .orEmpty()
                    .flatMap {
                        when (it) {
                            is SchemaObject -> it.toField(name)
                            is ReferenceObject -> it.resolveSchemaObject().resolve().toField(it.getReference())
                        }
                    }
                    .distinctBy { it.identifier })
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
            .let { listOf(Enum(comment = null, identifier = Identifier(name), entries = it)) }

        else -> when (type) {
            null, OpenapiType.OBJECT -> {
                val fields = properties.orEmpty()
                    .flatMap { (key, value) -> value.flatten(className(name, key)) }

                val schema = listOf(
                    Type(comment = null, identifier = Identifier(name), shape = Type.Shape(toField(name)))
                )
                schema + fields
            }

            OpenapiType.ARRAY -> when (val it = this.items) {
                is ReferenceObject -> emptyList()
                is SchemaObject -> it.flatten(className(name, "Array"))
                null -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun SchemaOrReferenceObject.flatten(name: String): List<Node> {
        return when (this) {
            is SchemaObject -> this.flatten(name)
            is ReferenceObject -> emptyList()
        }
    }

    private fun ReferenceObject.toReference(): Reference = resolveSchemaObject().let { refOrSchema ->
        val schema = refOrSchema.resolve()
        when {
            schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
                is BooleanObject -> Reference.Any(isIterable = false, isDictionary = true)
                is ReferenceObject -> additionalProperties.toReference().toMap()
                is SchemaObject -> additionalProperties.toReference(getReference()).toMap()
            }

            schema.enum != null -> Reference.Custom(className(getReference()), isIterable = false, isDictionary = false)
            schema.type.isPrimitive() -> Reference.Primitive(
                schema.type!!.toPrimitive(),
                isIterable = false,
                isDictionary = false
            )

            else -> when (schema.type) {
                OpenapiType.ARRAY -> when (val items = schema.items) {
                    is ReferenceObject -> Reference.Custom(className(items.getReference()), true)
                    is SchemaObject -> items.toReference(className(getReference(), "Array")).toIterable()
                    null -> error("items cannot be null when type is array: ${this.ref}")
                }

                else -> when (refOrSchema) {
                    is SchemaObject -> Reference.Custom(className(getReference()), false)
                    is ReferenceObject -> Reference.Custom(className(refOrSchema.getReference()), false)
                }

            }
        }
    }


    private fun SchemaObject.toReference(name: String): Reference = when {
        additionalProperties != null -> when (val additionalProperties = additionalProperties!!) {
            is BooleanObject -> Reference.Any(isIterable = false, isDictionary = true)
            is ReferenceObject -> additionalProperties.toReference().toMap()
            is SchemaObject -> additionalProperties
                .takeIf { it.type.isPrimitive() || it.properties != null }
                ?.run { toReference(name).toMap() }
                ?: Reference.Any(isIterable = false, isDictionary = true)
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
                    identifier = Identifier(key),
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
                    identifier = Identifier(key),
                    reference = value.toReference(),
                    isNullable = !(this.required?.contains(key) ?: false)
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
                        .let { primitive -> Reference.Primitive(primitive, isIterable = false) }

                    OpenapiType.ARRAY -> it.items
                        ?.resolve()
                        ?.type
                        ?.toPrimitive()
                        ?.let { primitive -> Reference.Primitive(primitive, isIterable = true) }
                        ?: TODO("Not yet implemented")

                    OpenapiType.OBJECT -> TODO("Not yet implemented")
                    OpenapiType.FILE -> TODO("Not yet implemented")
                    null -> TODO("Not yet implemented")
                }

            }
        }.let { Field(Identifier(this.name), it, !(this.required ?: false)) }

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
                            Identifier(param),
                            Reference.Primitive(it, false)
                        )
                    }
                    ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
            }

            else -> Endpoint.Segment.Literal(segment)
        }
    }

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
                    (openApi.consumes ?: operation.consumes ?: listOf("application/json")).map { type ->
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
                            (produces ?: operation.produces ?: listOf("application/json")).map { type ->
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
    is Reference.Unit -> this.copy(isIterable = true)
}

private fun Reference.toMap() = when (this) {
    is Reference.Custom -> this.copy(isDictionary = true)
    is Reference.Any -> this.copy(isDictionary = true)
    is Reference.Primitive -> this.copy(isDictionary = true)
    is Reference.Unit -> this.copy(isDictionary = true)
}
