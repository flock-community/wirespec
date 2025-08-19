package community.flock.wirespec.openapi.v3

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.kotlinx.openapi.bindings.v3.BooleanObject
import community.flock.kotlinx.openapi.bindings.v3.HeaderObject
import community.flock.kotlinx.openapi.bindings.v3.HeaderOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.kotlinx.openapi.bindings.v3.OpenAPIObject
import community.flock.kotlinx.openapi.bindings.v3.OperationObject
import community.flock.kotlinx.openapi.bindings.v3.ParameterLocation
import community.flock.kotlinx.openapi.bindings.v3.ParameterObject
import community.flock.kotlinx.openapi.bindings.v3.Path
import community.flock.kotlinx.openapi.bindings.v3.PathItemObject
import community.flock.kotlinx.openapi.bindings.v3.ReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.RequestBodyObject
import community.flock.kotlinx.openapi.bindings.v3.RequestBodyOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.ResponseObject
import community.flock.kotlinx.openapi.bindings.v3.ResponseOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaOrReferenceObject
import community.flock.kotlinx.openapi.bindings.v3.SchemaOrReferenceOrBooleanObject
import community.flock.kotlinx.openapi.bindings.v3.StatusCode
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.openapi.Common.className
import community.flock.wirespec.openapi.Common.filterNotNullValues
import kotlinx.serialization.json.Json
import community.flock.kotlinx.openapi.bindings.v3.Type as OpenapiType

