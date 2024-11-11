package community.flock.wirespec.openapi.v2

import arrow.core.filterIsInstance
import community.flock.kotlinx.openapi.bindings.v2.BooleanObject
import community.flock.kotlinx.openapi.bindings.v2.HeaderObject
import community.flock.kotlinx.openapi.bindings.v2.HeaderOrReferenceObject
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
import community.flock.wirespec.compiler.core.parse.ClassIdentifier
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.openapi.Common.className
import community.flock.wirespec.openapi.Common.filterNotNullValues
import kotlinx.serialization.json.Json
import community.flock.kotlinx.openapi.bindings.v2.Type as OpenapiType

object OpenApiV2Parser {

    fun parse(json: String, ignoreUnknown: Boolean = false): AST =
        OpenAPI(json = Json { prettyPrint = true; ignoreUnknownKeys = ignoreUnknown })
            .decodeFromString(json).parse()

    fun SwaggerObject.parse(): AST = listOf(
        parseEndpoints(),
        parseParameters(),
        parseRequestBody(),
        parseResponseBody(),
        parseDefinitions(),
    ).reduce(AST::plus)

    private fun SwaggerObject.parseEndpoints(): AST = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList().flatMap { (method, operation) ->
                val parameters = resolveParameters(pathItem) + resolveParameters(operation)
                val segments = path.toSegments(parameters)
                val name = operation.toName() ?: (path.toName() + method.name)
                val query = parameters
                    .filter { it.`in` == ParameterLocation.QUERY }
                    .map { toField(it, name) }
                val headers = parameters
                    .filter { it.`in` == ParameterLocation.HEADER }
                    .map { toField(it, name) }
                val requests = parameters
                    .filter { it.`in` == ParameterLocation.BODY }
                    .flatMap { requestBody ->
                        (consumes.orEmpty() + operation.consumes.orEmpty())
                            .distinct()
                            .ifEmpty { listOf("application/json") }
                            .map { type ->
                                Endpoint.Request(
                                    Endpoint.Content(
                                        type = type,
                                        reference = when (val schema = requestBody.schema) {
                                            is ReferenceObject -> toReference(schema)
                                            is SchemaObject -> toReference(schema, className(name, "RequestBody"))
                                            null -> TODO("Not yet implemented")
                                        },
                                        isNullable = !(requestBody.required ?: false)
                                    )
                                )
                            }
                    }
                    .ifEmpty { listOf(Endpoint.Request(null)) }
                val responses = operation.responses.orEmpty()
                    .mapValues { resolve(it.value) }
                    .flatMap { (status, res) ->
                        (produces.orEmpty() + operation.produces.orEmpty())
                            .distinct()
                            .ifEmpty { listOf("application/json") }.map { type ->
                                Endpoint.Response(
                                    status = status.value,
                                    headers = res.headers
                                        ?.map { (identifier, header) -> toField(header.resolve(), identifier) }
                                        .orEmpty(),
                                    content = res.schema?.let { schema ->
                                        Endpoint.Content(
                                            type = type,
                                            reference = when (schema) {
                                                is ReferenceObject -> toReference(schema)
                                                is SchemaObject -> toReference(
                                                    schema,
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
                        identifier = ClassIdentifier(name),
                        method = method,
                        path = segments,
                        queries = query,
                        headers = headers,
                        cookies = emptyList(),
                        requests = requests,
                        responses = responses,
                    )
                )

            }
        }

    private fun SwaggerObject.parseParameters(): AST = flatMapRequests {
        val parameters = resolveParameters(pathItem) + resolveParameters(operation)
        val name = operation.toName() ?: (path.toName() + method.name)
        parameters
            .filter { it.`in` != ParameterLocation.BODY }
            .flatMap { parameter ->
                parameter.schema?.let { flatten(it, className(name, "Parameter", parameter.name)) } ?: emptyList()
            }
    }

    private fun SwaggerObject.parseRequestBody(): AST = flatMapRequests {
        val parameters = resolveParameters(pathItem) + (resolveParameters(operation))
        val name = operation.toName() ?: (path.toName() + method.name)
        val enums: List<Definition> = parameters.flatMap { parameter ->
            when {
                parameter.enum != null -> listOf(
                    Enum(
                        comment = null,
                        identifier = ClassIdentifier(className(name, "Parameter", parameter.name)),
                        entries = parameter.enum!!.map { it.content }.toSet()
                    )
                )

                else -> emptyList()
            }
        }
        val types: AST = operation.parameters
            ?.map { resolve(it) }
            ?.filter { it.`in` == ParameterLocation.BODY }
            ?.flatMap { param ->
                when (val schema = param.schema) {
                    is SchemaObject -> when (schema.type) {
                        null, OpenapiType.OBJECT -> flatten(schema, className(name, "RequestBody"))

                        OpenapiType.ARRAY -> schema.items?.let { flatten(it, className(name, "RequestBody")) }.orEmpty()

                        else -> emptyList()
                    }

                    else -> emptyList()
                }
            }
            ?: emptyList()

        enums + types
    }

    private fun SwaggerObject.parseResponseBody(): AST = flatMapResponses {
        val schema = resolve(response).schema
        val name = operation.toName() ?: (path.toName() + method.name)
        when (schema) {
            is SchemaObject -> when (schema.type) {
                null, OpenapiType.OBJECT -> flatten(schema, className(name, statusCode.value, type, "ResponseBody"))

                OpenapiType.ARRAY -> schema.items
                    ?.let { flatten(it, className(name, statusCode.value, type, "ResponseBody")) }
                    .orEmpty()

                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun SwaggerObject.parseDefinitions(): AST = definitions.orEmpty()
        .filterIsInstance<String, SchemaObject>()
        .filter { it.value.additionalProperties == null }
        .flatMap { flatten(it.value, className(it.key)) }

    private fun String.mapType() = when (lowercase()) {
        "string" -> Reference.Primitive.Type.String
        "number" -> Reference.Primitive.Type.Number
        "integer" -> Reference.Primitive.Type.Integer
        "boolean" -> Reference.Primitive.Type.Boolean
        else -> error("Cannot map type: $this")
    }

    private fun SwaggerObject.resolveParameters(operation: OperationObject) =
        operation.parameters.orEmpty()
            .map {
                when (it) {
                    is ParameterObject -> it
                    is ReferenceObject -> resolveParameterObject(it)
                }
            }

    private fun SwaggerObject.resolveParameters(itemObject: PathItemObject) =
        itemObject.parameters.orEmpty()
            .map {
                when (it) {
                    is ParameterObject -> it
                    is ReferenceObject -> resolveParameterObject(it)
                }
            }

    private fun SwaggerObject.resolveParameterObject(reference: ReferenceObject) =
        parameters
            ?.get(reference.getReference())
            ?: error("Cannot resolve ref: ${reference.ref}")

    private fun SwaggerObject.resolveResponseObject(reference: ReferenceObject) =
        responses
            ?.get(reference.getReference())
            ?: error("Cannot resolve ref: ${reference.ref}")

    private fun SwaggerObject.resolveSchemaObject(reference: ReferenceObject) =
        definitions
            ?.get(reference.getReference())
            ?: error("Cannot resolve ref: ${reference.ref}")

    private tailrec fun SwaggerObject.resolve(schemaOrReference: SchemaOrReferenceObject): SchemaObject =
        when (schemaOrReference) {
            is SchemaObject -> schemaOrReference
            is ReferenceObject -> resolve(resolveSchemaObject(schemaOrReference))
        }

    private fun SwaggerObject.resolve(schemaOrReferenceOrBoolean: SchemaOrReferenceOrBooleanObject): SchemaObject =
        when (schemaOrReferenceOrBoolean) {
            is SchemaObject -> schemaOrReferenceOrBoolean
            is ReferenceObject -> resolve(resolveSchemaObject(schemaOrReferenceOrBoolean))
            is BooleanObject -> TODO("Not yet implemented")
        }

    private fun SwaggerObject.resolve(responseOrReference: ResponseOrReferenceObject): ResponseObject =
        when (responseOrReference) {
            is ResponseObject -> responseOrReference
            is ReferenceObject -> resolveResponseObject(responseOrReference)
        }

    private fun HeaderOrReferenceObject.resolve(): HeaderObject =
        when (this) {
            is HeaderObject -> this
            is ReferenceObject -> error("Headers cannot be referenced in open api v2")
        }

    private fun SwaggerObject.resolve(parameterOrReference: ParameterOrReferenceObject): ParameterObject =
        when (parameterOrReference) {
            is ParameterObject -> parameterOrReference
            is ReferenceObject -> resolveParameterObject(parameterOrReference)
        }

    private fun SwaggerObject.flatten(schemaObject: SchemaObject, name: String): AST = when {
        schemaObject.additionalProperties != null -> when (schemaObject.additionalProperties) {
            is BooleanObject -> emptyList()
            else -> schemaObject.additionalProperties
                ?.let { resolve(it) }
                ?.takeIf { it.properties != null }
                ?.let { flatten(it, name) }
                ?: emptyList()
        }

        schemaObject.allOf != null -> listOf(
            Type(
                comment = null,
                identifier = ClassIdentifier(name),
                shape = Type.Shape(schemaObject.allOf
                    .orEmpty()
                    .flatMap {
                        when (it) {
                            is SchemaObject -> toField(it, name)
                            is ReferenceObject -> toField(resolve(resolveSchemaObject(it)), it.getReference())
                        }
                    }
                    .distinctBy { it.identifier }),
                extends = emptyList(),
            )
        ).plus(schemaObject.allOf!!.flatMap {
            when (it) {
                is ReferenceObject -> emptyList()
                is SchemaObject -> it.properties.orEmpty().flatMap { (key, value) ->
                    when (value) {
                        is ReferenceObject -> emptyList()
                        is SchemaObject -> flatten(value, className(name, key))
                    }
                }
            }
        })

        schemaObject.enum != null -> schemaObject.enum!!
            .map { it.content }
            .toSet()
            .let { listOf(Enum(comment = null, identifier = ClassIdentifier(name), entries = it)) }

        else -> when (schemaObject.type) {
            null, OpenapiType.OBJECT -> {
                val fields = schemaObject.properties.orEmpty()
                    .flatMap { (key, value) -> flatten(value, className(name, key)) }

                val schema = listOf(
                    Type(
                        comment = null,
                        identifier = ClassIdentifier(name),
                        shape = Type.Shape(toField(schemaObject, name)),
                        extends = emptyList(),
                    )
                )
                schema + fields
            }

            OpenapiType.ARRAY -> when (val it = schemaObject.items) {
                is ReferenceObject -> emptyList()
                is SchemaObject -> flatten(it, className(name, "Array"))
                null -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun SwaggerObject.flatten(schemaOrReference: SchemaOrReferenceObject, name: String): AST =
        when (schemaOrReference) {
            is SchemaObject -> flatten(schemaOrReference, name)
            is ReferenceObject -> emptyList()
        }

    private fun SwaggerObject.toReference(reference: ReferenceObject): Reference =
        resolveSchemaObject(reference).let { refOrSchema ->
            val schema = resolve(refOrSchema)
            when {
                schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
                    is BooleanObject -> Reference.Any(isIterable = false, isDictionary = true)
                    is ReferenceObject -> toReference(additionalProperties).toMap()
                    is SchemaObject -> toReference(additionalProperties, reference.getReference()).toMap()
                }

                schema.enum != null -> Reference.Custom(
                    className(reference.getReference()),
                    isIterable = false,
                    isDictionary = false
                )

                schema.type.isPrimitive() -> Reference.Primitive(
                    schema.type!!.toPrimitive(),
                    isIterable = false,
                    isDictionary = false
                )

                else -> when (schema.type) {
                    OpenapiType.ARRAY -> when (val items = schema.items) {
                        is ReferenceObject -> Reference.Custom(className(items.getReference()), true)
                        is SchemaObject -> toReference(items, className(reference.getReference(), "Array")).toIterable()
                        null -> error("items cannot be null when type is array: ${reference.ref}")
                    }

                    else -> when (refOrSchema) {
                        is SchemaObject -> Reference.Custom(className(reference.getReference()), false)
                        is ReferenceObject -> Reference.Custom(className(refOrSchema.getReference()), false)
                    }

                }
            }
        }


    private fun SwaggerObject.toReference(schema: SchemaObject, name: String): Reference = when {
        schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanObject -> Reference.Any(isIterable = false, isDictionary = true)
            is ReferenceObject -> toReference(additionalProperties).toMap()
            is SchemaObject -> additionalProperties
                .takeIf { it.type.isPrimitive() || it.properties != null }
                ?.let { toReference(it, name).toMap() }
                ?: Reference.Any(isIterable = false, isDictionary = true)
        }

        schema.enum != null -> Reference.Custom(name, false, schema.additionalProperties != null)
        else -> when (val type = schema.type) {
            OpenapiType.STRING, OpenapiType.INTEGER, OpenapiType.NUMBER, OpenapiType.BOOLEAN ->
                Reference.Primitive(type.toPrimitive(), false, schema.additionalProperties != null)

            null, OpenapiType.OBJECT ->
                when {
                    schema.additionalProperties is BooleanObject -> Reference.Any(
                        false,
                        schema.additionalProperties != null
                    )

                    else -> Reference.Custom(name, false, schema.additionalProperties != null)
                }

            OpenapiType.ARRAY -> {
                when (val items = schema.items) {
                    is ReferenceObject -> toReference(items).toIterable()
                    is SchemaObject -> toReference(items, name).toIterable()
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

    private fun ReferenceObject.getReference() = ref.value.split("/")[2]

    private fun OpenapiType.toPrimitive() = when (this) {
        OpenapiType.STRING -> Reference.Primitive.Type.String
        OpenapiType.INTEGER -> Reference.Primitive.Type.Integer
        OpenapiType.NUMBER -> Reference.Primitive.Type.Number
        OpenapiType.BOOLEAN -> Reference.Primitive.Type.Boolean
        else -> error("Type is not a primitive")
    }

    private fun SwaggerObject.toField(header: HeaderObject, identifier: String) =
        Field(
            identifier = FieldIdentifier(identifier),
            reference = when (header.type) {
                "array" -> header.items?.let { resolve(it) }?.let { toReference(it, identifier) }
                    ?: error("Item cannot be null")

                else -> Reference.Primitive(header.type.mapType(), isIterable = false, isDictionary = false)
            },
            isNullable = true
        )

    private fun SwaggerObject.toField(schema: SchemaObject, name: String) =
        schema.properties.orEmpty().map { (key, value) ->
            when (value) {
                is SchemaObject -> {
                    Field(
                        identifier = FieldIdentifier(key),
                        reference = when {
                            value.enum != null -> toReference(value, className(name, key))
                            value.type == OpenapiType.ARRAY -> toReference(value, className(name, key, "Array"))
                            else -> toReference(value, className(name, key))
                        },
                        isNullable = !(schema.required?.contains(key) ?: false)
                    )
                }

                is ReferenceObject -> {
                    Field(
                        identifier = FieldIdentifier(key),
                        reference = toReference(value),
                        isNullable = !(schema.required?.contains(key) ?: false)
                    )
                }
            }
        }

    private fun SwaggerObject.toField(parameter: ParameterObject, name: String) = resolve(parameter)
        .let {
            when {
                parameter.enum != null -> Reference.Custom(className(name, "Parameter", it.name), false)
                else -> when (val type = it.type) {
                    OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN -> type
                        .toPrimitive()
                        .let { primitive -> Reference.Primitive(primitive, isIterable = false) }

                    OpenapiType.ARRAY -> it.items?.let { items -> resolve(items) }
                        ?.type
                        ?.toPrimitive()
                        ?.let { primitive -> Reference.Primitive(primitive, isIterable = true) }
                        ?: TODO("Not yet implemented")

                    OpenapiType.OBJECT -> TODO("Not yet implemented")
                    OpenapiType.FILE -> TODO("Not yet implemented")
                    null -> TODO("Not yet implemented")
                }

            }
        }.let { Field(FieldIdentifier(parameter.name), it, !(parameter.required ?: false)) }

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
                            FieldIdentifier(param),
                            Reference.Primitive(it, false)
                        )
                    }
                    ?: error(" Declared path parameter $param needs to be defined as a path parameter in path or operation level")
            }

            else -> Endpoint.Segment.Literal(segment)
        }
    }

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

    private data class FlattenRequest(
        val path: Path,
        val pathItem: PathItemObject,
        val method: Endpoint.Method,
        val operation: OperationObject,
        val type: String
    )

    private fun SwaggerObject.flatMapRequests(f: FlattenRequest.() -> AST) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList()
                .flatMap { (method, operation) ->
                    (consumes ?: operation.consumes ?: listOf("application/json")).map { type ->
                        FlattenRequest(
                            path = path,
                            pathItem = pathItem,
                            method = method,
                            operation = operation,
                            type = type
                        )
                    }
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
        val type: String
    )

    private fun SwaggerObject.flatMapResponses(f: FlattenResponse.() -> AST) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList()
                .flatMap { (method, operation) ->
                    operation.responses.orEmpty()
                        .flatMap { (statusCode, response) ->
                            (produces ?: operation.produces ?: listOf("application/json")).map { type ->
                                FlattenResponse(
                                    path = path,
                                    pathItem = pathItem,
                                    method = method,
                                    operation = operation,
                                    statusCode = statusCode,
                                    response = response,
                                    type = type
                                )
                            }
                        }
                }
        }
        .flatMap(f)
}

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
    is Reference.Custom -> copy(isIterable = true)
    is Reference.Any -> copy(isIterable = true)
    is Reference.Primitive -> copy(isIterable = true)
    is Reference.Unit -> copy(isIterable = true)
}

private fun Reference.toMap() = when (this) {
    is Reference.Custom -> copy(isDictionary = true)
    is Reference.Any -> copy(isDictionary = true)
    is Reference.Primitive -> copy(isDictionary = true)
    is Reference.Unit -> copy(isDictionary = true)
}
