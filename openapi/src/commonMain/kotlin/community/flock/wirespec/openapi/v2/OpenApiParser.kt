package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.kotlinx.openapi.bindings.v2.OperationObject
import community.flock.kotlinx.openapi.bindings.v2.ParameterLocation
import community.flock.kotlinx.openapi.bindings.v2.ParameterObject
import community.flock.kotlinx.openapi.bindings.v2.ParameterOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.PathItemObject
import community.flock.kotlinx.openapi.bindings.v2.ReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.ResponseObject
import community.flock.kotlinx.openapi.bindings.v2.ResponseOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.SchemaObject
import community.flock.kotlinx.openapi.bindings.v2.SchemaOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v2.SwaggerObject
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.openapi.Common
import community.flock.kotlinx.openapi.bindings.v2.Type as OpenapiType


object OpenApiParser {

    fun parse(json: String): List<Definition> =
        OpenAPI
            .decodeFromString(json)
            .let { parse(it) }

    fun parse(openApi: SwaggerObject): List<Definition> {

        val endpointAst = openApi.paths
            .flatMap { (key, path) ->
                path.toOperationList().map { (method, operation) ->
                    val parameters =
                        path.resolveParameters(openApi) + (operation?.resolveParameters(openApi) ?: emptyList())
                    val segments = key.value.split("/").drop(1).map { segment ->
                        val isParam = segment[0] == '{' && segment[segment.length - 1] == '}'
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
                    val name = operation?.operationId?.let { Common.className(it) } ?: segments
                        .joinToString("") {
                            when (it) {
                                is Endpoint.Segment.Literal -> Common.className(it.value)
                                is Endpoint.Segment.Param -> Common.className(it.identifier.value)
                            }
                        }
                        .let { it + method.name }
                    val query = parameters
                        .filter { it.`in` == ParameterLocation.QUERY }
                        .mapNotNull { it.toField(openApi) }
                    val headers = parameters
                        .filter { it.`in` == ParameterLocation.HEADER }
                        .mapNotNull { it.toField(openApi) }
                    val requests = parameters.find { it.`in` == ParameterLocation.BODY }
                        ?.let { requestBody ->
                            listOf(
                                Endpoint.Request(
                                    Endpoint.Content(
                                        type = "application/json",
                                        reference = requestBody.schema?.toReference(openApi) ?: TODO(),
                                        isNullable = requestBody.required ?: false
                                    )
                                )
                            )
                        }
                        ?: listOf(Endpoint.Request(null))
                    val responses = operation?.responses
                        ?.flatMap { (status, res) ->
                            res.resolve(openApi)
                                ?.let { response ->
                                    if (response.schema != null) {
                                        listOf(
                                            Endpoint.Response(
                                                status = status.value,
                                                content = Endpoint.Content(
                                                    type = "application/json",
                                                    reference = response.schema?.toReference(openApi) ?: TODO(),
                                                    isNullable = false
                                                )
                                            )
                                        )
                                    } else null
                                }
                                ?: listOf(
                                    Endpoint.Response(
                                        status = status.value,
                                        content = null
                                    )
                                )
                        }
                        ?: emptyList()

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
                }
            }

        val definitionsAst = openApi.definitions
            ?.flatMap { it.value.flatten(Common.className(it.key), openApi) }
            ?.map { Type(it.name, Type.Shape(it.properties.map { it.field })) }
            ?: emptyList()

        return endpointAst + definitionsAst
    }
}

fun OperationObject.resolveParameters(openApi: SwaggerObject): List<ParameterObject> = parameters
    ?.mapNotNull {
        when (it) {
            is ParameterObject -> it
            is ReferenceObject -> it.resolveParameterObject(openApi)
        }
    }
    ?: emptyList()

fun PathItemObject.resolveParameters(openApi: SwaggerObject): List<ParameterObject> = parameters
    ?.mapNotNull {
        when (it) {
            is ParameterObject -> it
            is ReferenceObject -> it.resolveParameterObject(openApi)
        }
    }
    ?: emptyList()


fun ReferenceObject.resolveParameterObject(openApi: SwaggerObject): ParameterObject? =
    openApi.parameters
        ?.get(getReference())

fun ReferenceObject.resolveResponseObject(openApi: SwaggerObject): Pair<ReferenceObject, ResponseObject>? =
    openApi.responses
        ?.get(getReference())
        ?.let { this to it }

fun SchemaOrReferenceObject.resolve(openApi: SwaggerObject): SchemaObject? =
    when (this) {
        is SchemaObject -> this
        is ReferenceObject -> this.resolveSchemaObject(openApi)?.second
    }

fun ResponseOrReferenceObject.resolve(openApi: SwaggerObject): ResponseObject? =
    when (this) {
        is ResponseObject -> this
        is ReferenceObject -> this.resolveResponseObject(openApi)?.second
    }

fun ParameterOrReferenceObject.resolve(openApi: SwaggerObject): ParameterObject? =
    when (this) {
        is ParameterObject -> this
        is ReferenceObject -> this.resolveParameterObject(openApi)
    }

private fun SchemaObject.flatten(
    name: String,
    openApi: SwaggerObject,
): List<SimpleSchema> =
    when (type) {
        null, OpenapiType.OBJECT -> {
            val fields = properties
                ?.flatMap { (key, value) ->
                    when (value) {
                        is SchemaObject -> when (value.type) {
                            OpenapiType.ARRAY -> emptyList()
                            else -> value.flatten(Common.className(name, key), openApi)
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
                                            val resolve = value.items?.resolve(openApi)
                                            when (resolve?.type) {
                                                OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Reference.Primitive(
                                                    (resolve.type as OpenapiType).toPrimitive(),
                                                    true
                                                )

                                                else -> when (value.items) {
                                                    is ReferenceObject -> Reference.Custom(
                                                        (value.items as ReferenceObject).getReference(),
                                                        true
                                                    )

                                                    else -> TODO()
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
                    is SchemaObject -> it.items?.flatten(Common.className(name, "array"), openApi)
                }
            }
            ?: emptyList()

        else -> emptyList()
    }

private fun SchemaOrReferenceObject.flatten(
    name: String,
    openApi: SwaggerObject,
): List<SimpleSchema> {
    return when (this) {
        is SchemaObject -> this.flatten(name, openApi)

        is ReferenceObject -> this
            .resolveSchemaObject(openApi)
            ?.second
            ?.flatten(name, openApi)
            ?: error("Reference not found")
    }
}

private data class SimpleProp(val key: String, val field: Field)
private data class SimpleSchema(val name: String, val properties: List<SimpleProp>)

private fun SchemaOrReferenceObject.toReference(openApi: SwaggerObject) =
    when (this) {
        is ReferenceObject -> {
            val resolved = resolveSchemaObject(openApi) ?: TODO()
            when (resolved.second.type) {
                OpenapiType.ARRAY -> when (resolved.second.items) {
                    is ReferenceObject -> Reference.Custom(
                        Common.className((resolved.second.items as ReferenceObject).getReference()),
                        true
                    )

                    is SchemaObject -> Reference.Custom(Common.className(resolved.first.getReference(), "Array"), true)
                    else -> TODO()
                }

                else -> Reference.Custom(Common.className(resolved.first.getReference()), false)
            }

        }

        is SchemaObject -> {
            when (type) {
                OpenapiType.STRING, OpenapiType.INTEGER, OpenapiType.NUMBER, OpenapiType.BOOLEAN -> Reference.Primitive(
                    (type as OpenapiType).toPrimitive(),
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

private fun ReferenceObject.resolveSchemaObject(openApi: SwaggerObject): Pair<ReferenceObject, SchemaObject>? =
    openApi.definitions
        ?.get(getReference())
        ?.let { this to it }

private fun ReferenceObject.getReference() = this.ref.value.split("/")[2]

private fun OpenapiType.toPrimitive() = when (this) {
    OpenapiType.STRING -> Reference.Primitive.Type.String
    OpenapiType.INTEGER -> Reference.Primitive.Type.Integer
    OpenapiType.NUMBER -> Reference.Primitive.Type.Integer
    OpenapiType.BOOLEAN -> Reference.Primitive.Type.Boolean
    else -> error("Type is not a primitive")
}

private fun ParameterObject.toField(openApi: SwaggerObject) = this
    .resolve(openApi)
    ?.let {
        when (val type = it.type) {
            OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> type
                .toPrimitive()
                .let { Reference.Primitive(it, false) }

            OpenapiType.ARRAY -> it.items
                ?.resolve(openApi)
                ?.type
                ?.toPrimitive()
                ?.let { Reference.Primitive(it, true) }
                ?: TODO()

            OpenapiType.OBJECT -> TODO()
            OpenapiType.FILE -> TODO()
            null -> TODO()
        }
    }
    ?.let { Field(Field.Identifier(name), it, !(this.required ?: false)) }
