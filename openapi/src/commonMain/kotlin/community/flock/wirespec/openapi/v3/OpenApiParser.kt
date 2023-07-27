package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.kotlinx.openapi.bindings.v3.OpenAPIObject
import community.flock.kotlinx.openapi.bindings.v3.OperationObject
import community.flock.kotlinx.openapi.bindings.v3.ParameterLocation
import community.flock.kotlinx.openapi.bindings.v3.ParameterObject
import community.flock.kotlinx.openapi.bindings.v3.PathItemObject
import community.flock.kotlinx.openapi.bindings.v3.ReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.RequestBodyObject
import community.flock.kotlinx.openapi.bindings.v3.RequestBodyOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.ResponseObject
import community.flock.kotlinx.openapi.bindings.v3.ResponseOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaOrReferenceObject
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.openapi.Common
import community.flock.kotlinx.openapi.bindings.v3.Type as OpenapiType

object OpenApiParser {

    fun parse(json: String): List<Definition> =
        OpenAPI
            .decodeFromString(json)
            .let { parse(it) }

    fun parse(openApi: OpenAPIObject): List<Definition> {

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
                                    ?.schema
                                    ?.resolve(openApi)
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
                        .map { it.toField() }
                    val headers = parameters
                        .filter { it.`in` == ParameterLocation.HEADER }
                        .map { it.toField() }
                    val cookies = parameters
                        .filter { it.`in` == ParameterLocation.COOKIE }
                        .map { it.toField() }
                    val requests = operation?.requestBody?.resolve(openApi)
                        ?.let { requestBody ->
                            requestBody.content
                                ?.map { (mediaType, mediaObject) ->
                                    Endpoint.Request(
                                        Endpoint.Content(
                                            type = mediaType.value,
                                            reference = mediaObject.schema?.toReference(openApi) ?: TODO(),
                                            isNullable = requestBody.required ?: false
                                        )
                                    )
                                }
                        }
                        ?: listOf(Endpoint.Request(null))
                    val responses = operation?.responses
                        ?.flatMap { (status, res) ->
                            res.resolve(openApi)?.content
                                ?.map { (contentType, media) ->
                                    Endpoint.Response(
                                        status = status.value,
                                        content = Endpoint.Content(
                                            type = contentType.value,
                                            reference = media.schema?.toReference(openApi) ?: TODO(),
                                            isNullable = media.schema?.resolve(openApi)?.nullable ?: false
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
                        ?: emptyList()

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

        val componentsAst = openApi.components?.schemas
            ?.flatMap { it.value.flatten(Common.className(it.key), openApi) }
            ?.map { Type(it.name, Type.Shape(it.properties.map { it.field })) }
            ?: emptyList()

        return endpointAst + componentsAst
    }
}

fun OperationObject.resolveParameters(openApi: OpenAPIObject): List<ParameterObject> = parameters
    ?.mapNotNull {
        when (it) {
            is ParameterObject -> it
            is ReferenceObject -> it.resolveParameterObject(openApi)
        }
    }
    ?: emptyList()

fun PathItemObject.resolveParameters(openApi: OpenAPIObject): List<ParameterObject> = parameters
    ?.mapNotNull {
        when (it) {
            is ParameterObject -> it
            is ReferenceObject -> it.resolveParameterObject(openApi)
        }
    }
    ?: emptyList()


fun ReferenceObject.resolveParameterObject(openApi: OpenAPIObject): ParameterObject? =
    openApi.components?.parameters
        ?.get(getReference())
        ?.let {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> it.resolveParameterObject(openApi)
            }
        }

fun ReferenceObject.resolveSchemaObject(openApi: OpenAPIObject): Pair<ReferenceObject, SchemaObject>? =
    openApi.components?.schemas
        ?.get(getReference())
        ?.let {
            when (it) {
                is SchemaObject -> this to it
                is ReferenceObject -> it.resolveSchemaObject(openApi)
            }
        }

fun ReferenceObject.resolveRequestBodyObject(openApi: OpenAPIObject): Pair<ReferenceObject, RequestBodyObject>? =
    openApi.components?.requestBodies
        ?.get(getReference())
        ?.let {
            when (it) {
                is RequestBodyObject -> this to it
                is ReferenceObject -> it.resolveRequestBodyObject(openApi)
            }
        }

fun ReferenceObject.resolveResponseObject(openApi: OpenAPIObject): Pair<ReferenceObject, ResponseObject>? =
    openApi.components?.responses
        ?.get(getReference())
        ?.let {
            when (it) {
                is ResponseObject -> this to it
                is ReferenceObject -> it.resolveResponseObject(openApi)
            }
        }

fun SchemaOrReferenceObject.resolve(openApi: OpenAPIObject): SchemaObject? =
    when (this) {
        is SchemaObject -> this
        is ReferenceObject -> this.resolveSchemaObject(openApi)?.second
    }

fun RequestBodyOrReferenceObject.resolve(openApi: OpenAPIObject): RequestBodyObject? =
    when (this) {
        is RequestBodyObject -> this
        is ReferenceObject -> this.resolveRequestBodyObject(openApi)?.second
    }

fun ResponseOrReferenceObject.resolve(openApi: OpenAPIObject): ResponseObject? =
    when (this) {
        is ResponseObject -> this
        is ReferenceObject -> this.resolveResponseObject(openApi)?.second
    }

private fun SchemaObject.flatten(
    name: String,
    openApi: OpenAPIObject,
): List<SimpleSchema> =
    when (type) {
        OpenapiType.OBJECT -> {
            val fields = properties
                ?.flatMap { (key, value) ->
                    when (value) {
                        is SchemaObject -> value.flatten(Common.className(name, key), openApi)
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
                                        OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Primitive(
                                            (value.type as OpenapiType).toPrimitive(),
                                            false
                                        )

                                        OpenapiType.ARRAY -> {
                                            val resolve = value.items?.resolve(openApi)
                                            when (resolve?.type) {
                                                OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Primitive(
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
                                        null -> TODO()
                                    }
                                    SimpleProp(
                                        key = key,
                                        field = Field(
                                            Field.Identifier(key),
                                            reference,
                                            false
                                        )
                                    )
                                }

                                is ReferenceObject -> SimpleProp(
                                    key = key,
                                    field = Field(
                                        Field.Identifier(key),
                                        Reference.Custom(value.getReference(), false),
                                        false
                                    )
                                )
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
                    is SchemaObject -> it.flatten(Common.className(name, "array"), openApi)
                }
            }
            ?: emptyList()


        else -> emptyList()
    }

private fun SchemaOrReferenceObject.flatten(
    name: String,
    openApi: OpenAPIObject,
): List<SimpleSchema> {
    return when (this) {
        is SchemaObject -> this
            .flatten(name, openApi)

        is ReferenceObject -> this
            .resolveSchemaObject(openApi)
            ?.second
            ?.flatten(name, openApi)
            ?: error("Reference not found")
    }
}

private fun SchemaOrReferenceObject.toReference(openApi: OpenAPIObject) =
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
        .let { Field(Field.Identifier(name), it, this.required ?: false) }


private data class SimpleSchema(val name: String, val properties: List<SimpleProp>)
private data class SimpleProp(val key: String, val field: Field)

