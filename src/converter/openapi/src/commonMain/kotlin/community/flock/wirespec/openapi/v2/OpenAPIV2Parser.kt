package community.flock.wirespec.openapi.v2

import arrow.core.filterIsInstance
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.kotlinx.openapi.bindings.v2.BaseObject
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
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.openapi.Common.className
import community.flock.wirespec.openapi.Common.filterNotNullValues
import kotlinx.serialization.json.Json
import community.flock.kotlinx.openapi.bindings.v2.Type as OpenapiType

object OpenAPIV2Parser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = AST(
        nonEmptyListOf(
            Module(
                moduleContent.fileUri,
                OpenAPI(
                    json = Json {
                        prettyPrint = true
                        ignoreUnknownKeys = !strict
                    },
                ).decodeFromString(moduleContent.content).parse().toNonEmptyListOrNull() ?: error("Cannot yield non empty AST for OpenAPI v2"),
            ),
        ),
    )

    fun SwaggerObject.parse(): List<Definition> = listOf(
        parseEndpoints(),
        parseParameters(),
        parseRequestBody(),
        parseResponseBody(),
        parseDefinitions(),
    ).reduce(List<Definition>::plus)

    private fun SwaggerObject.parseEndpoints(): List<Definition> = paths
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
                                val isNullable = false
                                Endpoint.Request(
                                    Endpoint.Content(
                                        type = type,
                                        reference = when (val schema = requestBody.schema) {
                                            is ReferenceObject -> toReference(schema, isNullable)
                                            is SchemaObject -> toReference(schema, className(name, "RequestBody"), isNullable)
                                            null -> TODO("Not yet implemented")
                                        },
                                    ),
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
                                                is ReferenceObject -> toReference(schema, false)
                                                is SchemaObject -> toReference(
                                                    schema,
                                                    className(name, status.value, "ResponseBody"),
                                                    false,
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                    }
                    .distinctBy { it.status to it.content }

                listOf(
                    Endpoint(
                        comment = null,
                        annotations = emptyList(),
                        identifier = DefinitionIdentifier(name.sanitize()),
                        method = method,
                        path = segments,
                        queries = query,
                        headers = headers,
                        requests = requests,
                        responses = responses,
                    ),
                )
            }
        }

    private fun SwaggerObject.parseParameters(): List<Definition> = flatMapRequests {
        val parameters = resolveParameters(pathItem) + resolveParameters(operation)
        val name = operation.toName() ?: (path.toName() + method.name)
        parameters
            .filter { it.`in` != ParameterLocation.BODY }
            .flatMap { parameter ->
                parameter.schema?.let { flatten(it, className(name, "Parameter", parameter.name)) } ?: emptyList()
            }
    }

    private fun SwaggerObject.parseRequestBody(): List<Definition> = flatMapRequests {
        val parameters = resolveParameters(pathItem) + (resolveParameters(operation))
        val name = operation.toName() ?: (path.toName() + method.name)
        val enums: List<Definition> = parameters.flatMap { parameter ->
            when {
                parameter.enum != null -> listOf(
                    Enum(
                        comment = null,
                        annotations = emptyList(),
                        identifier = DefinitionIdentifier(className(name, "Parameter", parameter.name).sanitize()),
                        entries = parameter.enum!!.map { it.content }.toSet(),
                    ),
                )

                else -> emptyList()
            }
        }
        val types: List<Definition> = operation.parameters
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

    private fun SwaggerObject.parseResponseBody(): List<Definition> = flatMapResponses {
        val schema = resolve(response).schema
        val name = operation.toName() ?: (path.toName() + method.name)
        when (schema) {
            is SchemaObject -> when (schema.type) {
                null, OpenapiType.OBJECT -> flatten(schema, className(name, statusCode.value, "ResponseBody"))

                OpenapiType.ARRAY ->
                    schema.items
                        ?.let { flatten(it, className(name, statusCode.value, "ResponseBody")) }
                        .orEmpty()

                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun SwaggerObject.parseDefinitions(): List<Definition> = definitions.orEmpty()
        .filterIsInstance<String, SchemaObject>()
        .filter { it.value.additionalProperties == null }
        .flatMap { flatten(it.value, className(it.key)) }

    private fun SwaggerObject.resolveParameters(operation: OperationObject) = operation.parameters.orEmpty()
        .map {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> resolveParameterObject(it)
            }
        }

    private fun SwaggerObject.resolveParameters(itemObject: PathItemObject) = itemObject.parameters.orEmpty()
        .map {
            when (it) {
                is ParameterObject -> it
                is ReferenceObject -> resolveParameterObject(it)
            }
        }

    private fun SwaggerObject.resolveParameterObject(reference: ReferenceObject) = parameters
        ?.get(reference.getReference())
        ?: error("Cannot resolve ref: ${reference.ref}")

    private fun SwaggerObject.resolveResponseObject(reference: ReferenceObject) = responses
        ?.get(reference.getReference())
        ?: error("Cannot resolve ref: ${reference.ref}")

    private fun SwaggerObject.resolveSchemaObject(reference: ReferenceObject) = definitions
        ?.get(reference.getReference())
        ?: error("Cannot resolve ref: ${reference.ref}")

    private tailrec fun SwaggerObject.resolve(schemaOrReference: SchemaOrReferenceObject): SchemaObject = when (schemaOrReference) {
        is SchemaObject -> schemaOrReference
        is ReferenceObject -> resolve(resolveSchemaObject(schemaOrReference))
    }

    private fun SwaggerObject.resolve(schemaOrReferenceOrBoolean: SchemaOrReferenceOrBooleanObject): SchemaObject = when (schemaOrReferenceOrBoolean) {
        is SchemaObject -> schemaOrReferenceOrBoolean
        is ReferenceObject -> resolve(resolveSchemaObject(schemaOrReferenceOrBoolean))
        is BooleanObject -> TODO("Not yet implemented")
    }

    private fun SwaggerObject.resolve(responseOrReference: ResponseOrReferenceObject): ResponseObject = when (responseOrReference) {
        is ResponseObject -> responseOrReference
        is ReferenceObject -> resolveResponseObject(responseOrReference)
    }

    private fun HeaderOrReferenceObject.resolve(): HeaderObject = when (this) {
        is HeaderObject -> this
        is ReferenceObject -> error("Headers cannot be referenced in OpenAPI v2")
    }

    private fun SwaggerObject.resolve(parameterOrReference: ParameterOrReferenceObject): ParameterObject = when (parameterOrReference) {
        is ParameterObject -> parameterOrReference
        is ReferenceObject -> resolveParameterObject(parameterOrReference)
    }

    private fun SwaggerObject.flatten(schemaObject: SchemaObject, name: String): List<Definition> = when {
        schemaObject.additionalProperties != null -> when (schemaObject.additionalProperties) {
            is BooleanObject -> emptyList()
            else ->
                schemaObject.additionalProperties
                    ?.let { resolve(it) }
                    ?.takeIf { it.properties != null }
                    ?.let { flatten(it, name) }
                    ?: emptyList()
        }

        schemaObject.allOf != null -> listOf(
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name.sanitize()),
                shape = Type.Shape(
                    schemaObject.allOf
                        .orEmpty()
                        .flatMap {
                            when (it) {
                                is SchemaObject -> toField(it, name)
                                is ReferenceObject -> toField(resolve(resolveSchemaObject(it)), it.getReference())
                            }
                        }
                        .distinctBy { it.identifier },
                ),
                extends = emptyList(),
            ),
        ).plus(
            schemaObject.allOf!!.flatMap {
                when (it) {
                    is ReferenceObject -> emptyList()
                    is SchemaObject -> it.properties.orEmpty().flatMap { (key, value) ->
                        when (value) {
                            is ReferenceObject -> emptyList()
                            is SchemaObject -> flatten(value, className(name, key))
                        }
                    }
                }
            },
        )

        schemaObject.enum != null ->
            schemaObject.enum!!
                .map { it.content }
                .toSet()
                .let { listOf(Enum(comment = null, annotations = emptyList(), identifier = DefinitionIdentifier(name.sanitize()), entries = it)) }

        else -> when (schemaObject.type) {
            null, OpenapiType.OBJECT -> {
                val fields = schemaObject.properties.orEmpty()
                    .flatMap { (key, value) -> flatten(value, className(name, key)) }

                val schema = listOf(
                    Type(
                        comment = null,
                        annotations = emptyList(),
                        identifier = DefinitionIdentifier(name.sanitize()),
                        shape = Type.Shape(toField(schemaObject, name)),
                        extends = emptyList(),
                    ),
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

    private fun SwaggerObject.flatten(schemaOrReference: SchemaOrReferenceObject, name: String): List<Definition> = when (schemaOrReference) {
        is SchemaObject -> flatten(schemaOrReference, name)
        is ReferenceObject -> emptyList()
    }

    private fun SwaggerObject.toReference(reference: ReferenceObject, isNullable: Boolean): Reference = resolveSchemaObject(reference).let { refOrSchema ->
        val schema = resolve(refOrSchema)
        when {
            schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
                is BooleanObject -> Reference.Dict(Reference.Any(isNullable = false), isNullable = isNullable)
                is ReferenceObject -> toReference(additionalProperties, false).toDict(isNullable)
                is SchemaObject -> toReference(additionalProperties, reference.getReference(), false).toDict(isNullable)
            }

            schema.enum != null -> Reference.Custom(
                className(reference.getReference()).sanitize(),
                isNullable = isNullable,
            )

            schema.type.isPrimitive() -> Reference.Primitive(
                schema.toPrimitive(),
                isNullable = isNullable,
            )

            else -> when (schema.type) {
                OpenapiType.ARRAY -> when (val items = schema.items) {
                    is ReferenceObject -> toReference(items, false).toIterable(isNullable)
                    is SchemaObject -> toReference(items, className(reference.getReference(), "Array"), isNullable).toIterable(isNullable)
                    null -> error("items cannot be null when type is array: ${reference.ref}")
                }

                else -> when (refOrSchema) {
                    is SchemaObject -> Reference.Custom(className(reference.getReference()).sanitize(), isNullable)
                    is ReferenceObject -> Reference.Custom(className(refOrSchema.getReference()).sanitize(), isNullable)
                }
            }
        }
    }

    private fun SwaggerObject.toReference(schema: SchemaObject, name: String, isNullable: Boolean): Reference = when {
        schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanObject -> Reference.Dict(Reference.Any(isNullable = false), isNullable = isNullable)
            is ReferenceObject -> toReference(additionalProperties, false).toDict(isNullable)
            is SchemaObject ->
                additionalProperties
                    .takeIf { it.type.isPrimitive() || it.properties != null }
                    ?.let { toReference(it, name, false).toDict(isNullable) }
                    ?: Reference.Dict(Reference.Any(isNullable = false), isNullable = isNullable)
        }

        schema.enum != null -> {
            if (schema.additionalProperties != null) {
                Reference.Dict(Reference.Custom(name.sanitize(), false), isNullable = isNullable)
            } else {
                Reference.Custom(name.sanitize(), isNullable = isNullable)
            }
        }
        else -> when (val type = schema.type) {
            OpenapiType.STRING, OpenapiType.INTEGER, OpenapiType.NUMBER, OpenapiType.BOOLEAN -> {
                if (schema.additionalProperties != null) {
                    Reference.Dict(Reference.Primitive(schema.toPrimitive(), isNullable = false), isNullable = isNullable)
                } else {
                    Reference.Primitive(schema.toPrimitive(), isNullable = isNullable)
                }
            }

            null, OpenapiType.OBJECT ->
                when {
                    schema.additionalProperties is BooleanObject -> {
                        if (schema.additionalProperties != null) {
                            Reference.Dict(Reference.Any(isNullable = isNullable), isNullable = false)
                        } else {
                            Reference.Any(isNullable = isNullable)
                        }
                    }

                    else -> {
                        if (schema.additionalProperties != null) Reference.Dict(Reference.Custom(name.sanitize(), isNullable = isNullable), isNullable = false)
                        Reference.Custom(name.sanitize(), isNullable = isNullable)
                    }
                }

            OpenapiType.ARRAY -> {
                when (val items = schema.items) {
                    is ReferenceObject -> toReference(items, false).toIterable(isNullable)
                    is SchemaObject -> toReference(items, name, false).toIterable(isNullable)
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

    private fun BaseObject.toPrimitive() = when (this.type) {
        OpenapiType.STRING -> when {
            pattern != null -> Reference.Primitive.Type.String(constraint = Reference.Primitive.Type.Constraint.RegExp(pattern!!))
            else -> Reference.Primitive.Type.String(null)
        }
        OpenapiType.INTEGER -> Reference.Primitive.Type.Integer(if (format == "int32") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64, null)
        OpenapiType.NUMBER -> Reference.Primitive.Type.Number(if (format == "float") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64, null)
        OpenapiType.BOOLEAN -> Reference.Primitive.Type.Boolean
        else -> error("Type is not a primitive")
    }

    private fun SwaggerObject.toField(header: HeaderObject, identifier: String) = Field(
        identifier = FieldIdentifier(identifier),
        reference = when (header.type) {
            OpenapiType.ARRAY -> header.items?.let { resolve(it) }?.let { toReference(it, identifier, false) }
                ?: error("Item cannot be null")

            else -> Reference.Primitive(
                header.toPrimitive(),
                isNullable = false,
            )
        },
    )

    private fun SwaggerObject.toField(schema: SchemaObject, name: String) = schema.properties.orEmpty().map { (key, value) ->
        val isNullable = !(schema.required?.contains(key) ?: false)
        when (value) {
            is SchemaObject -> {
                Field(
                    identifier = FieldIdentifier(key),
                    reference = when {
                        value.enum != null -> toReference(value, className(name, key), isNullable)
                        value.type == OpenapiType.ARRAY -> toReference(value, className(name, key, "Array"), isNullable)
                        else -> toReference(value, className(name, key), isNullable)
                    },
                )
            }

            is ReferenceObject -> {
                Field(
                    identifier = FieldIdentifier(key),
                    reference = toReference(value, isNullable),
                )
            }
        }
    }

    private fun SwaggerObject.toField(parameter: ParameterObject, name: String) = resolve(parameter)
        .let { schema ->
            val isNullable = !(parameter.required ?: false)
            when {
                parameter.enum != null -> Reference.Custom(className(name, "Parameter", schema.name).sanitize(), isNullable = isNullable)
                else -> when (schema.type) {
                    OpenapiType.STRING, OpenapiType.NUMBER, OpenapiType.INTEGER, OpenapiType.BOOLEAN ->
                        schema
                            .toPrimitive()
                            .let { primitive -> Reference.Primitive(primitive, isNullable = isNullable) }

                    OpenapiType.ARRAY -> schema.items?.let { items -> resolve(items) }
                        ?.toPrimitive()
                        ?.let { primitive ->
                            Reference.Iterable(
                                Reference.Primitive(primitive, false),
                                isNullable = isNullable,
                            )
                        }
                        ?: TODO("Not yet implemented")

                    OpenapiType.OBJECT -> TODO("Not yet implemented")
                    OpenapiType.FILE -> TODO("Not yet implemented")
                    null -> TODO("Not yet implemented")
                }
            }
        }.let { Field(FieldIdentifier(parameter.name), it) }

    private fun Path.toSegments(parameters: List<ParameterObject>) = value.split("/").drop(1).map { segment ->
        val isParam = segment.isNotEmpty() && segment[0] == '{' && segment[segment.length - 1] == '}'
        when {
            isParam -> {
                val param = segment.substring(1, segment.length - 1)
                parameters
                    .find { it.name == param }
                    ?.toPrimitive()
                    ?.let {
                        Endpoint.Segment.Param(
                            FieldIdentifier(param),
                            Reference.Primitive(it, false),
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
        val type: String,
    )

    private fun SwaggerObject.flatMapRequests(f: FlattenRequest.() -> List<Definition>) = paths
        .flatMap { (path, pathItem) ->
            pathItem.toOperationList()
                .flatMap { (method, operation) ->
                    (consumes ?: operation.consumes ?: listOf("application/json")).map { type ->
                        FlattenRequest(
                            path = path,
                            pathItem = pathItem,
                            method = method,
                            operation = operation,
                            type = type,
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
        val type: String,
    )

    private fun SwaggerObject.flatMapResponses(f: FlattenResponse.() -> List<Definition>) = paths
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
                                    type = type,
                                )
                            }
                        }
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
    OpenapiType.FILE -> false
    null -> false
}

private fun Reference.toIterable(isNullable: Boolean) = Reference.Iterable(
    reference = this,
    isNullable = isNullable,
)

private fun Reference.toDict(isNullable: Boolean) = Reference.Dict(
    reference = this,
    isNullable = isNullable,
)
