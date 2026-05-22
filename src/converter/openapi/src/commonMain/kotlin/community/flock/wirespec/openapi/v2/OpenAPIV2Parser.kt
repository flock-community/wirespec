package community.flock.wirespec.openapi.v2

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.kotlinx.openapi.bindings.BooleanValue
import community.flock.kotlinx.openapi.bindings.Header
import community.flock.kotlinx.openapi.bindings.HeaderOrReference
import community.flock.kotlinx.openapi.bindings.OpenAPIV2
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Base
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Header
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Operation
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Parameter
import community.flock.kotlinx.openapi.bindings.OpenAPIV20ParameterLocation
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Response
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Schema
import community.flock.kotlinx.openapi.bindings.OpenAPIV20Type
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Model
import community.flock.kotlinx.openapi.bindings.Parameter
import community.flock.kotlinx.openapi.bindings.ParameterOrReference
import community.flock.kotlinx.openapi.bindings.Path
import community.flock.kotlinx.openapi.bindings.Response
import community.flock.kotlinx.openapi.bindings.ResponseOrReference
import community.flock.kotlinx.openapi.bindings.Schema
import community.flock.kotlinx.openapi.bindings.SchemaOrReference
import community.flock.kotlinx.openapi.bindings.SchemaOrReferenceOrBoolean
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.openapi.common.APPLICATION_JSON
import community.flock.wirespec.openapi.common.className
import community.flock.wirespec.openapi.common.flatMapRequests
import community.flock.wirespec.openapi.common.flatMapResponses
import community.flock.wirespec.openapi.common.getReference
import community.flock.wirespec.openapi.common.jsonDefault
import community.flock.wirespec.openapi.common.parseOpenApi
import community.flock.wirespec.openapi.common.resolveEndpointNameCollisions
import community.flock.wirespec.openapi.common.sanitize
import community.flock.wirespec.openapi.common.toDescriptionAnnotationList
import community.flock.wirespec.openapi.common.toDict
import community.flock.wirespec.openapi.common.toIterable
import community.flock.wirespec.openapi.common.toName
import community.flock.wirespec.openapi.common.toOperationList
import community.flock.kotlinx.openapi.bindings.Reference as OpenAPIReference

object OpenAPIV2Parser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = parseOpenApi(moduleContent) {
        OpenAPIV2(jsonDefault(strict))
            .decodeFromString(it)
            .parse()
    }

    fun OpenAPIV2Model.parse(): NonEmptyList<Definition> = listOf(
        parseEndpoints(),
        parseParameters(),
        parseRequestBody(),
        parseResponseBody(),
        parseDefinitions(),
    ).reduce(List<Definition>::plus)
        .resolveEndpointNameCollisions()
        .toNonEmptyListOrNull()
        .let { requireNotNull(it) { "Cannot yield empty AST for OpenAPI v2" } }
}

