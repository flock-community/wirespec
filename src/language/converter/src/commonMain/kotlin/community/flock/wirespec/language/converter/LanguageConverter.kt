package community.flock.wirespec.language.converter

import community.flock.wirespec.language.core.AnonymousClass
import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.BinaryOp
import community.flock.wirespec.language.core.Call
import community.flock.wirespec.language.core.Case
import community.flock.wirespec.language.core.ClassLiteral
import community.flock.wirespec.language.core.Constructor
import community.flock.wirespec.language.core.ConstructorStatement
import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.Enum
import community.flock.wirespec.language.core.EnumReference
import community.flock.wirespec.language.core.ErrorStatement
import community.flock.wirespec.language.core.Expression
import community.flock.wirespec.language.core.Field
import community.flock.wirespec.language.core.Function
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Literal
import community.flock.wirespec.language.core.LiteralList
import community.flock.wirespec.language.core.LiteralMap
import community.flock.wirespec.language.core.MethodCall
import community.flock.wirespec.language.core.NullLiteral
import community.flock.wirespec.language.core.Parameter
import community.flock.wirespec.language.core.Precision
import community.flock.wirespec.language.core.PropertyAccess
import community.flock.wirespec.language.core.ReturnStatement
import community.flock.wirespec.language.core.StaticCall
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Switch
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.Union
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.compiler.core.parse.ast.Channel as ChannelWirespec
import community.flock.wirespec.compiler.core.parse.ast.Definition as DefinitionWirespec
import community.flock.wirespec.compiler.core.parse.ast.Endpoint as EndpointWirespec
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumWirespec
import community.flock.wirespec.compiler.core.parse.ast.Reference as ReferenceWirespec
import community.flock.wirespec.compiler.core.parse.ast.Refined as RefinedWirespec
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeWirespec
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionWirespec

fun DefinitionWirespec.convert(): Element = when (this) {
    is TypeWirespec -> convert()
    is EnumWirespec -> convert()
    is UnionWirespec -> convert()
    is RefinedWirespec -> convert()
    is ChannelWirespec -> convert()
    is EndpointWirespec -> convert()
    else -> error("Conversion not implemented for ${this::class.simpleName}")
}

fun TypeWirespec.convert() = Struct(
    name = this.identifier.value,
    fields = this.shape.value.map {
        Field(
            name = it.identifier.value,
            type = it.reference.convert(),
        )
    },
    interfaces = extends.map { it.convert() }.filterIsInstance<Type.Custom>(),
)

fun EnumWirespec.convert() = Enum(
    name = this.identifier.value,
    extends = Type.Custom("Wirespec.Enum"),
    entries = this.entries.map { Enum.Entry(it, emptyList()) },
)

fun UnionWirespec.convert() = Union(
    name = this.identifier.value,
    members = this.entries.map { it.convert() }.filterIsInstance<Type.Custom>().map { it.name },
)

fun RefinedWirespec.convert() = Struct(
    name = this.identifier.value,
    fields = listOf(
        Field(
            name = "value",
            type = this.reference.convert(),
        ),
    ),
    interfaces = listOf(Type.Custom("Wirespec.Refined")),
)

fun ChannelWirespec.convert() = Interface(
    name = identifier.value,
    extends = Type.Custom("Wirespec.Channel"),
    elements = listOf(
        Function(
            name = "invoke",
            parameters = listOf(
                Parameter(
                    name = "message",
                    type = reference.convert(),
                ),
            ),
            returnType = Type.Unit,
            body = emptyList(),
            isAsync = false,
            isStatic = false,
            isOverride = false,
        ),
    ),
)

