package community.flock.wirespec.openapi

import community.flock.kotlinx.openapi.bindings.*
import community.flock.wirespec.compiler.core.parse.*
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
                                    ?.internalize(param, param, openApi)
                                    ?.let { it.entries.first() }
                                    ?.let { (key, value) -> EndpointDefinition.Segment.Param(key, value) }
                                    ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
                            }

                            else -> EndpointDefinition.Segment.Literal(segment)
                        }
                    }
                    val name = operation?.operationId ?: segments
                        .joinToString("") {
                            when (it) {
                                is EndpointDefinition.Segment.Literal -> className(it.value)
                                is EndpointDefinition.Segment.Param -> className(it.key)
                            }
                        }
                        .let { it + method.name }
                    val responses = operation?.responses
                        ?.map { (status, res) ->
                            res.resolve(openApi)?.content
                                ?.map { (contentType, media) ->
                                    EndpointDefinition.Response(
                                        status = status.value,
                                        contentType = contentType.value,
                                        type = Shape.Field.Value.Custom(
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

                    EndpointDefinition(
                        EndpointDefinition.Name(className(name)),
                        method,
                        segments,
                        responses
                    )
                }
            }

        val componentsAst = openApi.components?.schemas
            ?.flatMap { it.value.internalize(className(it.key), className(it.key), openApi).entries }
            ?.map { (key, value) ->
                when (value) {
                    is Shape -> TypeDefinition(TypeDefinition.Name(key), value)
                    is Shape.Field.Value -> TypeDefinition(TypeDefinition.Name(key), value)
                }
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


fun SchemaObject.internalize(
    name: String,
    prefix: String,
    openApi: OpenAPIObject,
    isIterable: Boolean = false
): Map<String, Type> =
    when (type) {
        OpenapiType.STRING -> mapOf(
            name to Shape.Field.Value.Primitive(
                Shape.Field.Value.Primitive.PrimitiveType.String,
                isIterable
            )
        )

        OpenapiType.NUMBER -> mapOf(
            name to Shape.Field.Value.Primitive(
                Shape.Field.Value.Primitive.PrimitiveType.Integer,
                isIterable
            )
        )

        OpenapiType.INTEGER -> mapOf(
            name to Shape.Field.Value.Primitive(
                Shape.Field.Value.Primitive.PrimitiveType.Integer,
                isIterable
            )
        )

        OpenapiType.BOOLEAN -> mapOf(
            name to Shape.Field.Value.Primitive(
                Shape.Field.Value.Primitive.PrimitiveType.Boolean,
                isIterable
            )
        )

        OpenapiType.OBJECT -> {
            val fields = properties
                ?.internalize(name, openApi)
                ?.entries

            val models: Map<String, Type> = fields
                ?.fold(mapOf()) { acc, (key, value) ->
                    when (value) {
                        is Shape -> acc.plus(className(name, key) to value)
                        is Shape.Field.Value -> acc
                    }
                }
                ?: TODO()

            mapOf(
                name to Shape(
                    value = fields
                        .filter { properties?.containsKey(it.key) ?: false }
                        .fold(emptyList()) { acc, (key, value) ->
                            when (value) {
                                is Shape.Field.Value -> acc + Shape.Field(Shape.Field.Key(key), value, false)
                                is Shape -> acc + Shape.Field(
                                    Shape.Field.Key(key),
                                    Shape.Field.Value.Custom(className(prefix, key), false),
                                    false
                                )
                            }
                        }
                )
            ) + models
        }

        OpenapiType.ARRAY -> items
            ?.let {
                when (it) {
                    is ReferenceObject -> mapOf(name to Shape.Field.Value.Custom(className(it.ref.getType()), true))
                    is SchemaObject -> TODO()
                }
            }
            ?: TODO()

        null -> TODO()
    }

fun SchemaOrReferenceObject.internalize(
    name: String,
    prefix: String,
    openApi: OpenAPIObject,
    isIterable: Boolean = false
): Map<String, Type> {
    return when (this) {
        is SchemaObject -> this.internalize(name, prefix, openApi, isIterable)
        is ReferenceObject -> this.resolveSchemaObject(openApi)?.internalize(name, prefix, openApi, isIterable)
            ?: error("Reference not found")
    }
}

fun Map.Entry<String, SchemaOrReferenceObject>.internalize(
    name: String,
    openApi: OpenAPIObject,
    isIterable: Boolean = false
): Map<String, Type> {
    return this.value.internalize(key, className(name, key), openApi, isIterable)
}


fun Map<String, SchemaOrReferenceObject>.internalize(
    name: String,
    openApi: OpenAPIObject,
    isIterable: Boolean = false
): Map<String, Type> {
    return this
        .flatMap {
            it.internalize(name, openApi, isIterable).entries
        }
        .associate { (k, v) -> k to v }


}

fun PathItemObject.toOperationList() = EndpointDefinition.Method.values()
    .map {
        it to when (it) {
            EndpointDefinition.Method.GET -> get
            EndpointDefinition.Method.POST -> post
            EndpointDefinition.Method.PUT -> put
            EndpointDefinition.Method.DELETE -> delete
            EndpointDefinition.Method.OPTIONS -> options
            EndpointDefinition.Method.HEAD -> head
            EndpointDefinition.Method.PATCH -> patch
            EndpointDefinition.Method.TRACE -> trace
        }
    }
    .filter { (_, value) -> value != null }

fun className(vararg arg: String) = arg
    .map { it.replaceFirstChar { it.uppercase() } }
    .joinToString("")

fun Ref.getType() = value.split("/")[3]