private fun OpenAPIV2Model.parseEndpoints(): List<Definition> = paths.orEmpty()
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList()
            .flatMap { (method, operation) ->
                val parameters = resolveParameters(pathItem.parameters) + resolveParameters(operation.parameters)
                val segments = path.toSegments(parameters)
                val name = operation.toName() ?: (path.toName() + method.name)
                val query = parameters
                    .filter { it.location == OpenAPIV20ParameterLocation.QUERY }
                    .map { toField(it, name) }
                val headers = parameters
                    .filter { it.location == OpenAPIV20ParameterLocation.HEADER }
                    .map { toField(it, name) }
                val operationConsumes = (operation as? OpenAPIV20Operation)?.consumes
                val operationProduces = (operation as? OpenAPIV20Operation)?.produces
                val requests = parameters
                    .filter { it.location == OpenAPIV20ParameterLocation.BODY }
                    .flatMap { requestBody ->
                        (consumes.orEmpty() + operationConsumes.orEmpty())
                            .distinct()
                            .ifEmpty { listOf(APPLICATION_JSON) }
                            .map { type ->
                                val isNullable = false
                                Endpoint.Request(
                                    Endpoint.Content(
                                        type = type,
                                        reference = when (val schema = requestBody.schema) {
                                            is OpenAPIReference -> toReference(schema, isNullable)
                                            is Schema -> toReference(
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
                    .mapValues { resolveResponse(it.value) }
                    .flatMap { (status, res) ->
                        (produces.orEmpty() + operationProduces.orEmpty())
                            .distinct()
                            .ifEmpty { listOf(APPLICATION_JSON) }.map { type ->
                                Endpoint.Response(
                                    annotations = res.description.toDescriptionAnnotationList(),
                                    status = status.value,
                                    headers = res.headers
                                        ?.map { (identifier, header) -> toField(header.resolveHeader(), identifier) }
                                        .orEmpty(),
                                    content = (res as? OpenAPIV20Response)?.schema?.let { schema ->
                                        Endpoint.Content(
                                            type = type,
                                            reference = when (schema) {
                                                is OpenAPIReference -> toReference(schema, false)
                                                is Schema -> toReference(
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
    val parameters = resolveParameters(pathItem.parameters) + resolveParameters(operation.parameters)
    val name = operation.toName() ?: (path.toName() + method.name)
    parameters
        .filter { it.location != OpenAPIV20ParameterLocation.BODY }
        .flatMap { parameter ->
            parameter.schema?.let { flattenSchemaOrRef(it, className(name, "Parameter", parameter.name)) } ?: emptyList()
        }
}

private fun OpenAPIV2Model.parseRequestBody(): List<Definition> = flatMapRequests {
    val parameters = resolveParameters(pathItem.parameters) + resolveParameters(operation.parameters)
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
        ?.map { resolveParameter(it) }
        ?.filter { it.location == OpenAPIV20ParameterLocation.BODY }
        ?.flatMap { param ->
            when (val schema = param.schema) {
                is Schema -> when (schema.primitiveType) {
                    null, OpenAPIV20Type.OBJECT -> flatten(schema, className(name, "RequestBody"))
                    OpenAPIV20Type.ARRAY -> schema.items?.let { flattenSchemaOrRef(it, className(name, "RequestBody")) }.orEmpty()
                    else -> emptyList()
                }

                else -> emptyList()
            }
        }
        ?: emptyList()

    enums + types
}

private fun OpenAPIV2Model.parseResponseBody(): List<Definition> = flatMapResponses {
    val schema = (resolveResponse(response) as? OpenAPIV20Response)?.schema
    val name = operation.toName() ?: (path.toName() + method.name)
    when (schema) {
        is Schema -> when (schema.primitiveType) {
            null, OpenAPIV20Type.OBJECT -> flatten(schema, className(name, statusCode.value, "ResponseBody"))

            OpenAPIV20Type.ARRAY ->
                schema.items
                    ?.let { flattenSchemaOrRef(it, className(name, statusCode.value, "ResponseBody")) }
                    .orEmpty()

            else -> emptyList()
        }

        else -> emptyList()
    }
}

private fun OpenAPIV2Model.parseDefinitions(): List<Definition> = definitions.orEmpty()
    .mapNotNull { (key, value) -> (value as? Schema)?.let { key to it } }
    .filter {
        when (it.second.additionalProperties) {
            is BooleanValue -> true
            is OpenAPIReference -> false
            is Schema -> true
            null -> true
        }
    }
    .flatMap { flatten(it.second, className(it.first)) }

private fun OpenAPIV2Model.resolveParameters(parameters: List<ParameterOrReference>?): List<OpenAPIV20Parameter> = parameters.orEmpty()
    .mapNotNull {
        when (it) {
            is Parameter -> it as? OpenAPIV20Parameter
            is OpenAPIReference -> resolveParameterObject(it)
        }
    }

private fun OpenAPIV2Model.resolveParameterObject(reference: OpenAPIReference): OpenAPIV20Parameter = parameters
    ?.get(reference.getReference())
    ?.let { it as? OpenAPIV20Parameter }
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV2Model.resolveResponseObject(reference: OpenAPIReference): Response = responses
    ?.get(reference.getReference())
    ?: error("Cannot resolve ref: ${reference.ref}")

private fun OpenAPIV2Model.resolveSchemaRef(reference: OpenAPIReference): Pair<OpenAPIReference, Schema> = (
    definitions?.get(reference.getReference())
        ?: error("Cannot resolve ref: ${reference.ref}")
    ).let {
    when (it) {
        is Schema -> reference to it
        is OpenAPIReference -> resolveSchemaRef(it)
    }
}

private fun OpenAPIV2Model.resolve(schemaOrReference: SchemaOrReference): Schema = when (schemaOrReference) {
    is Schema -> schemaOrReference
    is OpenAPIReference -> resolveSchemaRef(schemaOrReference).second
}

private fun OpenAPIV2Model.resolve(schemaOrReferenceOrBoolean: SchemaOrReferenceOrBoolean): Schema = when (schemaOrReferenceOrBoolean) {
    is Schema -> schemaOrReferenceOrBoolean
    is OpenAPIReference -> resolveSchemaRef(schemaOrReferenceOrBoolean).second
    is BooleanValue -> TODO("Not yet implemented")
}

private fun OpenAPIV2Model.resolveResponse(responseOrReference: ResponseOrReference): Response = when (responseOrReference) {
    is Response -> responseOrReference
    is OpenAPIReference -> resolveResponseObject(responseOrReference)
}

private fun HeaderOrReference.resolveHeader(): Header = when (this) {
    is Header -> this
    is OpenAPIReference -> error("Headers cannot be referenced in OpenAPI v2")
}

private fun OpenAPIV2Model.resolveParameter(parameterOrReference: ParameterOrReference): OpenAPIV20Parameter = when (parameterOrReference) {
    is Parameter -> parameterOrReference as? OpenAPIV20Parameter ?: error("Unexpected parameter type: $parameterOrReference")
    is OpenAPIReference -> resolveParameterObject(parameterOrReference)
}

private fun OpenAPIV2Model.flatten(schemaObject: Schema, name: String): List<Definition> = when {
    schemaObject.additionalProperties.exists() -> when (schemaObject.additionalProperties) {
        is BooleanValue -> emptyList()
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
            annotations = schemaObject.description.toDescriptionAnnotationList(),
            identifier = DefinitionIdentifier(name.sanitize()),
            shape = Type.Shape(
                schemaObject.allOf
                    .orEmpty()
                    .flatMap {
                        when (it) {
                            is Schema -> toField(it, name)
                            is OpenAPIReference -> toField(resolveSchemaRef(it).second, it.getReference())
                        }
                    }
                    .distinctBy { it.identifier },
            ),
            extends = emptyList(),
        ),
    ).plus(
        schemaObject.allOf!!.flatMap {
            when (it) {
                is OpenAPIReference -> emptyList()
                is Schema -> it.properties.orEmpty().flatMap { (key, value) ->
                    when (value) {
                        is OpenAPIReference -> emptyList()
                        is Schema -> flatten(value, className(name, key))
                    }
                }
            }
        },
    )

    schemaObject.enum != null ->
        schemaObject.enum!!
            .map { it.content }
            .toSet()
            .let {
                listOf(
                    Enum(
                        comment = null,
                        annotations = schemaObject.description.toDescriptionAnnotationList(),
                        identifier = DefinitionIdentifier(name.sanitize()),
                        entries = it,
                    ),
                )
            }

    else -> when (schemaObject.primitiveType) {
        null, OpenAPIV20Type.OBJECT -> {
            val fields = schemaObject.properties.orEmpty()
                .flatMap { (key, value) -> flattenSchemaOrRef(value, className(name, key)) }

            val schema = listOf(
                Type(
                    comment = null,
                    annotations = schemaObject.description.toDescriptionAnnotationList(),
                    identifier = DefinitionIdentifier(name.sanitize()),
                    shape = Type.Shape(toField(schemaObject, name)),
                    extends = emptyList(),
                ),
            )
            schema + fields
        }

        OpenAPIV20Type.ARRAY -> when (val it = schemaObject.items) {
            is OpenAPIReference -> emptyList()
            is Schema -> flatten(it, className(name, "Array"))
            null -> emptyList()
        }

        else -> emptyList()
    }
}

private fun OpenAPIV2Model.flattenSchemaOrRef(schemaOrReference: SchemaOrReference, name: String): List<Definition> = when (schemaOrReference) {
    is Schema -> flatten(schemaOrReference, name)
    is OpenAPIReference -> emptyList()
}

private fun OpenAPIV2Model.toReference(reference: OpenAPIReference, isNullable: Boolean): Reference {
    val (referencingObject, schema) = resolveSchemaRef(reference)
    return when {
        schema.additionalProperties.exists() -> when (val additionalProperties = schema.additionalProperties!!) {
            is BooleanValue -> Reference.Dict(Reference.Any(isNullable = false), isNullable = isNullable)
            is OpenAPIReference -> toReference(additionalProperties, false).toDict(isNullable)
            is Schema -> toReference(additionalProperties, reference.getReference(), false).toDict(
                isNullable,
            )
        }

        schema.enum != null -> Reference.Custom(
            className(referencingObject.getReference()).sanitize(),
            isNullable = isNullable,
        )

        schema.primitiveType.isPrimitive() -> Reference.Primitive(
            schema.toPrimitive(),
            isNullable = isNullable,
        )

        else -> when (schema.primitiveType) {
            OpenAPIV20Type.ARRAY -> when (val items = schema.items) {
                is OpenAPIReference -> toReference(items, false).toIterable(isNullable)
                is Schema -> toReference(
                    items,
                    className(referencingObject.getReference(), "Array"),
                    isNullable,
                ).toIterable(isNullable)

                null -> error("items cannot be null when type is array: ${reference.ref}")
            }

            else -> Reference.Custom(className(referencingObject.getReference()).sanitize(), isNullable)
        }
    }
}

private fun OpenAPIV2Model.toReference(schema: Schema, name: String, isNullable: Boolean): Reference = when {
    schema.allOf?.size == 1 &&
        schema.allOf!!.first() is OpenAPIReference &&
        schema.properties == null &&
        schema.enum == null &&
        schema.primitiveType == null &&
        schema.items == null &&
        schema.additionalProperties == null -> toReference(schema.allOf!!.first() as OpenAPIReference, isNullable)

    schema.additionalProperties != null -> when (val additionalProperties = schema.additionalProperties!!) {
        is BooleanValue -> Reference.Dict(Reference.Any(isNullable = false), isNullable = isNullable)
        is OpenAPIReference -> toReference(additionalProperties, false).toDict(isNullable)
        is Schema ->
            additionalProperties
                .takeIf { it.primitiveType.isPrimitive() || it.properties != null }
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

    else -> when (schema.primitiveType) {
        OpenAPIV20Type.STRING, OpenAPIV20Type.INTEGER, OpenAPIV20Type.NUMBER, OpenAPIV20Type.BOOLEAN -> {
            if (schema.additionalProperties != null) {
                Reference.Dict(Reference.Primitive(schema.toPrimitive(), isNullable = false), isNullable = isNullable)
            } else {
                Reference.Primitive(schema.toPrimitive(), isNullable = isNullable)
            }
        }

        null, OpenAPIV20Type.OBJECT ->
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

        OpenAPIV20Type.ARRAY -> {
            when (val items = schema.items) {
                is OpenAPIReference -> toReference(items, false).toIterable(isNullable)
                is Schema -> toReference(items, name, false).toIterable(isNullable)
                null -> error("When schema is of type array items cannot be null for name: $name")
            }
        }

        OpenAPIV20Type.FILE -> TODO("Type file not implemented")
    }
}

private fun OpenAPIV20Base.toPrimitive() = when (this.type) {
    OpenAPIV20Type.STRING -> when {
        pattern != null -> Reference.Primitive.Type.String(
            constraint = Reference.Primitive.Type.Constraint.RegExp(
                pattern!!,
            ),
        )
        format == "binary" -> Reference.Primitive.Type.Bytes
        else -> Reference.Primitive.Type.String(null)
    }

    OpenAPIV20Type.INTEGER -> Reference.Primitive.Type.Integer(
        if (format == "int32") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64,
        null,
    )

    OpenAPIV20Type.NUMBER -> Reference.Primitive.Type.Number(
        if (format == "float") Reference.Primitive.Type.Precision.P32 else Reference.Primitive.Type.Precision.P64,
        null,
    )

    OpenAPIV20Type.BOOLEAN -> Reference.Primitive.Type.Boolean
    else -> error("Type is not a primitive")
}

private fun Schema.toPrimitive(): Reference.Primitive.Type = (this as OpenAPIV20Base).toPrimitive()

private fun OpenAPIV2Model.toField(header: Header, identifier: String): Field {
    val v20 = header as? OpenAPIV20Header ?: error("Unexpected header type: $header")
    return Field(
        identifier = FieldIdentifier(identifier),
        annotations = header.description.toDescriptionAnnotationList(),
        reference = when (v20.type) {
            OpenAPIV20Type.ARRAY -> v20.items?.let { resolve(it) }?.let { toReference(it, identifier, false) }
                ?: error("Item cannot be null")

            else -> Reference.Primitive(
                v20.toPrimitive(),
                isNullable = false,
            )
        },
    )
}

private fun OpenAPIV2Model.toField(schema: Schema, name: String) = schema.properties.orEmpty().map { (key, value) ->
    val isNullable = !(schema.required?.contains(key) ?: false)
    when (value) {
        is Schema -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = emptyList(),
                reference = when {
                    value.enum != null -> toReference(value, className(name, key), isNullable)
                    value.primitiveType == OpenAPIV20Type.ARRAY -> toReference(
                        value,
                        className(name, key, "Array"),
                        isNullable,
                    )

                    else -> toReference(value, className(name, key), isNullable)
                },
            )
        }

        is OpenAPIReference -> {
            Field(
                identifier = FieldIdentifier(key),
                annotations = emptyList(),
                reference = toReference(value, isNullable),
            )
        }
    }
}

private fun OpenAPIV2Model.toField(parameter: OpenAPIV20Parameter, name: String) = resolveParameter(parameter)
    .let { schema ->
        val isNullable = !(parameter.required ?: false)
        when {
            parameter.enum != null -> Reference.Custom(
                className(name, "Parameter", schema.name).sanitize(),
                isNullable = isNullable,
            )

            else -> when (schema.type) {
                OpenAPIV20Type.STRING, OpenAPIV20Type.NUMBER, OpenAPIV20Type.INTEGER, OpenAPIV20Type.BOOLEAN ->
                    schema
                        .toPrimitive()
                        .let { primitive -> Reference.Primitive(primitive, isNullable = isNullable) }

                OpenAPIV20Type.ARRAY -> schema.items?.let { items -> resolve(items) }
                    ?.toPrimitive()
                    ?.let { primitive ->
                        Reference.Iterable(
                            Reference.Primitive(primitive, false),
                            isNullable = isNullable,
                        )
                    }
                    ?: TODO("Not yet implemented")

                OpenAPIV20Type.OBJECT -> TODO("Not yet implemented")
                OpenAPIV20Type.FILE -> TODO("Not yet implemented")
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

private fun Path.toSegments(parameters: List<OpenAPIV20Parameter>) = value.split("/").drop(1).map { segment ->
    val isParam = segment.isNotEmpty() && segment[0] == '{' && segment[segment.length - 1] == '}'
    when (isParam) {
        true -> {
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

        false -> Endpoint.Segment.Literal(segment)
    }
}

private fun OpenAPIV20Type?.isPrimitive() = when (this) {
    OpenAPIV20Type.STRING -> true
    OpenAPIV20Type.NUMBER -> true
    OpenAPIV20Type.INTEGER -> true
    OpenAPIV20Type.BOOLEAN -> true
    OpenAPIV20Type.ARRAY -> false
    OpenAPIV20Type.OBJECT -> false
    OpenAPIV20Type.FILE -> false
    null -> false
}

private val Schema.primitiveType: OpenAPIV20Type? get() = (this as? OpenAPIV20Schema)?.type

private val OpenAPIV20Parameter.location: OpenAPIV20ParameterLocation get() = `in`

private fun SchemaOrReferenceOrBoolean?.exists() = when (this) {
    is Schema -> true
    is BooleanValue -> this.value
    is OpenAPIReference -> true
    null -> false
}
