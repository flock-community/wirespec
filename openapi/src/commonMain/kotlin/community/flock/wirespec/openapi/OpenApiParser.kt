package community.flock.wirespec.openapi

import community.flock.kotlinx.openapi.bindings.*
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.kotlinx.openapi.bindings.Type as OpenapiType

object OpenApiParser {

    fun parse(openApi: OpenAPIObject): List<Definition> {

        val endpointAst = openApi.paths
            .flatMap { (key, path) ->
                path.toOperationList().map { (method, operation) ->
                    val segments = key.value.split("/").drop(1).map { segment ->
                        val param = "^\\{(.*)}$".toRegex().find(segment)?.groupValues?.get(1)
                        when {
                            param != null -> {
                                (path.findParameter(openApi, param) ?: operation?.findParameter(openApi, param))
                                    ?.schema
                                    ?.resolve(openApi)
                                    ?.let { (key, value) ->
                                        Endpoint.Segment.Param(
                                            param,
                                            Primitive(Primitive.Type.String, false)
                                        )
                                    }
                                    ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
                            }

                            else -> Endpoint.Segment.Literal(segment)
                        }
                    }
                    val name = operation?.operationId ?: segments
                        .joinToString("") {
                            when (it) {
                                is Endpoint.Segment.Literal -> className(it.value)
                                is Endpoint.Segment.Param -> className(it.key)
                            }
                        }
                        .let { it + method.name }
                    val responses = operation?.responses
                        ?.map { (status, res) ->
                            res.resolve(openApi)?.content
                                ?.map { (contentType, media) ->
                                    Endpoint.Response(
                                        status = status.value,
                                        contentType = contentType.value,
                                        type = Field.Reference.Custom(
                                            when (media.schema) {
                                                is ReferenceObject -> className((media.schema as ReferenceObject).ref.getType())
                                                is SchemaObject -> TODO()
                                                null -> TODO()
                                            },
                                            (media.schema as SchemaOrReferenceObject).resolve(openApi)?.type == OpenapiType.ARRAY
                                        )
                                    )
                                }
                                ?: TODO()
                        }
                        ?.flatten()
                        ?: TODO()

                    Endpoint(
                        className(name),
                        method,
                        segments,
                        responses
                    )
                }
            }

        val componentsAst = openApi.components?.schemas
            ?.flatMap { it.value.flatten(className(it.key), className(it.key), openApi).entries }
            ?.fold(listOf<Type>()) { acc, (key, value) ->
                acc + Type(key, Type.Shape(value.fields()))
            }
            ?: TODO()

        return endpointAst + componentsAst
    }
}

fun OperationObject.findParameter(openApi: OpenAPIObject, parameterName: String): ParameterObject? = parameters
    ?.map {
        when (it) {
            is ParameterObject -> it
            is ReferenceObject -> it.resolveParameterObject(openApi)
        }
    }
    ?.find { it?.name == parameterName }


fun PathItemObject.findParameter(openApi: OpenAPIObject, parameterName: String): ParameterObject? = parameters
    ?.map {
        when (it) {
            is ParameterObject -> it
            is ReferenceObject -> it.resolveParameterObject(openApi)
        }
    }
    ?.find { it?.name == parameterName }


fun ReferenceObject.resolveParameterObject(openApi: OpenAPIObject): ParameterObject? =
    openApi.components?.parameters
        ?.get(ref.getType())
        ?.let {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> it.resolveParameterObject(openApi)
            }
        }

fun ReferenceObject.resolveSchemaObject(openApi: OpenAPIObject): SchemaObject? =
    openApi.components?.schemas
        ?.get(ref.getType())
        ?.let {
            when (it) {
                is SchemaObject -> it
                is ReferenceObject -> it.resolveSchemaObject(openApi)
            }
        }

fun ReferenceObject.resolveResponseObject(openApi: OpenAPIObject): ResponseObject? =
    openApi.components?.responses
        ?.get(ref.getType())
        ?.let {
            when (it) {
                is ResponseObject -> it
                is ReferenceObject -> it.resolveResponseObject(openApi)
            }
        }

fun SchemaOrReferenceObject.resolve(openApi: OpenAPIObject): SchemaObject? =
    when (this) {
        is SchemaObject -> this
        is ReferenceObject -> this.resolveSchemaObject(openApi)
    }

fun ResponseOrReferenceObject.resolve(openApi: OpenAPIObject): ResponseObject? =
    when (this) {
        is ResponseObject -> this
        is ReferenceObject -> this.resolveResponseObject(openApi)
    }


fun SchemaObject.fields() = properties
    ?.map {
        val value = it.value
        Field(
            Field.Identifier(it.key), when (value) {
                is ReferenceObject -> TODO()
                is SchemaObject -> Primitive(Primitive.Type.String, false)
            },
            false
        )
    }
    ?: emptyList()

fun SchemaObject.flatten(
    name: String,
    prefix: String,
    openApi: OpenAPIObject,
): Map<String, SchemaObject> =
    when (type) {

        OpenapiType.OBJECT -> {
            val fields = properties
                ?.filter { !isPrimitive() }
                ?.flatMap {
                    it.value.flatten(it.key, className(name, it.key), openApi).entries
                }

            mapOf(name to this)
                .plus(fields
                    ?.filter { !isPrimitive() }
                    ?.fold(mapOf()) { acc, (key, value) -> acc + (className(name, key) to value) }
                    ?: emptyMap())

        }

        OpenapiType.ARRAY -> items
            ?.let {
                when (it) {
                    is ReferenceObject -> it.flatten(name, prefix, openApi)
                    is SchemaObject -> mapOf(name to it)
                }
            }
            ?: TODO()

        else -> mapOf()
    }

fun SchemaOrReferenceObject.flatten(
    name: String,
    prefix: String,
    openApi: OpenAPIObject,
): Map<String, SchemaObject> {
    return when (this) {
        is SchemaObject -> this.flatten(name, prefix, openApi)
        is ReferenceObject -> this.resolveSchemaObject(openApi)?.flatten(name, prefix, openApi)
            ?: error("Reference not found")
    }
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

fun Ref.getType() = value.split("/")[3]

fun SchemaObject.isPrimitive() = when(type){
    community.flock.kotlinx.openapi.bindings.Type.STRING -> true
    community.flock.kotlinx.openapi.bindings.Type.NUMBER -> true
    community.flock.kotlinx.openapi.bindings.Type.INTEGER -> true
    community.flock.kotlinx.openapi.bindings.Type.BOOLEAN -> true
    community.flock.kotlinx.openapi.bindings.Type.ARRAY -> false
    community.flock.kotlinx.openapi.bindings.Type.OBJECT -> false
    null -> TODO()
}