fun EndpointWirespec.
        convert(): Interface {
    val pathParams = path.filterIsInstance<EndpointWirespec.Segment.Param>()
    val requestContent = requests.first().content
    val requestBodyType = requestContent?.reference?.convert() ?: Type.Unit

    return Interface(
        name = identifier.value,
        extends = Type.Custom("Wirespec.Endpoint"),
        elements = buildList {
            // Path record
            add(
                Struct(
                    name = "Path",
                    fields = pathParams.map { Field(it.identifier.value.sanitizeForJava(), it.reference.convert()) },
                    interfaces = listOf(Type.Custom("Wirespec.Path")),
                )
            )

            // Queries record
            add(
                Struct(
                    name = "Queries",
                    fields = queries.map { Field(it.identifier.value.sanitizeForJava(), it.reference.convert()) },
                    interfaces = listOf(Type.Custom("Wirespec.Queries")),
                )
            )

            // RequestHeaders record
            add(
                Struct(
                    name = "RequestHeaders",
                    fields = headers.map { Field(it.identifier.value.sanitizeForJava(), it.reference.convert()) },
                    interfaces = listOf(Type.Custom("Wirespec.Request.Headers")),
                )
            )

            // Request record
            add(
                Struct(
                    name = "Request",
                    fields = listOf(
                        Field("path", Type.Custom("Path")),
                        Field("method", Type.Custom("Wirespec.Method")),
                        Field("queries", Type.Custom("Queries")),
                        Field("headers", Type.Custom("RequestHeaders")),
                        Field("body", requestBodyType),
                    ),
                    interfaces = listOf(Type.Custom("Wirespec.Request", listOf(requestBodyType))),
                    constructors = listOf(
                        Constructor(
                            parameters = pathParams.map { Parameter(it.identifier.value.sanitizeForJava(), it.reference.convert()) } +
                                queries.map { Parameter(it.identifier.value.sanitizeForJava(), it.reference.convert()) } +
                                headers.map { Parameter(it.identifier.value.sanitizeForJava(), it.reference.convert()) } +
                                listOfNotNull(requestContent?.let { Parameter("body", it.reference.convert()) }),
                            body = listOf(
                                Assignment(
                                    "path",
                                    ConstructorStatement(
                                        type = Type.Custom("Path"),
                                        namedArguments = pathParams.associate { it.identifier.value.sanitizeForJava() to VariableReference(it.identifier.value.sanitizeForJava()) },
                                    )
                                ),
                                Assignment("method", EnumReference(Type.Custom("Wirespec.Method"), method.name)),
                                Assignment(
                                    "queries",
                                    ConstructorStatement(
                                        type = Type.Custom("Queries"),
                                        namedArguments = queries.associate { it.identifier.value.sanitizeForJava() to VariableReference(it.identifier.value.sanitizeForJava()) },
                                    )
                                ),
                                Assignment(
                                    "headers",
                                    ConstructorStatement(
                                        type = Type.Custom("RequestHeaders"),
                                        namedArguments = headers.associate { it.identifier.value.sanitizeForJava() to VariableReference(it.identifier.value.sanitizeForJava()) },
                                    )
                                ),
                                Assignment("body", if (requestContent != null) VariableReference("body") else NullLiteral),
                            ),
                        )
                    ),
                )
            )

            // Response sealed interface
            add(
                Interface(
                    name = "Response",
                    extends = Type.Custom("Wirespec.Response", listOf(Type.Custom("T"))),
                    elements = emptyList(),
                    isSealed = true,
                    typeParameters = listOf("T"),
                )
            )

            // Response status interfaces (Response2XX, Response5XX, etc.)
            addAll(
                responses.map { it.status.first() }.distinct().map { statusPrefix ->
                    Interface(
                        name = "Response${statusPrefix}XX",
                        extends = Type.Custom("Response", listOf(Type.Custom("T"))),
                        elements = emptyList(),
                        isSealed = true,
                        typeParameters = listOf("T"),
                    )
                }
            )

            // Response content type interfaces (ResponseVoid, ResponseTodoDto, etc.)
            addAll(
                responses.distinctBy { it.status }
                    .map { it.content?.reference }
                    .distinct()
                    .map { ref ->
                        val contentType = ref?.convert() ?: Type.Unit
                        val typeName = contentType.toTypeName()
                        Interface(
                            name = "Response$typeName",
                            extends = Type.Custom("Response", listOf(contentType)),
                            elements = emptyList(),
                            isSealed = true,
                        )
                    }
            )

            // Individual response records (Response200, Response201, etc.)
            addAll(
                responses.distinctBy { it.status }.map { response ->
                    val bodyType = response.content?.reference?.convert() ?: Type.Unit
                    val typeName = bodyType.toTypeName()
                    val statusCode = response.status.toIntOrNull() ?: 0
                    val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                    Struct(
                        name = "Response$statusClassName",
                        fields = listOf(
                            Field("status", Type.Integer(Precision.P32)),
                            Field("headers", Type.Custom("Headers")),
                            Field("body", bodyType),
                        ),
                        interfaces = listOf(
                            Type.Custom("Response${response.status.first()}XX", listOf(bodyType)),
                            Type.Custom("Response$typeName"),
                        ),
                        elements = listOf(
                            Struct(
                                name = "Headers",
                                fields = response.headers.map { Field(it.identifier.value.sanitizeForJava(), it.reference.convert()) },
                                interfaces = listOf(Type.Custom("Wirespec.Response.Headers")),
                            ),
                        ),
                        constructors = listOf(
                            Constructor(
                                parameters = response.headers.map { Parameter(it.identifier.value.sanitizeForJava(), it.reference.convert()) } +
                                    listOfNotNull(response.content?.let { Parameter("body", it.reference.convert()) }),
                                body = listOf(
                                    Assignment("status", Literal(statusCode, Type.Integer(Precision.P32))),
                                    Assignment(
                                        "headers",
                                        ConstructorStatement(
                                            type = Type.Custom("Headers"),
                                            namedArguments = response.headers.associate { it.identifier.value.sanitizeForJava() to VariableReference(it.identifier.value.sanitizeForJava()) },
                                        )
                                    ),
                                    Assignment("body", if (response.content != null) VariableReference("body") else NullLiteral),
                                ),
                            )
                        ),
                    )
                }
            )

            // Handler interface
            add(
                Interface(
                    name = "Handler",
                    extends = Type.Custom("Wirespec.Handler"),
                    elements = listOf(
                        convertToRequest(),
                        convertFromRequest(),
                        convertToResponse(),
                        convertFromResponse(),
                        Function(
                            name = identifier.value.replaceFirstChar { it.lowercase() },
                            parameters = listOf(Parameter("request", Type.Custom("Request"))),
                            returnType = Type.Custom(
                                "java.util.concurrent.CompletableFuture",
                                listOf(Type.Custom("Response", listOf(Type.Custom("?"))))
                            ),
                            body = emptyList(),
                            isAsync = false,
                            isStatic = false,
                            isOverride = false,
                        ),
                        convertHandlers(),
                    ),
                )
            )
        },
    )
}

