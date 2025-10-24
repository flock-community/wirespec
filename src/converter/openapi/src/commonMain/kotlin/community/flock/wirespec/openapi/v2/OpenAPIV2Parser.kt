package community.flock.wirespec.openapi.v2

import arrow.core.NonEmptyList
import arrow.core.filterIsInstance
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.kotlinx.openapi.bindings.BooleanValue
import community.flock.kotlinx.openapi.bindings.OpenAPIV2
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Base
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Boolean
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV2HeaderOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Model
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Operation
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV2ParameterLocation
import community.flock.kotlinx.openapi.bindings.OpenAPIV2ParameterOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV2PathItem
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Reference
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV2ResponseOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV2SchemaOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV2SchemaOrReferenceOrBoolean
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Type
import community.flock.kotlinx.openapi.bindings.Path
import community.flock.kotlinx.openapi.bindings.StatusCode
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.openapi.common.APPLICATION_JSON
import community.flock.wirespec.openapi.common.className
import community.flock.wirespec.openapi.common.filterNotNullValues
import community.flock.wirespec.openapi.toDescriptionAnnotationList
import kotlinx.serialization.json.Json

object OpenAPIV2Parser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = AST(
        nonEmptyListOf(
            Module(
                moduleContent.fileUri,
                OpenAPIV2(
                    json = Json {
                        prettyPrint = true
                        ignoreUnknownKeys = !strict
                    },
                ).decodeFromString(moduleContent.content)
                    .parse()
                    .let { requireNotNull(it) { "Cannot yield empty AST for OpenAPI v2" } },
            ),
        ),
    )

    fun OpenAPIV2Model.parse(): NonEmptyList<Definition>? = listOf(
        parseEndpoints(),
        parseParameters(),
        parseRequestBody(),
        parseResponseBody(),
        parseDefinitions(),
    ).reduce(List<Definition>::plus).toNonEmptyListOrNull()
}

