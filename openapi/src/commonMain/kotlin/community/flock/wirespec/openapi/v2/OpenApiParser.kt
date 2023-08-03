package community.flock.wirespec.openapi.v2

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

    fun parse(): List<Definition> {
        val endpointAst = openApi.flatMapRequests { req ->
            val parameters = req.pathItem.resolveParameters() + req.operation.resolveParameters()
            val segments = req.path.toSegments(parameters)
            val name = req.operation.toName(segments, req.method)
            val query = parameters
                .filter { it.`in` == ParameterLocation.QUERY }
                .map { it.toField() }
            val headers = parameters
                .filter { it.`in` == ParameterLocation.HEADER }
                .map { it.toField() }
            val requests = parameters
                .filter { it.`in` == ParameterLocation.BODY }
                .flatMap { requestBody ->
                    openApi.consumes.orEmpty().map { type ->
                        Endpoint.Request(
                            Endpoint.Content(
                                type = type,
                                reference = when (val schema = requestBody.schema) {
                                    is ReferenceObject -> schema.toReference()
                                    is SchemaObject -> Reference.Custom(
                                        Common.className(
                                            name,
                                            "RequestBody"
                                        ), true
                                    )

                                    null -> TODO()
                                },
                                isNullable = requestBody.required ?: false
                            )
                        )
                    }
                }
                .ifEmpty { listOf(Endpoint.Request(null)) }
            val responses = req.operation.responses.orEmpty().flatMap { (status, res) ->
                openApi.produces.orEmpty().map { type ->
                    Endpoint.Response(
                        status = status.value,
                        content = res.resolve().schema?.let { schema ->
                            Endpoint.Content(
                                type = type,
                                reference = when (schema) {
                                    is ReferenceObject -> schema.toReference()
                                    is SchemaObject -> when (schema.type) {
                                        null, OpenapiType.OBJECT -> Reference.Custom(
                                            Common.className(
                                                name,
                                                status.value,
                                                "ResponseBody",
                                            ), true
                                        )

                                        else -> schema.toReference()
                                    }
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
                    method = req.method,
                    path = segments,
                    query = query,
                    headers = headers,
                    cookies = emptyList(),
                    requests = requests,
                    responses = responses,
                )
            )

        }

        val requestBodyAst = openApi.flatMapRequests { req ->
            req.operation.parameters
                ?.map { it.resolve() }
                ?.filter { it.`in` == ParameterLocation.BODY }
                ?.flatMap {
                    val parameters =
                        req.pathItem.resolveParameters() + (req.operation.resolveParameters())
                    val segments = req.path.toSegments(parameters)
                    val name = req.operation.toName(segments, req.method)
                    when (val schema = it.schema) {
                        is SchemaObject -> when (schema.type) {
                            null, OpenapiType.OBJECT -> schema
                                .flatten(Common.className(name, "RequestBody"))
                                .map { s -> Type(s.name, Type.Shape(s.properties.map { it.field })) }

                            else -> emptyList()
                        }

                        is ReferenceObject -> emptyList()
                        null -> emptyList()
                    }
                }
                ?: emptyList()
        }

        val responseBodyAst: List<Type> = openApi
            .flatMapResponses { req ->
                val response = req.response.resolve()
                val parameters = req.pathItem.resolveParameters() + (req.operation.resolveParameters())
                val segments = req.path.toSegments(parameters)
                val name = req.operation.toName(segments, req.method)
                when (val schema = response.schema) {
                    is SchemaObject -> when (schema.type) {
                        null, OpenapiType.OBJECT -> schema
                            .flatten(Common.className(name, req.statusCode.value, "ResponseBody"))
                            .map { Type(it.name, Type.Shape(it.properties.map { it.field })) }

                        else -> emptyList()
                    }

                    is ReferenceObject -> emptyList()
                    null -> emptyList()
                }
            }

        val definitionsAst = openApi.definitions
            ?.flatMap { it.value.flatten(Common.className(it.key)) }
            ?.map { Type(it.name, Type.Shape(it.properties.map { it.field })) }
            ?: emptyList()

        return endpointAst + requestBodyAst + responseBodyAst + definitionsAst
    }

    private fun OperationObject.resolveParameters(): List<ParameterObject> = parameters.orEmpty()
        .mapNotNull {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> it.resolveParameterObject()
            }
        }

    private fun PathItemObject.resolveParameters(): List<ParameterObject> = parameters.orEmpty()
        .mapNotNull {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> it.resolveParameterObject()
            }
        }


    private fun ReferenceObject.resolveParameterObject() =
        openApi.parameters
            ?.get(getReference())

    private fun ReferenceObject.resolveResponseObject() =
        openApi.responses
            ?.get(getReference())

    private fun ReferenceObject.resolveSchemaObject(): Pair<ReferenceObject, SchemaObject>? =
        openApi.definitions
            ?.get(getReference())
            ?.let { this to it }

    private fun SchemaOrReferenceObject.resolve(): SchemaObject =
        when (this) {
            is SchemaObject -> this
            is ReferenceObject -> this.resolveSchemaObject()?.second ?: error("Cannot resolve reference: $ref")
        }

    private fun ResponseOrReferenceObject.resolve(): ResponseObject =
        when (this) {
            is ResponseObject -> this
            is ReferenceObject -> this.resolveResponseObject() ?: error("Cannot resolve reference: $ref")
        }

    private fun ParameterOrReferenceObject.resolve(): ParameterObject =
        when (this) {
            is ParameterObject -> this
            is ReferenceObject -> this.resolveParameterObject() ?: error("Cannot resolve reference: $ref")
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

            listOf(
                SimpleSchema(
                    name = name,
                    properties = properties
                        ?.map { (key, value) ->
                            when (value) {
                                is SchemaObject -> {
                                    val reference = when (value.type) {
                                        OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Reference.Primitive(
                                            (value.type as OpenapiType).toPrimitive(),
                                            false
                                        )

                                        OpenapiType.ARRAY -> {
                                            val resolve = value.items?.resolve()
                                            when (val type = resolve?.type) {
                                                OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Reference.Primitive(
                                                    type.toPrimitive(),
                                                    true
                                                )

                                                else -> when (val items = value.items) {
                                                    is ReferenceObject -> Reference.Custom(
                                                        items.getReference(),
                                                        true
                                                    )

                                                    else -> Reference.Custom(
                                                        name,
                                                        true
                                                    )
                                                }
                                            }

                                        }

                                        OpenapiType.OBJECT -> Reference.Custom(Common.className(name, key), false)
                                        OpenapiType.FILE -> TODO()
                                        null -> TODO()
                                    }
                                    SimpleProp(
                                        key = key,
                                        field = Field(
                                            Field.Identifier(key),
                                            reference,
                                            !(this.required?.contains(key) ?: false)
                                        )
                                    )
                                }

                                is ReferenceObject -> {
                                    SimpleProp(
                                        key = key,
                                        field = Field(
                                            Field.Identifier(key),
                                            Reference.Custom(value.getReference(), false),
                                            !(this.required?.contains(key) ?: false)
                                        )
                                    )
                                }
                            }
                        } ?: emptyList()
                )
            )
                .plus(fields)


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
                ?.second
                ?.flatten(name)
                ?: error("Reference not found")
        }
    }

    private data class SimpleProp(val key: String, val field: Field)
    private data class SimpleSchema(val name: String, val properties: List<SimpleProp>)

    private fun SchemaObject.toReference(): Reference =
        when (val type = this.type) {
            OpenapiType.STRING, OpenapiType.INTEGER, OpenapiType.NUMBER, OpenapiType.BOOLEAN -> Reference.Primitive(
                type.toPrimitive(),
                false
            )

            OpenapiType.ARRAY -> when (items) {
                is ReferenceObject -> Reference.Custom(
                    Common.className((items as ReferenceObject).getReference()),
                    true
                )

                else -> TODO()
            }

            else -> TODO()
        }

    private fun ReferenceObject.toReference(): Reference {
        val resolved = resolveSchemaObject() ?: error("Cannot resolve ref: ${this.ref}")
        return when (resolved.second.type) {
            OpenapiType.ARRAY -> when (val items = resolved.second.items) {
                is ReferenceObject -> Reference.Custom(Common.className(items.getReference()), true)
                is SchemaObject -> Reference.Custom(Common.className(resolved.first.getReference(), "Array"), true)
                else -> TODO()
            }

            else -> Reference.Custom(Common.className(resolved.first.getReference()), false)
        }
    }

    private fun PathItemObject.toOperationList() = Endpoint.Method.values()
        .map {
            it to when (it) {
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
        .filter { (_, value) -> value != null }

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

    data class FlattenRequest(
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
                    operation
                        ?.let { consumes?.map { type -> FlattenRequest(path, pathItem, method, operation, type) } }
                        ?: emptyList()
                }
        }
        .flatMap { f(it) }

    data class FlattenResponse(
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
                        ?.responses?.flatMap { (statusCode, response) ->
                            produces
                                ?.map { type ->
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
                                ?: emptyList()
                        }
                        ?: emptyList()
                }
        }
        .flatMap { f(it) }
}