private fun Type.toTypeName(): String = when (this) {
    is Type.Unit -> "Void"
    is Type.Custom -> name
    is Type.Array -> "List${elementType.toTypeName()}"
    is Type.Nullable -> "Optional${type.toTypeName()}"
    is Type.String -> "String"
    is Type.Integer -> "Integer"
    is Type.Number -> "Number"
    is Type.Boolean -> "Boolean"
    is Type.Bytes -> "Bytes"
    is Type.Dict -> "Map"
}

fun ReferenceWirespec.convert(): Type = when (this) {
    is ReferenceWirespec.Any -> TODO("Any is not implemented yet")
    is ReferenceWirespec.Custom -> Type.Custom(value)
    is ReferenceWirespec.Dict -> Type.Dict(Type.String, reference.convert())
    is ReferenceWirespec.Iterable -> Type.Array(reference.convert())
    is ReferenceWirespec.Primitive -> when (val t = type) {
        ReferenceWirespec.Primitive.Type.Boolean -> Type.Boolean
        ReferenceWirespec.Primitive.Type.Bytes -> Type.Bytes
        is ReferenceWirespec.Primitive.Type.Integer -> when (t.precision) {
            ReferenceWirespec.Primitive.Type.Precision.P32 -> Type.Integer(Precision.P32)
            ReferenceWirespec.Primitive.Type.Precision.P64 -> Type.Integer(Precision.P64)
        }

        is ReferenceWirespec.Primitive.Type.Number -> when (t.precision) {
            ReferenceWirespec.Primitive.Type.Precision.P32 -> Type.Number(Precision.P32)
            ReferenceWirespec.Primitive.Type.Precision.P64 -> Type.Number(Precision.P64)
        }

        is ReferenceWirespec.Primitive.Type.String -> Type.String
    }

    is ReferenceWirespec.Unit -> Type.Unit
}
    .let { if (isNullable) Type.Nullable(it) else it }