private fun OpenAPIV2Model.parseEndpoints(): List<Definition> = paths
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList().flatMap { (method, operation) ->
            val parameters = resolveParameters(pathItem) + resolveParameters(operation)
            val segments = path.toSegments(parameters)
            val name = operation.toName() ?: (path.toName() + method.name)
            val query = parameters
                .filter { it.`in` == OpenAPIV2ParameterLocation.QUERY }
                .map { toField(it, name) }
            val headers = parameters
                .filter { it.`in` == OpenAPIV2ParameterLocation.HEADER }
                .map { toField(it, name) }
            val requests = parameters
                .filter { it.`in` == OpenAPIV2ParameterLocation.BODY }
                .flatMap { requestBody ->
                    (consumes.orEmpty() + operation.consumes.orEmpty())
                        .distinct()
                        .ifEmpty { listOf(APPLICATION_JSON) }
                        .map { type ->
                            val isNullable = false
                            Endpoint.Request(
                                Endpoint.Content(
                                    type = type,
                                    reference = when (val schema = requestBody.schema) {
                                        is OpenAPIV2Reference -> toReference(schema, isNullable)
                                        is OpenAPIV2Schema -> toReference(
                                            schema,
                                            className(name, "RequestBody"),
                                            isNullable,
                                        )

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
                        .ifEmpty { listOf(APPLICATION_JSON) }.map { type ->
                            Endpoint.Response(
                                annotations = res.description.toDescriptionAnnotationList(),
                                status = status.value,
                                headers = res.headers
                                    ?.map { (identifier, header) -> toField(header.resolve(), identifier) }
                                    .orEmpty(),
                                content = res.schema?.let { schema ->
                                    Endpoint.Content(
                                        type = type,
                                        reference = when (schema) {
                                            is OpenAPIV2Reference -> toReference(schema, false)
                                            is OpenAPIV2Schema -> toReference(
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
                    annotations = operation.description.toDescriptionAnnotationList(),
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

private fun OpenAPIV2Model.parseParameters(): List<Definition> = flatMapRequests {
    val parameters = resolveParameters(pathItem) + resolveParameters(operation)
    val name = operation.toName() ?: (path.toName() + method.name)
    parameters
        .filter { it.`in` != OpenAPIV2ParameterLocation.BODY }
        .flatMap { parameter ->
            parameter.schema?.let { flatten(it, className(name, "Parameter", parameter.name)) } ?: emptyList()
        }
}

private fun OpenAPIV2Model.parseRequestBody(): List<Definition> = flatMapRequests {
    val parameters = resolveParameters(pathItem) + (resolveParameters(operation))
    val name = operation.toName() ?: (path.toName() + method.name)
    val enums: List<Definition> = parameters.flatMap { parameter ->
        when {
            parameter.enum != null -> listOf(
                Enum(
                    comment = null,
                    annotations = parameter.description.toDescriptionAnnotationList(),
                    identifier = DefinitionIdentifier(className(name, "Parameter", parameter.name).sanitize()),
                    entries = parameter.enum!!.map { it.content }.toSet(),
                ),
            )

            else -> emptyList()
        }
    }
    val types: List<Definition> = operation.parameters
        ?.map { resolve(it) }
        ?.filter { it.`in` == OpenAPIV2ParameterLocation.BODY }
        ?.flatMap { param ->
            when (val schema = param.schema) {
                is OpenAPIV2Schema -> when (schema.type) {
                    null, OpenAPIV2Type.OBJECT -> flatten(schema, className(name, "RequestBody"))
                    OpenAPIV2Type.ARRAY -> schema.items?.let { flatten(it, className(name, "RequestBody")) }.orEmpty()
                    else -> emptyList()
                }

                else -> emptyList()
            }
        }
        ?: emptyList()

    enums + types
}

private fun OpenAPIV2Model.parseResponseBody(): List<Definition> = flatMapResponses {
    val schema = resolve(response).schema
    val name = operation.toName() ?: (path.toName() + method.name)
    when (schema) {
        is OpenAPIV2Schema -> when (schema.type) {
            null, OpenAPIV2Type.OBJECT -> flatten(schema, className(name, statusCode.value, "ResponseBody"))

            OpenAPIV2Type.ARRAY ->
                schema.items
                    ?.let { flatten(it, className(name, statusCode.value, "ResponseBody")) }
                    .orEmpty()

            else -> emptyList()
        }

        else -> emptyList()
    }
}

private fun OpenAPIV2Model.parseDefinitions(): List<Definition> = definitions.orEmpty()
    .filterIsInstance<String, OpenAPIV2Schema>()
    .filter {
        when (it.value.additionalProperties) {
            is OpenAPIV2Boolean -> true
            is OpenAPIV2Reference -> false
            is OpenAPIV2Schema -> true
            null -> true
        }
    }
    .flatMap { flatten(it.value, className(it.key)) }

private fun OpenAPIV2Model.resolveParameters(operation: OpenAPIV2Operation) = operation.parameters.orEmpty()
    .map {
        when (it) {
            is OpenAPIV2Parameter -> it
            is OpenAPIV2Reference -> resolveParameterObject(it)
        }
    }

private fun OpenAPIV2Model.resolveParameters(itemObject: OpenAPIV2PathItem) = itemObject.parameters.orEmpty()
    .map {
        when (it) {
            is OpenAPIV2Parameter -> it
            is OpenAPIV2Reference -> resolveParameterObject(it)
        }
    }

private fun OpenAPIV2Model.resolveParameterObject(reference: OpenAPIV2Reference) = parameters
    ?.get(reference.getReference())
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV2Model.resolveResponseObject(reference: OpenAPIV2Reference) = responses
    ?.get(reference.getReference())
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV2Model.resolveOpenAPIV2Schema(reference: OpenAPIV2Reference) = definitions
    ?.get(reference.getReference())
    ?: error("Cannot resolve ref: ${reference.ref}")

private tailrec fun OpenAPIV2Model.resolve(schemaOrReference: OpenAPIV2SchemaOrReference): OpenAPIV2Schema = when (schemaOrReference) {
    is OpenAPIV2Schema -> schemaOrReference
    is OpenAPIV2Reference -> resolve(resolveOpenAPIV2Schema(schemaOrReference))
}

private fun OpenAPIV2Model.resolve(schemaOrReferenceOrBoolean: OpenAPIV2SchemaOrReferenceOrBoolean): OpenAPIV2Schema = when (schemaOrReferenceOrBoolean) {
    is OpenAPIV2Schema -> schemaOrReferenceOrBoolean
    is OpenAPIV2Reference -> resolve(resolveOpenAPIV2Schema(schemaOrReferenceOrBoolean))
    is BooleanValue -> TODO("Not yet implemented")
}

private fun OpenAPIV2Model.resolve(responseOrReference: OpenAPIV2ResponseOrReference): OpenAPIV2Response = when (responseOrReference) {
    is OpenAPIV2Response -> responseOrReference
    is OpenAPIV2Reference -> resolveResponseObject(responseOrReference)
}

private fun OpenAPIV2HeaderOrReference.resolve(): OpenAPIV2Header = when (this) {
    is OpenAPIV2Header -> this
    is OpenAPIV2Reference -> error("Headers cannot be referenced in OpenAPI v2")
}

private fun OpenAPIV2Model.resolve(parameterOrReference: OpenAPIV2ParameterOrReference): OpenAPIV2Parameter = when (parameterOrReference) {
    is OpenAPIV2Parameter -> parameterOrReference
    is OpenAPIV2Reference -> resolveParameterObject(parameterOrReference)
}

private fun OpenAPIV2Model.flatten(openAPIV2Schema: OpenAPIV2Schema, name: String): List<Definition> = when {
    // OpenAPI v2 workaround: we sometimes emit `{ allOf: [ { $ref: ... } ], description: ... }`
    // to attach a field-level description to a referenced schema.
    // This wrapper should *not* produce an extra synthetic definition during flattening.
    openAPIV2Schema.allOf?.size == 1 &&
        openAPIV2Schema.allOf!!.first() is OpenAPIV2Reference &&
        openAPIV2Schema.properties == null &&
        openAPIV2Schema.enum == null &&
        openAPIV2Schema.type == null &&
        openAPIV2Schema.items == null &&
        openAPIV2Schema.additionalProperties == null -> emptyList()

    openAPIV2Schema.additionalProperties.exists() -> when (openAPIV2Schema.additionalProperties) {
        is BooleanValue -> emptyList()
        else ->
            openAPIV2Schema.additionalProperties
                ?.let { resolve(it) }
                ?.takeIf { it.properties != null }
                ?.let { flatten(it, name) }
                ?: emptyList()
    }

    openAPIV2Schema.allOf != null -> listOf(
        Type(
            comment = null,
            annotations = openAPIV2Schema.description.toDescriptionAnnotationList(),
            identifier = DefinitionIdentifier(name.sanitize()),
            shape = Type.Shape(
                openAPIV2Schema.allOf
                    .orEmpty()
                    .flatMap {
                        when (it) {
                            is OpenAPIV2Schema -> toField(it, name)
                            is OpenAPIV2Reference -> toField(resolve(resolveOpenAPIV2Schema(it)), it.getReference())
                        }
                    }
                    .distinctBy { it.identifier },
            ),
            extends = emptyList(),
        ),
    ).plus(
        openAPIV2Schema.allOf!!.flatMap {
            when (it) {
                is OpenAPIV2Reference -> emptyList()
                is OpenAPIV2Schema -> it.properties.orEmpty().flatMap { (key, value) ->
                    when (value) {
                        is OpenAPIV2Reference -> emptyList()
                        is OpenAPIV2Schema -> flatten(value, className(name, key))
                    }
                }
            }
        },
    )

    openAPIV2Schema.enum != null ->
        openAPIV2Schema.enum!!
            .map { it.content }
            .toSet()
            .let {
                listOf(
                    Enum(
                        comment = null,
                        annotations = openAPIV2Schema.description.toDescriptionAnnotationList(),
                        identifier = DefinitionIdentifier(name.sanitize()),
                        entries = it,
                    ),
                )
            }

    else -> when (openAPIV2Schema.type) {
        null, OpenAPIV2Type.OBJECT -> {
            val fields = openAPIV2Schema.properties.orEmpty()
                .flatMap { (key, value) -> flatten(value, className(name, key)) }

            val schema = listOf(
                Type(
                    comment = null,
                    annotations = openAPIV2Schema.description.toDescriptionAnnotationList(),
                    identifier = DefinitionIdentifier(name.sanitize()),
                    shape = Type.Shape(toField(openAPIV2Schema, name)),
                    extends = emptyList(),
                ),
            )
            schema + fields
        }

        OpenAPIV2Type.ARRAY -> when (val it = openAPIV2Schema.items) {
            is OpenAPIV2Reference -> emptyList()
            is OpenAPIV2Schema -> flatten(it, className(name, "Array"))
            null -> emptyList()
        }

        else -> emptyList()
    }
}

private fun OpenAPIV2Model.flatten(schemaOrReference: OpenAPIV2SchemaOrReference, name: String): List<Definition> = when (schemaOrReference) {
    is OpenAPIV2Schema -> flatten(schemaOrReference, name)
    is OpenAPIV2Reference -> emptyList()
}

private fun OpenAPIV2Model.toReference(reference: OpenAPIV2Reference, isNullable: Boolean): Reference = resolveOpenAPIV2Schema(reference).let { refOrSchema ->
    val schema = resolve(refOrSchema)
    when {
        schema.additionalProperties.exists() -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanValue -> Reference.Dict(Reference.Any(isNullable = false), isNullable = isNullable)
            is OpenAPIV2Reference -> toReference(additionalProperties, false).toDict(isNullable)
            is OpenAPIV2Schema -> toReference(additionalProperties, reference.getReference(), false).toDict(
                isNullable,
            )
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
            OpenAPIV2Type.ARRAY -> when (val items = schema.items) {
                is OpenAPIV2Reference -> toReference(items, false).toIterable(isNullable)
                is OpenAPIV2Schema -> toReference(
                    items,
                    className(reference.getReference(), "Array"),
                    isNullable,
                ).toIterable(isNullable)

                null -> error("items cannot be null when type is array: ${reference.ref}")
            }

            else -> when (refOrSchema) {
                is OpenAPIV2Schema -> Reference.Custom(className(reference.getReference()).sanitize(), isNullable)
                is OpenAPIV2Reference -> Reference.Custom(
                    className(refOrSchema.getReference()).sanitize(),
                    isNullable,
                )
            }
        }
    }
}

private fun OpenAPIV2Model.toReference(schema: OpenAPIV2Schema, name: String, isNullable: Boolean): Reference = when {
    schema.allOf?.size == 1 &&
        schema.allOf!!.first() is OpenAPIV2Reference &&
        schema.properties == null &&
        schema.enum == null &&
        schema.type == null &&
        schema.items == null &&
        schema.additionalProperties == null -> toReference(schema.allOf!!.first() as OpenAPIV2Reference, isNullable)

    schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
        is BooleanValue -> Reference.Dict(Reference.Any(isNullable = false), isNullable = isNullable)
        is OpenAPIV2Reference -> toReference(additionalProperties, false).toDict(isNullable)
        is OpenAPIV2Schema ->
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

    else -> when (schema.type) {
        OpenAPIV2Type.STRING, OpenAPIV2Type.INTEGER, OpenAPIV2Type.NUMBER, OpenAPIV2Type.BOOLEAN -> {
            if (schema.additionalProperties != null) {
                Reference.Dict(Reference.Primitive(schema.toPrimitive(), isNullable = false), isNullable = isNullable)
            } else {
                Reference.Primitive(schema.toPrimitive(), isNullable = isNullable)
            }
        }

        null, OpenAPIV2Type.OBJECT ->
            when {
                schema.additionalProperties is BooleanValue -> {
                    if (schema.additionalProperties != null) {
                        Reference.Dict(Reference.Any(isNullable = isNullable), isNullable = false)
                    } else {
                        Reference.Any(isNullable = isNullable)
                    }
                }

                else -> {
                    if (schema.additionalProperties != null) {
                        Reference.Dict(
                            Reference.Custom(
                                name.sanitize(),
                                isNullable = isNullable,
                            ),
                            isNullable = false,
                        )
                    }
                    Reference.Custom(name.sanitize(), isNullable = isNullable)
                }
            }

        OpenAPIV2Type.ARRAY -> {
            when (val items = schema.items) {
                is OpenAPIV2Reference -> toReference(items, false).toIterable(isNullable)
                is OpenAPIV2Schema -> toReference(items, name, false).toIterable(isNullable)
                null -> error("When schema is of type array items cannot be null for name: $name")
            }
        }

        OpenAPIV2Type.FILE -> TODO("Type file not implemented")
    }
}

private fun OpenAPIV2PathItem.toOperationList() = Endpoint.Method.entries
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

private fun OpenAPIV2Reference.getReference() = ref.value.split("/")[2]

private fun OpenAPIV2Base.toPrimitive() = when (this.type) {
    OpenAPIV2Type.STRING -> when {
        pattern != null -> Reference.Primitive.Type.String(
            constraint = Reference.Primitive.Type.Constraint.RegExp(
                pattern!!,
            ),
        )

        else -> Reference.Primitive.Type.String(null)
    }

    OpenAPIV2Type.INTEGER -> Reference.Primitive.Type.Integer(
        if (format == "int32") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64,
        null,
    )

    OpenAPIV2Type.NUMBER -> Reference.Primitive.Type.Number(
        if (format == "float") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64,
        null,
    )

    OpenAPIV2Type.BOOLEAN -> Reference.Primitive.Type.Boolean
    else -> error("Type is not a primitive")
}

private fun OpenAPIV2Model.toField(header: OpenAPIV2Header, identifier: String) = Field(
    identifier = FieldIdentifier(identifier),
    annotations = header.description.toDescriptionAnnotationList(),
    reference = when (header.type) {
        OpenAPIV2Type.ARRAY -> header.items?.let { resolve(it) }?.let { toReference(it, identifier, false) }
            ?: error("Item cannot be null")

        else -> Reference.Primitive(
            header.toPrimitive(),
            isNullable = false,
        )
    },
)

private fun OpenAPIV2Model.toField(schema: OpenAPIV2Schema, name: String) = schema.properties.orEmpty().map { (key, value) ->
    val isNullable = !(schema.required?.contains(key) ?: false)
    when (value) {
        is OpenAPIV2Schema -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = emptyList(),
                reference = when {
                    value.enum != null -> toReference(value, className(name, key), isNullable)
                    value.type == OpenAPIV2Type.ARRAY -> toReference(
                        value,
                        className(name, key, "Array"),
                        isNullable,
                    )

                    else -> toReference(value, className(name, key), isNullable)
                },
            )
        }

        is OpenAPIV2Reference -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = emptyList(),
                reference = toReference(value, isNullable),
            )
        }
    }
}

private fun OpenAPIV2Model.toField(parameter: OpenAPIV2Parameter, name: String) = resolve(parameter)
    .let { schema ->
        val isNullable = !(parameter.required ?: false)
        when {
            parameter.enum != null -> Reference.Custom(
                className(name, "Parameter", schema.name).sanitize(),
                isNullable = isNullable,
            )

            else -> when (schema.type) {
                OpenAPIV2Type.STRING, OpenAPIV2Type.NUMBER, OpenAPIV2Type.INTEGER, OpenAPIV2Type.BOOLEAN ->
                    schema
                        .toPrimitive()
                        .let { primitive -> Reference.Primitive(primitive, isNullable = isNullable) }

                OpenAPIV2Type.ARRAY -> schema.items?.let { items -> resolve(items) }
                    ?.toPrimitive()
                    ?.let { primitive ->
                        Reference.Iterable(
                            Reference.Primitive(primitive, false),
                            isNullable = isNullable,
                        )
                    }
                    ?: TODO("Not yet implemented")

                OpenAPIV2Type.OBJECT -> TODO("Not yet implemented")
                OpenAPIV2Type.FILE -> TODO("Not yet implemented")
                null -> TODO("Not yet implemented")
            }
        }
    }.let {
        Field(
            identifier = FieldIdentifier(parameter.name),
            annotations = parameter.description.toDescriptionAnnotationList(),
            reference = it,
        )
    }

private fun Path.toSegments(parameters: List<OpenAPIV2Parameter>) = value.split("/").drop(1).map { segment ->
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

private fun OpenAPIV2Operation.toName() = operationId?.let { className(it) }

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
    val pathItem: OpenAPIV2PathItem,
    val method: Endpoint.Method,
    val operation: OpenAPIV2Operation,
    val type: String,
)

private fun OpenAPIV2Model.flatMapRequests(f: FlattenRequest.() -> List<Definition>) = paths
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList()
            .flatMap { (method, operation) ->
                (consumes ?: operation.consumes ?: listOf(APPLICATION_JSON)).map { type ->
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
    val pathItem: OpenAPIV2PathItem,
    val method: Endpoint.Method,
    val operation: OpenAPIV2Operation,
    val statusCode: StatusCode,
    val response: OpenAPIV2ResponseOrReference,
    val type: String,
)

private fun OpenAPIV2Model.flatMapResponses(f: FlattenResponse.() -> List<Definition>) = paths
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList()
            .flatMap { (method, operation) ->
                operation.responses.orEmpty()
                    .flatMap { (statusCode, response) ->
                        (produces ?: operation.produces ?: listOf(APPLICATION_JSON)).map { type ->
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

private fun String.sanitize() = this
    .split(".", " ", "-")
    .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
    .joinToString("")
    .asSequence()
    .filter { it.isLetterOrDigit() || it in listOf('_') }
    .joinToString("")

private fun OpenAPIV2Type?.isPrimitive() = when (this) {
    OpenAPIV2Type.STRING -> true
    OpenAPIV2Type.NUMBER -> true
    OpenAPIV2Type.INTEGER -> true
    OpenAPIV2Type.BOOLEAN -> true
    OpenAPIV2Type.ARRAY -> false
    OpenAPIV2Type.OBJECT -> false
    OpenAPIV2Type.FILE -> false
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

private fun OpenAPIV2SchemaOrReferenceOrBoolean?.exists() = when (this) {
    is OpenAPIV2SchemaOrReference -> true
    is BooleanValue -> this.value
    is OpenAPIV2Reference -> true
    else -> false
}
