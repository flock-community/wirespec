package community.flock.wirespec.openapi

import community.flock.kotlinx.openapi.bindings.OpenAPIObject
import community.flock.kotlinx.openapi.bindings.OperationObject
import community.flock.kotlinx.openapi.bindings.ParameterLocation
import community.flock.kotlinx.openapi.bindings.ParameterObject
import community.flock.kotlinx.openapi.bindings.PathItemObject
import community.flock.kotlinx.openapi.bindings.ReferenceObject
import community.flock.kotlinx.openapi.bindings.RequestBodyObject
import community.flock.kotlinx.openapi.bindings.RequestBodyOrReferenceObject
import community.flock.kotlinx.openapi.bindings.ResponseObject
import community.flock.kotlinx.openapi.bindings.ResponseOrReferenceObject
import community.flock.kotlinx.openapi.bindings.SchemaObject
import community.flock.kotlinx.openapi.bindings.SchemaOrReferenceObject
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.kotlinx.openapi.bindings.Type as OpenapiType

object OpenApiParser {

    fun parse(openApi: OpenAPIObject): List<Definition> {

        val endpointAst = openApi.paths
            .flatMap { (key, path) ->
                path.toOperationList().map { (method, operation) ->
                    val parameters =
                        path.resolveParameters(openApi) + (operation?.resolveParameters(openApi) ?: emptyList())
                    val segments = key.value.split("/").drop(1).map { segment ->
                        val param = "^\\{(.*)}$".toRegex().find(segment)?.groupValues?.get(1)
                        when {
                            param != null -> {
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
                    val name = operation?.operationId?.let { className(it) } ?: segments
                        .joinToString("") {
                            when (it) {
                                is Endpoint.Segment.Literal -> className(it.value)
                                is Endpoint.Segment.Param -> className(it.identifier.value)
                            }
                        }
                        .let { it + method.name }
                    val query = parameters
                        .filter { it.`in` == ParameterLocation.QUERY }
                        .mapNotNull { it.toField(openApi) }
                    val headers = parameters
                        .filter { it.`in` == ParameterLocation.HEADER }
                        .mapNotNull { it.toField(openApi) }
                    val cookies = parameters
                        .filter { it.`in` == ParameterLocation.COOKIE }
                        .mapNotNull { it.toField(openApi) }
                    val requests = operation?.requestBody?.resolve(openApi)?.content
                        ?.map { (mediaType, mediaObject) ->
                            Endpoint.Request(
                                Endpoint.Content(
                                    type = mediaType.value,
                                    reference = mediaObject.schema?.toReference(openApi) ?: TODO()
                                )
                            )
                        }
                        ?: listOf(
                            Endpoint.Request(null)
                        )
                    val responses = operation?.responses
                        ?.flatMap { (status, res) ->
                            res.resolve(openApi)?.content
                                ?.map { (contentType, media) ->
                                    Endpoint.Response(
                                        status = status.value,
                                        content = Endpoint.Content(
                                            type = contentType.value,
                                            reference = media.schema?.toReference(openApi) ?: TODO()
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
            ?.flatMap { it.value.flatten(className(it.key), openApi) }
            ?.map { Type(it.name, Type.Shape(it.fields())) }
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

fun SimpleSchema.fields() = properties
    .map {
        when (it.type) {
            OpenapiType.STRING -> Field(
                Field.Identifier(it.key),
                Primitive(Primitive.Type.String, false),
                false
            )

            OpenapiType.NUMBER -> Field(
                Field.Identifier(it.key),
                Primitive(Primitive.Type.Integer, false),
                false
            )

            OpenapiType.INTEGER -> Field(
                Field.Identifier(it.key),
                Primitive(Primitive.Type.Integer, false),
                false
            )

            OpenapiType.BOOLEAN -> Field(
                Field.Identifier(it.key),
                Primitive(Primitive.Type.Boolean, false),
                false
            )

            OpenapiType.ARRAY -> it.field
            OpenapiType.OBJECT -> it.field
            null -> TODO()
        }
    }

data class SimpleSchema(val name: String, val properties: List<SimpleProp>)
data class SimpleProp(val key: String, val type: community.flock.kotlinx.openapi.bindings.Type?, val field: Field)

fun SchemaObject.flatten(
    name: String,
    openApi: OpenAPIObject,
): List<SimpleSchema> =
    when (type) {
        OpenapiType.OBJECT -> {
            val fields = properties
                ?.flatMap { (key, value) -> value.flatten(className(name, key), openApi) }
                ?: emptyList()

            listOf(
                SimpleSchema(
                    name = name,
                    properties = properties
                        ?.map { (key, value) ->
                            when (value) {
                                is SchemaObject -> SimpleProp(
                                    key = key,
                                    type = value.type,
                                    field = Field(
                                        Field.Identifier(key),
                                        Reference.Custom(className(name, key), false),
                                        false
                                    )
                                )

                                is ReferenceObject -> TODO()
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
                    is SchemaObject -> it.flatten(className(name, "array"), openApi)
                }
            }
            ?: emptyList()


        else -> emptyList()
    }

fun SchemaOrReferenceObject.flatten(
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

fun SchemaOrReferenceObject.toReference(openApi: OpenAPIObject) =
    when (this) {
        is ReferenceObject -> {
            val resolved = resolveSchemaObject(openApi) ?: TODO()
            when (resolved.second.type) {
                OpenapiType.ARRAY -> when (resolved.second.items) {
                    is ReferenceObject -> Reference.Custom(
                        className((resolved.second.items as ReferenceObject).getReference()),
                        true
                    )

                    is SchemaObject -> Reference.Custom(className(resolved.first.getReference(), "Array"), true)
                    null -> TODO()
                }

                else -> Reference.Custom(className(resolved.first.getReference()), false)
            }

        }

        is SchemaObject -> TODO()
    }

fun PathItemObject.toOperationList() = Endpoint.Method.values()
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

fun className(vararg arg: String) = arg
    .map { it.replaceFirstChar { it.uppercase() } }
    .joinToString("")


fun ReferenceObject.getReference() = this.ref.value.split("/")[3]

fun SchemaOrReferenceObject.getReference(openApi: OpenAPIObject) = when (this) {
    is ReferenceObject -> {
        this.resolveSchemaObject(openApi)
    }

    is SchemaObject -> TODO()
}

fun OpenapiType.toPrimitive() = when (this) {
    OpenapiType.STRING -> Primitive.Type.String
    OpenapiType.INTEGER -> Primitive.Type.Integer
    OpenapiType.NUMBER -> Primitive.Type.Integer
    OpenapiType.BOOLEAN -> Primitive.Type.Boolean
    else -> error("Type is not a primitive")
}

fun ParameterObject.toField(openApi: OpenAPIObject) = schema
    ?.resolve(openApi)
    ?.type
    ?.toPrimitive()
    ?.let { Field(Field.Identifier(name), Primitive(it, false), this.required ?: false) }