fun EndpointWirespec.convertHandlers(): Struct {
    val pathTemplate = "/" + path.joinToString("/") {
        when (it) {
            is EndpointWirespec.Segment.Literal -> it.value
            is EndpointWirespec.Segment.Param -> "{${it.identifier.value}}"
        }
    }

    return Struct(
        name = "Handlers",
        fields = emptyList(),
        interfaces = listOf(
            Type.Custom("Wirespec.Server", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Custom("?"))))),
            Type.Custom("Wirespec.Client", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Custom("?"))))),
        ),
        elements = listOf(
            Function(
                name = "getPathTemplate",
                parameters = emptyList(),
                returnType = Type.String,
                isOverride = true,
                body = listOf(ReturnStatement(Literal(pathTemplate, Type.String))),
            ),
            Function(
                name = "getMethod",
                parameters = emptyList(),
                returnType = Type.String,
                isOverride = true,
                body = listOf(ReturnStatement(Literal(method.name, Type.String))),
            ),
            Function(
                name = "getServer",
                parameters = listOf(Parameter("serialization", Type.Custom("Wirespec.Serialization"))),
                returnType = Type.Custom("Wirespec.ServerEdge", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Custom("?"))))),
                isOverride = true,
                body = listOf(
                    ReturnStatement(
                        AnonymousClass(
                            baseType = Type.Custom("Wirespec.ServerEdge"),
                            typeArguments = emptyList(),
                            methods = listOf(
                                Function(
                                    name = "from",
                                    parameters = listOf(Parameter("request", Type.Custom("Wirespec.RawRequest"))),
                                    returnType = Type.Custom("Request"),
                                    isOverride = true,
                                    body = listOf(
                                        ReturnStatement(
                                            Call(
                                                name = "fromRequest",
                                                arguments = mapOf(
                                                    "serialization" to VariableReference("serialization"),
                                                    "request" to VariableReference("request"),
                                                ),
                                            )
                                        )
                                    ),
                                ),
                                Function(
                                    name = "to",
                                    parameters = listOf(Parameter("response", Type.Custom("Response", listOf(Type.Custom("?"))))),
                                    returnType = Type.Custom("Wirespec.RawResponse"),
                                    isOverride = true,
                                    body = listOf(
                                        ReturnStatement(
                                            Call(
                                                name = "toResponse",
                                                arguments = mapOf(
                                                    "serialization" to VariableReference("serialization"),
                                                    "response" to VariableReference("response"),
                                                ),
                                            )
                                        )
                                    ),
                                ),
                            ),
                        )
                    )
                ),
            ),
            Function(
                name = "getClient",
                parameters = listOf(Parameter("serialization", Type.Custom("Wirespec.Serialization"))),
                returnType = Type.Custom("Wirespec.ClientEdge", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Custom("?"))))),
                isOverride = true,
                body = listOf(
                    ReturnStatement(
                        AnonymousClass(
                            baseType = Type.Custom("Wirespec.ClientEdge"),
                            typeArguments = emptyList(),
                            methods = listOf(
                                Function(
                                    name = "to",
                                    parameters = listOf(Parameter("request", Type.Custom("Request"))),
                                    returnType = Type.Custom("Wirespec.RawRequest"),
                                    isOverride = true,
                                    body = listOf(
                                        ReturnStatement(
                                            Call(
                                                name = "toRequest",
                                                arguments = mapOf(
                                                    "serialization" to VariableReference("serialization"),
                                                    "request" to VariableReference("request"),
                                                ),
                                            )
                                        )
                                    ),
                                ),
                                Function(
                                    name = "from",
                                    parameters = listOf(Parameter("response", Type.Custom("Wirespec.RawResponse"))),
                                    returnType = Type.Custom("Response", listOf(Type.Custom("?"))),
                                    isOverride = true,
                                    body = listOf(
                                        ReturnStatement(
                                            Call(
                                                name = "fromResponse",
                                                arguments = mapOf(
                                                    "serialization" to VariableReference("serialization"),
                                                    "response" to VariableReference("response"),
                                                ),
                                            )
                                        )
                                    ),
                                ),
                            ),
                        )
                    )
                ),
            ),
        ),
    )
}

