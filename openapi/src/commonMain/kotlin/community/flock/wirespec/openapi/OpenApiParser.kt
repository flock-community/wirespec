package community.flock.wirespec.openapi

import community.flock.kotlinx.openapi.bindings.OpenAPIObject
import community.flock.kotlinx.openapi.bindings.OperationObject
import community.flock.kotlinx.openapi.bindings.ParameterObject
import community.flock.kotlinx.openapi.bindings.PathItemObject
import community.flock.kotlinx.openapi.bindings.Ref
import community.flock.kotlinx.openapi.bindings.ReferenceObject
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
                                        type = Reference.Custom(
                                            when (media.schema) {
                                                is ReferenceObject -> className((media.schema as ReferenceObject).getReference())
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
            ?.flatMap { it.value.flatten(className(it.key), openApi) }
            ?.map { Type(it.name, Type.Shape(it.fields())) }
            ?: emptyList()

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
        ?.get(getReference())
        ?.let {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> it.resolveParameterObject(openApi)
            }
        }

fun ReferenceObject.resolveSchemaObject(openApi: OpenAPIObject): SchemaObject? =
    openApi.components?.schemas
        ?.get(getReference())
        ?.let {
            when (it) {
                is SchemaObject -> it
                is ReferenceObject -> it.resolveSchemaObject(openApi)
            }
        }

fun ReferenceObject.resolveResponseObject(openApi: OpenAPIObject): ResponseObject? =
    openApi.components?.responses
        ?.get(getReference())
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

fun SimpleSchema.fields() = properties
    .map {
        when (it.type) {
            community.flock.kotlinx.openapi.bindings.Type.STRING -> Field(
                Field.Identifier(it.key),
                Primitive(Primitive.Type.String, false),
                false
            )

            community.flock.kotlinx.openapi.bindings.Type.NUMBER -> Field(
                Field.Identifier(it.key),
                Primitive(Primitive.Type.Integer, false),
                false
            )

            community.flock.kotlinx.openapi.bindings.Type.INTEGER -> Field(
                Field.Identifier(it.key),
                Primitive(Primitive.Type.Integer, false),
                false
            )

            community.flock.kotlinx.openapi.bindings.Type.BOOLEAN -> Field(
                Field.Identifier(it.key),
                Primitive(Primitive.Type.Boolean, false),
                false
            )

            community.flock.kotlinx.openapi.bindings.Type.ARRAY -> it.field
            community.flock.kotlinx.openapi.bindings.Type.OBJECT -> it.field
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
            ?.flatten(name, openApi)
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


fun ReferenceObject.getReference() = this.ref.value.split("/")[3]

fun SchemaOrReferenceObject.getReference(openApi: OpenAPIObject) = when(this){
    is ReferenceObject -> {
        this.resolveSchemaObject(openApi)
    }
    is SchemaObject -> TODO()
}