object OpenAPIV3Parser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = AST(
        nonEmptyListOf(
            Module(
                moduleContent.src,
                OpenAPI(
                    json = Json {
                        prettyPrint = true
                        ignoreUnknownKeys = !strict
                    },
                ).decodeFromString(moduleContent.content).parse().toNonEmptyListOrNull() ?: error("Cannot yield non empty List<Node> for OpenAPI v3"),
            ),
        ),
    )

    fun OpenAPIObject.parse(): List<Definition> = listOf(
        parseEndpoint(),
        parseParameters(),
        parseRequestBody(),
        parseResponseBody(),
        parseComponents(),
    ).reduce(List<Definition>::plus)

    private fun OpenAPIObject.parseEndpoint(): List<Definition> = paths
        .flatMap { (key, path) ->
            path.toOperationList().map { (method, operation) ->
                val parameters = resolveParameters(path) + resolveParameters(operation)
                val segments = toSegments(key, parameters, operation, method)
                val name = operation.toName() ?: (key.toName() + method.name)
                val query = parameters
                    .filter { it.`in` == ParameterLocation.QUERY }
                    .map { toField(it, className(name, "Parameter", it.name)) }
                val headers = parameters
                    .filter { it.`in` == ParameterLocation.HEADER }
                    .map { toField(it, className(name, "Parameter", it.name)) }
                val requests = operation.requestBody?.let { resolve(it) }
                    ?.let { requestBody ->
                        val isNullable = false
                        requestBody.content?.map { (mediaType, mediaObject) ->
                            val reference = when (val schema = mediaObject.schema) {
                                is ReferenceObject -> toReference(schema, isNullable)
                                is SchemaObject -> toReference(schema, isNullable, className(name, "RequestBody"))
                                null -> null
                            }
                            reference?.let {
                                Endpoint.Request(
                                    Endpoint.Content(
                                        type = mediaType.value,
                                        reference = reference,
                                    ),
                                )
                            } ?: Endpoint.Request(null)
                        }
                    }
                    ?: listOf(Endpoint.Request(null))

                val responses = operation.responses.orEmpty().flatMap { (status, res) ->
                    resolve(res).let { response ->
                        if (response.content.isNullOrEmpty()) {
                            listOf(
                                Endpoint.Response(
                                    status = status.value,
                                    headers = response.headers?.map { entry ->
                                        toField(resolve(entry.value), entry.key, className(name, "ResponseHeader"))
                                    }.orEmpty(),
                                    content = null,
                                ),
                            )
                        } else {
                            response.content?.map { (contentType, media) ->
                                val isNullable = media.schema?.let { resolve(it) }?.nullable ?: false
                                Endpoint.Response(
                                    status = status.value,
                                    headers = response.headers?.map { entry ->
                                        toField(resolve(entry.value), entry.key, className(name, "ResponseHeader"))
                                    }.orEmpty(),
                                    content = Endpoint.Content(
                                        type = contentType.value,
                                        reference = when (val schema = media.schema) {
                                            is ReferenceObject -> toReference(schema, isNullable)
                                            is SchemaObject -> toReference(
                                                schema,
                                                isNullable,
                                                className(name, status.value, "ResponseBody"),
                                            )

                                            null -> Reference.Any(isNullable)
                                        },
                                    ),
                                )
                            }
                        }
                    }
                        ?: listOf(
                            Endpoint.Response(
                                status = status.value,
                                headers = emptyList(),
                                content = null,
                            ),
                        )
                }

                Endpoint(
                    comment = null,
                    identifier = DefinitionIdentifier(name),
                    method = method,
                    path = segments,
                    queries = query,
                    headers = headers,
                    requests = requests,
                    responses = responses,
                )
            }
        }

    private fun OpenAPIObject.parseParameters(): List<Definition> = flatMapRequests {
        val parameters = resolveParameters(pathItem) + resolveParameters(operation)
        val name = operation.toName() ?: (path.toName() + method.name)
        parameters.flatMap { parameter ->
            parameter.schema?.let { flatten(it, className(name, "Parameter", parameter.name)) } ?: emptyList()
        }
    }

    private fun OpenAPIObject.parseRequestBody(): List<Definition> = flatMapRequests {
        val name = operation.toName() ?: (path.toName() + method.name)
        operation.requestBody?.let { resolve(it) }?.content.orEmpty()
            .flatMap { (_, mediaObject) ->
                when (val schema = mediaObject.schema) {
                    is SchemaObject -> when (schema.type) {
                        null, OpenapiType.OBJECT -> flatten(schema, className(name, "RequestBody"))

                        OpenapiType.ARRAY -> schema.items?.let { flatten(it, className(name, "RequestBody")) }.orEmpty()

                        else -> emptyList()
                    }

                    is ReferenceObject, null -> emptyList()
                }
            }
    }

    private fun OpenAPIObject.flatMapResponse(response: ResponseObject, name: String, statusCode: StatusCode): List<Definition> = response.content.orEmpty()
        .flatMap { (_, mediaObject) ->
            when (val schema = mediaObject.schema) {
                is SchemaObject -> when (schema.type) {
                    null, OpenapiType.OBJECT -> flatten(
                        schema,
                        className(name, statusCode.value, "ResponseBody"),
                    )

                    OpenapiType.ARRAY -> schema.items?.let {
                        flatten(
                            it,
                            className(name, statusCode.value, "ResponseBody"),
                        )
                    }.orEmpty()

                    else -> emptyList()
                }

                else -> emptyList()
            }
        }

    private fun OpenAPIObject.parseResponseBody(): List<Definition> = flatMapResponses {
        val name = operation.toName() ?: (path.toName() + method.name)
        when (val response = response) {
            is ResponseObject -> flatMapResponse(response, name, statusCode)
            is ReferenceObject -> flatMapResponse(resolveResponseObject(response).second, name, statusCode)
        }
    }

    private fun OpenAPIObject.parseComponents(): List<Definition> = components?.schemas.orEmpty()
        .filter {
            when (val s = it.value) {
                is SchemaObject -> s.additionalProperties == null
                else -> false
            }
        }
        .flatMap { flatten(it.value, className(it.key)) }

    private fun String.isParam() = this[0] == '{' && this[length - 1] == '}'

    private fun OperationObject.toName() = operationId?.let { className(it) }

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

    private fun OpenAPIObject.toSegments(
        path: Path,
        parameters: List<ParameterObject>,
        operation: OperationObject,
        method: Endpoint.Method,
    ) = path.value.split("/").drop(1).filter { it.isNotBlank() }.map { segment ->
        when (segment.isParam()) {
            true -> {
                val param = segment.substring(1, segment.length - 1)
                val name = operation.toName() ?: (path.toName() + method.name)
                parameters
                    .find { it.name == param }
                    ?.schema
                    ?.let { resolve(it) }
                    ?.let { toReference(it, false, className(name, "Parameter", param)) }
                    ?.let {
                        Endpoint.Segment.Param(
                            identifier = FieldIdentifier(param),
                            reference = it,
                        )
                    }
                    ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
            }

            false -> Endpoint.Segment.Literal(segment)
        }
    }

    private fun OpenAPIObject.resolveParameters(operation: OperationObject): List<ParameterObject> = operation.parameters
        ?.mapNotNull {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> resolveParameterObject(it)
            }
        }
        ?: emptyList()

    private fun OpenAPIObject.resolveParameters(pathItem: PathItemObject): List<ParameterObject> = pathItem.parameters
        ?.mapNotNull {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> resolveParameterObject(it)
            }
        }
        ?: emptyList()

    private fun OpenAPIObject.resolveParameterObject(reference: ReferenceObject): ParameterObject? = components?.parameters
        ?.get(reference.getReference())
        ?.let {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> resolveParameterObject(it)
            }
        }

    private fun OpenAPIObject.resolveSchemaObject(reference: ReferenceObject): Pair<ReferenceObject, SchemaObject> = components?.schemas
        ?.get(reference.getReference())
        ?.let {
            when (it) {
                is SchemaObject -> reference to it
                is ReferenceObject -> resolveSchemaObject(it)
            }
        }
        ?: error("Cannot resolve ref: ${reference.ref}")

    private fun OpenAPIObject.resolveHeaderObject(reference: ReferenceObject): Pair<ReferenceObject, HeaderObject> = components?.headers
        ?.get(reference.getReference())
        ?.let {
            when (it) {
                is HeaderObject -> reference to it
                is ReferenceObject -> resolveHeaderObject(it)
            }
        }
        ?: error("Cannot resolve ref: ${reference.ref}")

    private fun OpenAPIObject.resolveRequestBodyObject(reference: ReferenceObject): Pair<ReferenceObject, RequestBodyObject> = components?.requestBodies
        ?.get(reference.getReference())
        ?.let {
            when (it) {
                is RequestBodyObject -> reference to it
                is ReferenceObject -> resolveRequestBodyObject(it)
            }
        }
        ?: error("Cannot resolve ref: ${reference.ref}")

    private fun OpenAPIObject.resolveResponseObject(reference: ReferenceObject): Pair<ReferenceObject, ResponseObject> = components?.responses
        ?.get(reference.getReference())
        ?.let {
            when (it) {
                is ResponseObject -> reference to it
                is ReferenceObject -> resolveResponseObject(it)
            }
        }
        ?: error("Cannot resolve ref: ${reference.ref}")

    private fun OpenAPIObject.resolve(schemaOrReference: SchemaOrReferenceObject): SchemaObject = when (schemaOrReference) {
        is SchemaObject -> schemaOrReference
        is ReferenceObject -> resolveSchemaObject(schemaOrReference).second
    }

    private fun OpenAPIObject.resolve(headerOrReference: HeaderOrReferenceObject): HeaderObject = when (headerOrReference) {
        is HeaderObject -> headerOrReference
        is ReferenceObject -> resolveHeaderObject(headerOrReference).second
    }

    private fun OpenAPIObject.resolve(schemaOrReferenceOrBoolean: SchemaOrReferenceOrBooleanObject): SchemaObject = when (schemaOrReferenceOrBoolean) {
        is SchemaObject -> schemaOrReferenceOrBoolean
        is ReferenceObject -> resolveSchemaObject(schemaOrReferenceOrBoolean).second
        is BooleanObject -> TODO("Not yet implemented")
    }

    private fun OpenAPIObject.resolve(requestBodyOrReference: RequestBodyOrReferenceObject): RequestBodyObject = when (requestBodyOrReference) {
        is RequestBodyObject -> requestBodyOrReference
        is ReferenceObject -> resolveRequestBodyObject(requestBodyOrReference).second
    }

    private fun OpenAPIObject.resolve(responseOrReferenceObject: ResponseOrReferenceObject): ResponseObject = when (responseOrReferenceObject) {
        is ResponseObject -> responseOrReferenceObject
        is ReferenceObject -> resolveResponseObject(responseOrReferenceObject).second
    }

    private fun OpenAPIObject.flatten(schemaObject: SchemaObject, name: String): List<Definition> = when {
        schemaObject.additionalProperties != null -> when (schemaObject.additionalProperties) {
            is BooleanObject -> emptyList()
            else ->
                schemaObject.additionalProperties
                    ?.let { resolve(it) }
                    ?.takeIf { it.properties != null }
                    ?.let { flatten(it, name) }
                    ?: emptyList()
        }

        schemaObject.oneOf != null || schemaObject.anyOf != null -> listOf(
            Union(
                comment = null,
                identifier = DefinitionIdentifier(name.sanitize()),
                entries = schemaObject.oneOf
                    .orEmpty()
                    .mapIndexed { index, it ->
                        when (it) {
                            is ReferenceObject -> toReference(it, false)
                            is SchemaObject -> toReference(it, false, className(name, index.toString()))
                        }
                    }
                    .toSet(),

            ),
        )
            .plus(
                schemaObject.oneOf.orEmpty().flatMapIndexed { index, it ->
                    when (it) {
                        is ReferenceObject -> emptyList()
                        is SchemaObject -> flatten(it, className(name, index.toString()))
                    }
                },
            )

        schemaObject.allOf != null -> listOf(
            Type(
                comment = null,
                identifier = DefinitionIdentifier(name.sanitize()),
                shape = Type.Shape(
                    schemaObject.allOf.orEmpty().flatMap { toField(resolve(it), name) }
                        .distinctBy { it.identifier },
                ),
                extends = emptyList(),
            ),
        )
            .plus(
                schemaObject.allOf!!
                    .flatMap {
                        when (it) {
                            is ReferenceObject -> resolveSchemaObject(it).second.properties.orEmpty()
                            is SchemaObject -> it.properties.orEmpty()
                        }
                            .flatMap { (key, value) ->
                                flatten(value, className(name, key))
                            }
                    },
            )

        schemaObject.enum != null ->
            schemaObject.enum!!
                .map { it.content }
                .toSet()
                .let { listOf(Enum(comment = null, identifier = DefinitionIdentifier(name), entries = it)) }

        else -> when (schemaObject.type) {
            null, OpenapiType.OBJECT -> {
                val fields = schemaObject.properties.orEmpty().flatMap { (key, value) ->
                    flatten(value, className(name, key))
                }
                val schema = listOf(
                    Type(
                        comment = null,
                        identifier = DefinitionIdentifier(name),
                        shape = Type.Shape(toField(schemaObject, name)),
                        extends = emptyList(),
                    ),
                )

                schema + fields
            }

            OpenapiType.ARRAY ->
                schemaObject.items
                    ?.let {
                        when (it) {
                            is ReferenceObject -> emptyList()
                            is SchemaObject -> flatten(it, className(name, "array"))
                        }
                    }
                    ?: emptyList()

            else -> emptyList()
        }
    }

    private fun OpenAPIObject.flatten(schemaOrReference: SchemaOrReferenceObject, name: String) = when (schemaOrReference) {
        is SchemaObject -> flatten(schemaOrReference, name)
        is ReferenceObject -> emptyList()
    }

    private fun OpenAPIObject.toReference(reference: ReferenceObject, isNullable: Boolean): Reference = resolveSchemaObject(reference).let { (referencingObject, schema) ->
        when {
            schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
                is BooleanObject -> Reference.Dict(
                    reference = Reference.Any(isNullable = isNullable),
                    isNullable = false,
                )

                is ReferenceObject -> toReference(additionalProperties, schema.nullable ?: false).toDict(isNullable)
                is SchemaObject -> toReference(
                    additionalProperties,
                    schema.nullable ?: false,
                    reference.getReference(),
                ).toDict(false)
            }

            schema.enum != null -> Reference.Custom(
                value = className(referencingObject.getReference()).sanitize(),
                isNullable = isNullable,
            )

            schema.type.isPrimitive() -> Reference.Primitive(
                type = schema.toPrimitive(),
                isNullable = isNullable,
            )

            schema.type == OpenapiType.ARRAY -> when (val items = schema.items) {
                is ReferenceObject -> toReference(items, schema.nullable ?: false).toIterable(isNullable)
                is SchemaObject -> Reference.Custom(
                    className(referencingObject.getReference(), "Array").sanitize(),
                    schema.nullable ?: false,
                ).toIterable(isNullable)

                null -> error("items cannot be null when type is array: ${reference.ref}")
            }

            else -> Reference.Custom(
                value = className(referencingObject.getReference()).sanitize(),
                isNullable = isNullable,
            )
        }
    }

    private fun OpenAPIObject.toReference(
        schema: SchemaObject,
        isNullable: Boolean,
        name: String = "",
    ): Reference = when {
        schema.type == OpenapiType.ARRAY -> {
            when (val items = schema.items) {
                is ReferenceObject -> toReference(items, schema.nullable ?: false).toIterable(isNullable)
                is SchemaObject -> toReference(items, schema.nullable ?: false, name).toIterable(isNullable)
                null -> error("property 'items' of '$name' cannot be null when 'type' is array: $schema ")
            }
        }

        schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanObject -> Reference.Dict(
                reference = Reference.Any(isNullable = schema.nullable ?: false),
                isNullable = isNullable,
            )

            is ReferenceObject -> toReference(additionalProperties, schema.nullable ?: false).toDict(isNullable)
            is SchemaObject ->
                additionalProperties
                    .takeIf { it.type.isPrimitive() || it.properties != null }
                    ?.let { toReference(it, schema.nullable ?: false, name).toDict(isNullable) }
                    ?: Reference.Dict(
                        reference = Reference.Any(isNullable = schema.nullable ?: false),
                        isNullable = isNullable,
                    )
        }

        schema.enum != null -> Reference.Custom(value = name.sanitize(), isNullable = isNullable)
            .let { if (schema.additionalProperties != null) Reference.Dict(reference = it, isNullable = false) else it }

        else -> when (schema.type) {
            OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> Reference.Primitive(
                type = schema.toPrimitive(),
                isNullable = isNullable,
            ).let { if (schema.additionalProperties != null) Reference.Dict(it, isNullable = false) else it }

            null, OpenapiType.OBJECT ->
                when {
                    schema.additionalProperties is BooleanObject -> Reference.Any(isNullable = false)
                        .let { if (schema.additionalProperties != null) Reference.Dict(it, isNullable = false) else it }

                    else -> Reference.Custom(
                        value = name.sanitize(),
                        isNullable = isNullable,
                    ).let { if (schema.additionalProperties != null) Reference.Dict(it, isNullable = false) else it }
                }

            OpenapiType.ARRAY -> {
                when (val it = schema.items) {
                    is ReferenceObject -> toReference(it, schema.nullable ?: false).toIterable(isNullable)
                    is SchemaObject -> toReference(it, schema.nullable ?: false, name).toIterable(isNullable)
                    null -> error("When schema is of type array items cannot be null for name: $name")
                }
            }
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

    private fun ReferenceObject.getReference() = ref.value
        .split("/").getOrNull(3)
        ?: error("Wrong reference: ${ref.value}")

    private fun SchemaObject.toPrimitive() = when (this.type) {
        OpenapiType.STRING -> when {
            pattern != null -> Reference.Primitive.Type.String(constraint = Reference.Primitive.Type.Constraint.RegExp(pattern!!))
            else -> Reference.Primitive.Type.String(null)
        }
        OpenapiType.INTEGER -> Reference.Primitive.Type.Integer(if (format == "int32") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64, null)
        OpenapiType.NUMBER -> Reference.Primitive.Type.Number(if (format == "float") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64, null)
        OpenapiType.BOOLEAN -> Reference.Primitive.Type.Boolean
        else -> error("Type is not a primitive")
    }

    private fun OpenAPIObject.toField(schema: SchemaObject, name: String) = schema.properties.orEmpty().map { (key, value) ->
        val isNullable = !(schema.required?.contains(key) ?: false)
        when (value) {
            is SchemaObject -> {
                Field(
                    identifier = FieldIdentifier(key),
                    reference = when {
                        value.enum != null -> toReference(value, isNullable, className(name, key))
                        value.type == OpenapiType.ARRAY -> toReference(
                            value,
                            isNullable,
                            className(name, key, "Array"),
                        )

                        else -> toReference(value, isNullable, className(name, key))
                    },
                )
            }

            is ReferenceObject -> {
                Field(
                    FieldIdentifier(key),
                    toReference(value, isNullable),
                )
            }
        }
    }

    private fun OpenAPIObject.toField(parameter: ParameterObject, name: String): Field {
        val isNullable = !(parameter.required ?: false)
        return when (val s = parameter.schema) {
            is ReferenceObject -> toReference(s, isNullable)
            is SchemaObject -> toReference(s, isNullable, name + if (s.type == OpenapiType.ARRAY) "Array" else "")
            null -> Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = isNullable)
        }.let { Field(FieldIdentifier(parameter.name), it) }
    }

    private fun OpenAPIObject.toField(header: HeaderObject, identifier: String, name: String): Field {
        val isNullable = !(header.required ?: false)
        return when (val s = header.schema) {
            is ReferenceObject -> toReference(s, isNullable)
            is SchemaObject -> toReference(s, isNullable, name)
            null -> Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = isNullable)
        }.let { Field(FieldIdentifier(identifier), it) }
    }

    private data class FlattenRequest(
        val path: Path,
        val pathItem: PathItemObject,
        val method: Endpoint.Method,
        val operation: OperationObject,
    )

    private fun OpenAPIObject.flatMapRequests(f: FlattenRequest.() -> List<Definition>) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList().map { (method, operation) ->
                FlattenRequest(path = path, pathItem = pathItem, method = method, operation = operation)
            }
        }
        .flatMap(f)

    private data class FlattenResponse(
        val path: Path,
        val pathItem: PathItemObject,
        val method: Endpoint.Method,
        val operation: OperationObject,
        val statusCode: StatusCode,
        val response: ResponseOrReferenceObject,
    )

    private fun OpenAPIObject.flatMapResponses(f: FlattenResponse.() -> List<Definition>) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList()
                .flatMap { (method, operation) ->
                    operation.responses?.map { (statusCode, response) ->
                        FlattenResponse(
                            path = path,
                            pathItem = pathItem,
                            method = method,
                            operation = operation,
                            statusCode = statusCode,
                            response = response,
                        )
                    }.orEmpty()
                }
        }
        .flatMap(f)
}

private fun String.sanitize() = this
    .split(".", " ", "-")
    .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
    .joinToString("")
    .asSequence()
    .filter { it.isLetterOrDigit() || it in listOf('_') }
    .joinToString("")

private fun OpenapiType?.isPrimitive() = when (this) {
    OpenapiType.STRING -> true
    OpenapiType.NUMBER -> true
    OpenapiType.INTEGER -> true
    OpenapiType.BOOLEAN -> true
    OpenapiType.ARRAY -> false
    OpenapiType.OBJECT -> false
    null -> false
}

private fun Reference.toIterable(isNullable: Boolean) = Reference.Iterable(reference = this, isNullable = isNullable)

private fun Reference.toDict(isNullable: Boolean) = Reference.Dict(reference = this, isNullable = isNullable)