fun EndpointWirespec.convertToResponse() = Function(
    name = "toResponse",
    isStatic = true,
    parameters = listOf(
        Parameter("serialization", Type.Custom("Wirespec.Serializer")),
        Parameter("response", Type.Custom("Response", listOf(Type.Custom("?")))),
    ),
    returnType = Type.Custom("Wirespec.RawResponse"),
    body = listOf(
        Switch(
            expression = VariableReference("response"),
            cases = responses.distinctBy { it.status }.map { response ->
                val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                Case(
                    value = NullLiteral,
                    type = Type.Custom("Response$statusClassName"),
                    variable = "r",
                    body = listOf(
                        ReturnStatement(
                            ConstructorStatement(
                                type = Type.Custom("Wirespec.RawResponse"),
                                namedArguments = mapOf(
                                    "status" to PropertyAccess(VariableReference("r"), "status"),
                                    "headers" to if (response.headers.isNotEmpty()) {
                                        LiteralMap(
                                            values = response.headers.associate { header ->
                                                header.identifier.value to Call(
                                                    name = "serialization.serializeParam",
                                                    arguments = mapOf(
                                                        "value" to PropertyAccess(
                                                            PropertyAccess(VariableReference("r"), "headers"),
                                                            header.identifier.value.sanitizeForJava()
                                                        ),
                                                        "type" to header.reference.toJavaType(),
                                                    ),
                                                )
                                            },
                                            keyType = Type.String,
                                            valueType = Type.Custom("List<String>"),
                                        )
                                    } else {
                                        StaticCall("java.util.Collections.emptyMap")
                                    },
                                    "body" to (
                                        response.content?.let { content ->
                                            Call(
                                                name = "serialization.serializeBody",
                                                arguments = mapOf(
                                                    "value" to PropertyAccess(VariableReference("r"), "body"),
                                                    "type" to content.reference.toJavaType(),
                                                ),
                                            )
                                        } ?: NullLiteral
                                        ),
                                ),
                            ),
                        ),
                    ),
                )
            },
            default = listOf(
                ErrorStatement(
                    BinaryOp(
                        Literal("Cannot match response with status: ", Type.String),
                        BinaryOp.Operator.PLUS,
                        PropertyAccess(VariableReference("response"), "status")
                    )
                ),
            ),
        ),
    ),
)

fun EndpointWirespec.convertFromResponse() = Function(
    name = "fromResponse",
    isStatic = true,
    parameters = listOf(
        Parameter("serialization", Type.Custom("Wirespec.Deserializer")),
        Parameter("response", Type.Custom("Wirespec.RawResponse")),
    ),
    returnType = Type.Custom("Response", listOf(Type.Custom("?"))),
    body = listOf(
        Switch(
            expression = PropertyAccess(VariableReference("response"), "statusCode"),
            cases = responses.distinctBy { it.status }.filter { it.status.toIntOrNull() != null }.map { response ->
                val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                Case(
                    value = Literal(response.status.toInt(), Type.Integer(Precision.P32)),
                    body = listOf(
                        ReturnStatement(
                            ConstructorStatement(
                                type = Type.Custom("Response$statusClassName"),
                                namedArguments = response.headers.associate { header ->
                                    header.identifier.value to Call(
                                        name = "serialization.deserializeParam",
                                        typeArguments = listOf(header.reference.convert()),
                                        arguments = mapOf(
                                            "value" to MethodCall(
                                                receiver = PropertyAccess(VariableReference("response"), "headers"),
                                                method = "getOrDefault",
                                                arguments = listOf(
                                                    Literal(header.identifier.value, Type.String),
                                                    StaticCall("java.util.Collections.emptyList"),
                                                ),
                                            ),
                                            "type" to header.reference.toJavaType(),
                                        ),
                                    )
                                } + (response.content?.let { content ->
                                    mapOf(
                                        "body" to Call(
                                            name = "serialization.deserializeBody",
                                            arguments = mapOf(
                                                "value" to PropertyAccess(VariableReference("response"), "body"),
                                                "type" to content.reference.toJavaType(),
                                            ),
                                        )
                                    )
                                } ?: emptyMap()),
                            ),
                        ),
                    ),
                )
            },
            default = listOf(
                ErrorStatement(
                    BinaryOp(
                        Literal("Cannot match response with status: ", Type.String),
                        BinaryOp.Operator.PLUS,
                        PropertyAccess(VariableReference("response"), "statusCode")
                    )
                ),
            ),
        ),
    ),
)

fun EndpointWirespec.convertToRequest() = Function(
    name = "toRequest",
    isStatic = true,
    parameters = listOf(
        Parameter("serialization", Type.Custom("Wirespec.Serializer")),
        Parameter("request", Type.Custom("Request")),
    ),
    returnType = Type.Custom("Wirespec.RawRequest"),
    body = listOf(
        ReturnStatement(
            ConstructorStatement(
                type = Type.Custom("Wirespec.RawRequest"),
                namedArguments = mapOf<String, Expression>(
                    "method" to PropertyAccess(PropertyAccess(VariableReference("request"), "method"), "name"),
                    "path" to LiteralList(
                        values = path.map {
                            when (it) {
                                is EndpointWirespec.Segment.Literal -> Literal(it.value, Type.String)
                                is EndpointWirespec.Segment.Param -> Call(
                                    name = "serialization.serializePath",
                                    arguments = mapOf(
                                        "value" to PropertyAccess(
                                            PropertyAccess(VariableReference("request"), "path"),
                                            it.identifier.value.replaceFirstChar { char -> char.lowercase() }
                                        ),
                                        "type" to it.reference.toJavaType(),
                                    ),
                                )
                            }
                        },
                        type = Type.String,
                    ),
                    "queries" to LiteralMap(
                        values = queries.associate {
                            it.identifier.value to Call(
                                name = "serialization.serializeParam",
                                arguments = mapOf(
                                    "value" to PropertyAccess(
                                        PropertyAccess(VariableReference("request"), "queries"),
                                        it.identifier.value.sanitizeForJava()
                                    ),
                                    "type" to it.reference.toJavaType(),
                                ),
                            )
                        },
                        keyType = Type.String,
                        valueType = Type.Custom("List<String>"),
                    ),
                    "headers" to LiteralMap(
                        values = headers.associate {
                            it.identifier.value to Call(
                                name = "serialization.serializeParam",
                                arguments = mapOf(
                                    "value" to PropertyAccess(
                                        PropertyAccess(VariableReference("request"), "headers"),
                                        it.identifier.value.sanitizeForJava()
                                    ),
                                    "type" to it.reference.toJavaType(),
                                ),
                            )
                        },
                        keyType = Type.String,
                        valueType = Type.Custom("List<String>"),
                    ),
                    "body" to (
                        requests.first().content?.let {
                            Call(
                                name = "serialization.serializeBody",
                                arguments = mapOf(
                                    "value" to PropertyAccess(VariableReference("request"), "body"),
                                    "type" to it.reference.toJavaType(),
                                ),
                            )
                        } ?: NullLiteral
                        ),
                ),
            ),
        ),
    ),
)

fun EndpointWirespec.convertFromRequest() = Function(
    name = "fromRequest",
    isStatic = true,
    parameters = listOf(
        Parameter("serialization", Type.Custom("Wirespec.Deserializer")),
        Parameter("request", Type.Custom("Wirespec.RawRequest")),
    ),
    returnType = Type.Custom("Request"),
    body = listOf(
        ReturnStatement(
            ConstructorStatement(
                type = Type.Custom("Request"),
                namedArguments = mapOf<String, Expression>()
                    .plus(
                        path.mapIndexedNotNull { index, segment ->
                            if (segment is EndpointWirespec.Segment.Param) {
                                segment.identifier.value to Call(
                                    name = "serialization.deserializePath",
                                    arguments = mapOf(
                                        "value" to MethodCall(
                                            receiver = PropertyAccess(VariableReference("request"), "path"),
                                            method = "get",
                                            arguments = listOf(Literal(index, Type.Integer(Precision.P32))),
                                        ),
                                        "type" to segment.reference.toJavaType(),
                                    ),
                                )
                            } else {
                                null
                            }
                        },
                    )
                    .plus(
                        queries.map { field ->
                            field.identifier.value to Call(
                                name = "serialization.deserializeParam",
                                arguments = mapOf(
                                    "value" to MethodCall(
                                        receiver = PropertyAccess(VariableReference("request"), "queries"),
                                        method = "getOrDefault",
                                        arguments = listOf(
                                            Literal(field.identifier.value, Type.String),
                                            StaticCall("java.util.Collections.emptyList"),
                                        ),
                                    ),
                                    "type" to field.reference.toJavaType(),
                                ),
                            )
                        },
                    )
                    .plus(
                        headers.map { field ->
                            field.identifier.value to Call(
                                name = "serialization.deserializeParam",
                                arguments = mapOf(
                                    "value" to MethodCall(
                                        receiver = PropertyAccess(VariableReference("request"), "headers"),
                                        method = "getOrDefault",
                                        arguments = listOf(
                                            Literal(field.identifier.value, Type.String),
                                            StaticCall("java.util.Collections.emptyList"),
                                        ),
                                    ),
                                    "type" to field.reference.toJavaType(),
                                ),
                            )
                        },
                    )
                    .plus(
                        requests.first().content?.let {
                            mapOf(
                                "body" to Call(
                                    name = "serialization.deserializeBody",
                                    arguments = mapOf(
                                        "value" to PropertyAccess(VariableReference("request"), "body"),
                                        "type" to it.reference.toJavaType(),
                                    ),
                                ),
                            )
                        } ?: emptyMap(),
                    ),
            ),
        ),
    ),
)

/**
 * Sanitize an identifier value for use as a Java method/field name.
 * Converts identifiers like "Refresh-Token" to "RefreshToken".
 */
private fun String.sanitizeForJava(): String = this
    .split(".", " ", "-")
    .mapIndexed { index, s -> if (index > 0) s.replaceFirstChar { c -> c.uppercaseChar() } else s }
    .joinToString("")
    .filter { it.isLetterOrDigit() || it == '_' }
    .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

private fun ReferenceWirespec.toJavaType(): Expression {
    val isIterable = this is ReferenceWirespec.Iterable
    val isNullable = this.isNullable

    val element = if (isIterable) (this as ReferenceWirespec.Iterable).reference else this

    val elementType = when (element) {
        is ReferenceWirespec.Custom -> Type.Custom(element.value)
        is ReferenceWirespec.Primitive -> when (val t = element.type) {
            is ReferenceWirespec.Primitive.Type.String -> Type.Custom("String")
            is ReferenceWirespec.Primitive.Type.Integer -> when (t.precision) {
                ReferenceWirespec.Primitive.Type.Precision.P32 -> Type.Custom("Integer")
                ReferenceWirespec.Primitive.Type.Precision.P64 -> Type.Custom("Long")
            }
            is ReferenceWirespec.Primitive.Type.Number -> when (t.precision) {
                ReferenceWirespec.Primitive.Type.Precision.P32 -> Type.Custom("Float")
                ReferenceWirespec.Primitive.Type.Precision.P64 -> Type.Custom("Double")
            }
            is ReferenceWirespec.Primitive.Type.Boolean -> Type.Custom("Boolean")
            is ReferenceWirespec.Primitive.Type.Bytes -> Type.Custom("byte[]")
        }
        else -> Type.Custom("Void")
    }

    val containerArg: Expression = if (isNullable) {
        ClassLiteral(Type.Custom("java.util.Optional"))
    } else if (isIterable) {
        ClassLiteral(Type.Custom("java.util.List"))
    } else {
        NullLiteral
    }

    return StaticCall(
        qualifiedName = "Wirespec.getType",
        arguments = listOf(
            ClassLiteral(elementType),
            containerArg,
        ),
    )
}
