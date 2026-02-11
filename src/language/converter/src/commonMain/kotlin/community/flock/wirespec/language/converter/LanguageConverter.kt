package community.flock.wirespec.language.converter

import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.language.core.ArrayIndexCall
import community.flock.wirespec.language.core.BinaryOp
import community.flock.wirespec.language.core.EnumValueCall
import community.flock.wirespec.language.core.ErrorStatement
import community.flock.wirespec.language.core.Expression
import community.flock.wirespec.language.core.FieldCall
import community.flock.wirespec.language.core.File
import community.flock.wirespec.language.core.FunctionCall
import community.flock.wirespec.language.core.Literal
import community.flock.wirespec.language.core.LiteralList
import community.flock.wirespec.language.core.LiteralMap
import community.flock.wirespec.language.core.NullCheck
import community.flock.wirespec.language.core.NullableEmpty
import community.flock.wirespec.language.core.NullableMap
import community.flock.wirespec.language.core.NullableOf
import community.flock.wirespec.language.core.Precision
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.TypeDescriptor
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.enum
import community.flock.wirespec.language.core.file
import community.flock.wirespec.language.core.`interface`
import community.flock.wirespec.language.core.struct
import community.flock.wirespec.language.core.union
import community.flock.wirespec.compiler.core.parse.ast.Channel as ChannelWirespec
import community.flock.wirespec.compiler.core.parse.ast.Definition as DefinitionWirespec
import community.flock.wirespec.compiler.core.parse.ast.Endpoint as EndpointWirespec
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumWirespec
import community.flock.wirespec.compiler.core.parse.ast.Field as FieldWirespec
import community.flock.wirespec.compiler.core.parse.ast.Reference as ReferenceWirespec
import community.flock.wirespec.compiler.core.parse.ast.Refined as RefinedWirespec
import community.flock.wirespec.compiler.core.parse.ast.Shared as SharedWirespec
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeWirespec
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionWirespec

fun DefinitionWirespec.convert(): File = when (this) {
    is TypeWirespec -> convert()
    is EnumWirespec -> convert()
    is UnionWirespec -> convert()
    is RefinedWirespec -> convert()
    is ChannelWirespec -> convert()
    is EndpointWirespec -> convert()
}

fun SharedWirespec.convert(): File = file("Wirespec") {
    `package`(packageString)

    static("Wirespec") {
        `interface`("Enum") {
            field("label", string)
        }
        `interface`("Endpoint")
        `interface`("Channel")
        `interface`("Refined") {
            typeParam(type("T"))
            field("value", type("T"))
        }
        `interface`("Path")
        `interface`("Queries")
        `interface`("Headers")
        `interface`("Handler")

        enum("Method") {
            entry("GET")
            entry("PUT")
            entry("POST")
            entry("DELETE")
            entry("OPTIONS")
            entry("HEAD")
            entry("PATCH")
            entry("TRACE")
        }
        `interface`("Request") {
            typeParam(type("T"))
            field("path", type("Path"))
            field("method", type("Method"))
            field("queries", type("Queries"))
            field("headers", type("Headers"))
            field("body", type("T"))
            `interface`("Headers")
        }
        `interface`("Response") {
            typeParam(type("T"))
            field("status", integer)
            field("headers", type("Headers"))
            field("body", type("T"))
            `interface`("Headers")
        }
        `interface`("BodySerializer") {
            function("serializeBody") {
                returnType(bytes)
                typeParam(type("T"))
                arg("t", type("T"))
                arg("type", type("Type"))
            }
        }
        `interface`("BodyDeserializer") {
            function("deserializeBody") {
                returnType(type("T"))
                typeParam(type("T"))
                arg("raw", bytes)
                arg("type", type("Type"))
            }
        }
        `interface`("BodySerialization") {
            extends(type("BodySerializer"))
            extends(type("BodyDeserializer"))
        }
        `interface`("PathSerializer") {
            function("serializePath") {
                returnType(string)
                typeParam(type("T"))
                arg("t", type("T"))
                arg("type", type("Type"))
            }
        }
        `interface`("PathDeserializer") {
            function("deserializePath") {
                returnType(type("T"))
                typeParam(type("T"))
                arg("raw", string)
                arg("type", type("Type"))
            }
        }
        `interface`("PathSerialization") {
            extends(type("PathSerializer"))
            extends(type("PathDeserializer"))
        }
        `interface`("ParamSerializer") {
            function("serializeParam") {
                returnType(list(string))
                typeParam(type("T"))
                arg("value", type("T"))
                arg("type", type("Type"))
            }
        }
        `interface`("ParamDeserializer") {
            function("deserializeParam") {
                returnType(type("T"))
                typeParam(type("T"))
                arg("values", list(string))
                arg("type", type("Type"))
            }
        }
        `interface`("ParamSerialization") {
            extends(type("ParamSerializer"))
            extends(type("ParamDeserializer"))
        }
        `interface`("Serializer") {
            extends(type("BodySerializer"))
            extends(type("PathSerializer"))
            extends(type("ParamSerializer"))
        }
        `interface`("Deserializer") {
            extends(type("BodyDeserializer"))
            extends(type("PathDeserializer"))
            extends(type("ParamDeserializer"))
        }
        `interface`("Serialization") {
            extends(type("Serializer"))
            extends(type("Deserializer"))
        }
        struct("RawRequest") {
            field("method", string)
            field("path", list(string))
            field("queries", dict(string, list(string)))
            field("headers", dict(string, list(string)))
            field("body", bytes.nullable())
        }
        struct("RawResponse") {
            field("statusCode", integer)
            field("headers", dict(string, list(string)))
            field("body", bytes.nullable())
        }
        `interface`("Transportation") {
            asyncFunction("transport") {
                returnType(type("RawResponse"))
                arg("request", type("RawRequest"))
            }
        }
    }
}

fun TypeWirespec.convert() = file(identifier.sanitize()) {
    struct(identifier.sanitize()) {
        extends.map { it.convert() }.filterIsInstance<Type.Custom>().forEach { implements(it) }
        shape.value.forEach {
            field(it.identifier.sanitize(), it.reference.convert())
        }
    }
}

fun EnumWirespec.convert() = file(identifier.sanitize()) {
    enum(identifier.sanitize(), Type.Custom("Wirespec.Enum")) {
        entries.forEach { entry(it) }
    }
}

fun UnionWirespec.convert() = file(identifier.sanitize()) {
    union(identifier.sanitize()) {
        entries.map { it.convert() }.filterIsInstance<Type.Custom>().forEach { member(it.name) }
    }
}

fun RefinedWirespec.convert() = file(identifier.sanitize()) {
    struct(identifier.sanitize()) {
        implements(type("Wirespec.Refined", reference.convert()))
        field("value", reference.convert())
    }
}

fun ChannelWirespec.convert() = file(identifier.sanitize()) {
    `interface`(identifier.sanitize()) {
        extends(type("Wirespec.Channel"))
        function("invoke") {
            arg("message", reference.convert())
            returnType(unit)
        }
    }
}

fun EndpointWirespec.convert(): File {
    val endpoint = this
    val pathParams = path.filterIsInstance<EndpointWirespec.Segment.Param>()
    val requestContent = requests.first().content
    val requestBodyType = requestContent?.reference?.convert() ?: Type.Unit

    return file(identifier.sanitize()) {
        `interface`(identifier.sanitize()) {
            extends(type("Wirespec.Endpoint"))

            // Path record
            struct("Path") {
                implements(type("Wirespec.Path"))
                pathParams.forEach { field(it.identifier.sanitize(), it.reference.convert()) }
            }

            // Queries record
            struct("Queries") {
                implements(type("Wirespec.Queries"))
                endpoint.queries.forEach { field(it.identifier.sanitize(), it.reference.convert()) }
            }

            // RequestHeaders record
            struct("RequestHeaders") {
                implements(type("Wirespec.Request.Headers"))
                endpoint.headers.forEach { field(it.identifier.sanitize(), it.reference.convert()) }
            }

            // Request record
            struct("Request") {
                implements(type("Wirespec.Request", requestBodyType))
                field("path", type("Path"), isOverride = true)
                field("method", type("Wirespec.Method"), isOverride = true)
                field("queries", type("Queries"), isOverride = true)
                field("headers", type("RequestHeaders"), isOverride = true)
                field("body", requestBodyType, isOverride = true)
                constructo {
                    endpoint.requestParameters().forEach { (name, type) -> arg(name, type) }
                    assign(
                        "path",
                        construct(type("Path")) {
                            pathParams.forEach {
                                arg(
                                    it.identifier.sanitize(),
                                    RawExpression(it.identifier.sanitize()),
                                )
                            }
                        },
                    )
                    assign("method", RawExpression("Wirespec.Method.${endpoint.method.name}"))
                    assign(
                        "queries",
                        construct(type("Queries")) {
                            endpoint.queries.forEach {
                                arg(
                                    it.identifier.sanitize(),
                                    RawExpression(it.identifier.sanitize()),
                                )
                            }
                        },
                    )
                    assign(
                        "headers",
                        construct(type("RequestHeaders")) {
                            endpoint.headers.forEach {
                                arg(
                                    it.identifier.sanitize(),
                                    RawExpression(it.identifier.sanitize()),
                                )
                            }
                        },
                    )
                    assign("body", if (requestContent != null) RawExpression("body") else construct(Type.Unit))
                }
            }

            // Pre-compute response names grouped by status prefix and content type
            val distinctResponses = endpoint.responses.distinctBy { it.status }
            val statusPrefixGroups = distinctResponses.groupBy { it.status.first() }
            val contentTypeGroups = distinctResponses.groupBy { it.content?.reference }

            val statusPrefixUnionNames = statusPrefixGroups.keys.map { "Response${it}XX" }
            val contentTypeUnionNames = contentTypeGroups.map { (ref, _) ->
                val contentType = ref?.convert() ?: Type.Unit
                "Response${contentType.toTypeName()}"
            }

            // Response union — members are the intermediate unions
            union("Response", extends = type("Wirespec.Response", type("T"))) {
                typeParam(type("T"))
                (statusPrefixUnionNames + contentTypeUnionNames).distinct().forEach { member(it) }
            }

            // Status prefix unions (Response2XX, Response5XX, etc.)
            statusPrefixGroups.forEach { (prefix, responses) ->
                union("Response${prefix}XX", extends = type("Response", type("T"))) {
                    typeParam(type("T"))
                    responses.forEach { member("Response${it.status.replaceFirstChar { c -> c.uppercaseChar() }}") }
                }
            }

            // Content type unions (ResponseUnit, ResponseTodoDto, etc.)
            contentTypeGroups.forEach { (ref, responses) ->
                val contentType = ref?.convert() ?: Type.Unit
                val typeName = contentType.toTypeName()
                union("Response$typeName", extends = type("Response", contentType)) {
                    responses.forEach { member("Response${it.status.replaceFirstChar { c -> c.uppercaseChar() }}") }
                }
            }

            // Individual response records (Response200, Response201, etc.)
            endpoint.responses.distinctBy { it.status }.forEach { response ->
                val bodyType = response.content?.reference?.convert() ?: Type.Unit
                val typeName = bodyType.toTypeName()
                val statusCode = response.status.toIntOrNull() ?: 0
                val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                struct("Response$statusClassName") {
                    field("status", integer32, isOverride = true)
                    field("headers", type("Headers"), isOverride = true)
                    field("body", bodyType, isOverride = true)
                    struct("Headers") {
                        implements(type("Wirespec.Response.Headers"))
                        response.headers.forEach { field(it.identifier.sanitize(), it.reference.convert()) }
                    }
                    constructo {
                        response.responseParameters().forEach { (name, type) -> arg(name, type) }
                        assign("status", Literal(statusCode, Type.Integer(Precision.P32)))
                        assign(
                            "headers",
                            construct(type("Headers")) {
                                response.headers.forEach {
                                    arg(
                                        it.identifier.sanitize(),
                                        RawExpression(it.identifier.sanitize()),
                                    )
                                }
                            },
                        )
                        assign("body", if (response.content != null) RawExpression("body") else construct(Type.Unit))
                    }
                }
            }

            // Conversion functions at Endpoint interface level
            function("toRawRequest", isStatic = true) {
                returnType(type("Wirespec.RawRequest"))
                arg("serialization", type("Wirespec.Serializer"))
                arg("request", type("Request"))
                returns(
                    construct(type("Wirespec.RawRequest")) {
                        arg("method", EnumValueCall(FieldCall(VariableReference("request"), "method")))
                        arg(
                            "path",
                            LiteralList(
                                values = endpoint.path.map {
                                    when (it) {
                                        is EndpointWirespec.Segment.Literal -> Literal(it.value, Type.String)
                                        is EndpointWirespec.Segment.Param -> FunctionCall(
                                            receiver = VariableReference("serialization"),
                                            name = "serializePath",
                                            typeArguments = listOf(it.reference.convert()),
                                            arguments = mapOf(
                                                "value" to FieldCall(
                                                    FieldCall(VariableReference("request"), "path"),
                                                    it.identifier.sanitize().replaceFirstChar { char -> char.lowercase() },
                                                ),
                                                "type" to it.reference.toTypeDescriptor(),
                                            ),
                                        )
                                    }
                                },
                                type = Type.String,
                            ),
                        )
                        arg(
                            "queries",
                            LiteralMap(
                                values = endpoint.queries.associate {
                                    it.identifier.sanitize() to serializeParamExpression(
                                        fieldAccess = FieldCall(
                                            FieldCall(VariableReference("request"), "queries"),
                                            it.identifier.sanitize(),
                                        ),
                                        field = it,
                                    )
                                },
                                keyType = Type.String,
                                valueType = Type.Custom("List<String>"),
                            ),
                        )
                        arg(
                            "headers",
                            LiteralMap(
                                values = endpoint.headers.associate {
                                    it.identifier.sanitize() to serializeParamExpression(
                                        fieldAccess = FieldCall(
                                            FieldCall(VariableReference("request"), "headers"),
                                            it.identifier.sanitize(),
                                        ),
                                        field = it,
                                    )
                                },
                                keyType = Type.String,
                                valueType = Type.Custom("List<String>"),
                            ),
                        )
                        arg(
                            "body",
                            endpoint.requests.first().content?.let {
                                NullableOf(
                                    FunctionCall(
                                        receiver = VariableReference("serialization"),
                                        name = "serializeBody",
                                        typeArguments = listOf(it.reference.convert()),
                                        arguments = mapOf(
                                            "value" to FieldCall(VariableReference("request"), "body"),
                                            "type" to it.reference.toTypeDescriptor(),
                                        ),
                                    ),
                                )
                            } ?: NullableEmpty,
                        )
                    },
                )
            }

            function("fromRawRequest", isStatic = true) {
                returnType(type("Request"))
                arg("serialization", type("Wirespec.Deserializer"))
                arg("request", type("Wirespec.RawRequest"))
                returns(
                    construct(type("Request")) {
                        endpoint.path.forEachIndexed { index, segment ->
                            if (segment is EndpointWirespec.Segment.Param) {
                                arg(
                                    segment.identifier.sanitize(),
                                    FunctionCall(
                                        receiver = VariableReference("serialization"),
                                        name = "deserializePath",
                                        typeArguments = listOf(segment.reference.convert()),
                                        arguments = mapOf(
                                            "value" to ArrayIndexCall(
                                                receiver = FieldCall(VariableReference("request"), "path"),
                                                index = Literal(index, Type.Integer(Precision.P32)),
                                            ),
                                            "type" to segment.reference.toTypeDescriptor(),
                                        ),
                                    ),
                                )
                            }
                        }
                        endpoint.queries.forEach { field ->
                            arg(
                                field.identifier.sanitize(),
                                deserializeParamExpression(
                                    map = FieldCall(VariableReference("request"), "queries"),
                                    fieldName = field.identifier.sanitize(),
                                    field = field,
                                ),
                            )
                        }
                        endpoint.headers.forEach { field ->
                            arg(
                                field.identifier.sanitize(),
                                deserializeParamExpression(
                                    map = FieldCall(VariableReference("request"), "headers"),
                                    fieldName = field.identifier.sanitize(),
                                    field = field,
                                ),
                            )
                        }
                        endpoint.requests.first().content?.let {
                            arg(
                                "body",
                                NullableMap(
                                    expression = FieldCall(VariableReference("request"), "body"),
                                    body = FunctionCall(
                                        receiver = VariableReference("serialization"),
                                        name = "deserializeBody",
                                        typeArguments = listOf(it.reference.convert()),
                                        arguments = mapOf(
                                            "value" to VariableReference("it"),
                                            "type" to it.reference.toTypeDescriptor(),
                                        ),
                                    ),
                                    alternative = ErrorStatement(Literal("body is null", Type.String)),
                                ),
                            )
                        }
                    },
                )
            }

            function("toRawResponse", isStatic = true) {
                returnType(type("Wirespec.RawResponse"))
                arg("serialization", type("Wirespec.Serializer"))
                arg("response", type("Response", wildcard))
                switch(VariableReference("response"), "r") {
                    endpoint.responses.distinctBy { it.status }.forEach { response ->
                        val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                        case(type("Response$statusClassName")) {
                            returns(
                                construct(type("Wirespec.RawResponse")) {
                                    arg("statusCode", FieldCall(VariableReference("r"), "status"))
                                    arg(
                                        "headers",
                                        LiteralMap(
                                            values = response.headers.associate { header ->
                                                header.identifier.sanitize() to serializeParamExpression(
                                                    fieldAccess = FieldCall(
                                                        FieldCall(VariableReference("r"), "headers"),
                                                        header.identifier.sanitize(),
                                                    ),
                                                    field = header,
                                                )
                                            },
                                            keyType = Type.String,
                                            valueType = Type.Custom("List<String>"),
                                        ),
                                    )
                                    arg(
                                        "body",
                                        response.content?.let { content ->
                                            NullableOf(
                                                FunctionCall(
                                                    receiver = VariableReference("serialization"),
                                                    name = "serializeBody",
                                                    arguments = mapOf(
                                                        "value" to FieldCall(VariableReference("r"), "body"),
                                                        "type" to content.reference.toTypeDescriptor(),
                                                    ),
                                                ),
                                            )
                                        } ?: NullableEmpty,
                                    )
                                },
                            )
                        }
                    }
                    default {
                        error(
                            BinaryOp(
                                Literal("Cannot match response with status: ", Type.String),
                                BinaryOp.Operator.PLUS,
                                FieldCall(VariableReference("response"), "status"),
                            ),
                        )
                    }
                }
            }

            function("fromRawResponse", isStatic = true) {
                returnType(type("Response", wildcard))
                arg("serialization", type("Wirespec.Deserializer"))
                arg("response", type("Wirespec.RawResponse"))
                switch(FieldCall(receiver = VariableReference("response"), field = "statusCode")) {
                    endpoint.responses.distinctBy { it.status }.filter { it.status.toIntOrNull() != null }
                        .forEach { response ->
                            val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                            case(literal(response.status.toInt())) {
                                returns(
                                    construct(type("Response$statusClassName")) {
                                        response.headers.forEach { header ->
                                            arg(
                                                header.identifier.sanitize(),
                                                deserializeParamExpression(
                                                    map = FieldCall(VariableReference("response"), "headers"),
                                                    fieldName = header.identifier.sanitize(),
                                                    field = header,
                                                ),
                                            )
                                        }
                                        response.content?.let { content ->
                                            arg(
                                                "body",
                                                NullableMap(
                                                    expression = FieldCall(VariableReference("response"), "body"),
                                                    body = FunctionCall(
                                                        receiver = VariableReference("serialization"),
                                                        name = "deserializeBody",
                                                        typeArguments = listOf(content.reference.convert()),
                                                        arguments = mapOf(
                                                            "value" to VariableReference("it"),
                                                            "type" to content.reference.toTypeDescriptor(),
                                                        ),
                                                    ),
                                                    alternative = ErrorStatement(Literal("body is null", Type.String)),
                                                ),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    default {
                        error(
                            BinaryOp(
                                Literal("Cannot match response with status: ", Type.String),
                                BinaryOp.Operator.PLUS,
                                FieldCall(VariableReference("response"), "statusCode"),
                            ),
                        )
                    }
                }
            }

            // Handler interface
            `interface`("Handler") {
                extends(type("Wirespec.Handler"))
                asyncFunction(endpoint.identifier.sanitize().replaceFirstChar { it.lowercase() }) {
                    arg("request", type("Request"))
                    returnType(type("Response", wildcard))
                }
            }
        }
    }
}

private fun Type.toTypeName(): String = when (this) {
    Type.Any -> "Any"
    is Type.Unit -> "Unit"
    is Type.Wildcard -> "Wildcard"
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
    is ReferenceWirespec.Any -> Type.Custom("Any")
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

private fun ReferenceWirespec.toTypeDescriptor(): TypeDescriptor = TypeDescriptor(convert())

private fun deserializeParamExpression(
    map: Expression,
    fieldName: String,
    field: FieldWirespec,
): Expression {
    val type = field.reference.copy(isNullable = false)
    val getCall = ArrayIndexCall(
        receiver = map,
        index = Literal(fieldName, Type.String),
    )
    return NullCheck(
        expression = getCall,
        body = FunctionCall(
            receiver = VariableReference("serialization"),
            name = "deserializeParam",
            typeArguments = listOf(type.convert()),
            arguments = mapOf(
                "value" to VariableReference("it"),
                "type" to type.toTypeDescriptor(),
            ),
        ),
        alternative = if (field.reference.isNullable) {
            null
        } else {
            ErrorStatement(
                Literal(
                    "Param $fieldName cannot be null",
                    Type.String,
                ),
            )
        },
    )
}

private fun serializeParamExpression(
    fieldAccess: Expression,
    field: FieldWirespec,
): Expression {
    val type = field.reference.copy(isNullable = false)
    val serializeCall = FunctionCall(
        receiver = VariableReference("serialization"),
        name = "serializeParam",
        typeArguments = listOf(type.convert()),
        arguments = mapOf(
            "value" to VariableReference("it"),
            "type" to type.toTypeDescriptor(),
        ),
    )
    return if (field.reference.isNullable) {
        NullableMap(
            expression = fieldAccess,
            body = serializeCall,
            alternative = LiteralList(emptyList(), Type.String),
        )
    } else {
        FunctionCall(
            receiver = VariableReference("serialization"),
            name = "serializeParam",
            typeArguments = listOf(type.convert()),
            arguments = mapOf(
                "value" to fieldAccess,
                "type" to field.reference.toTypeDescriptor(),
            ),
        )
    }
}

fun EndpointWirespec.requestParameters(): List<Pair<String, Type>> = buildList {
    path.filterIsInstance<EndpointWirespec.Segment.Param>()
        .forEach { add(it.identifier.sanitize() to it.reference.convert()) }
    queries.forEach { add(it.identifier.sanitize() to it.reference.convert()) }
    headers.forEach { add(it.identifier.sanitize() to it.reference.convert()) }
    requests.first().content?.let { add("body" to it.reference.convert()) }
}

fun EndpointWirespec.Response.responseParameters(): List<Pair<String, Type>> = buildList {
    headers.forEach { add(it.identifier.sanitize() to it.reference.convert()) }
    content?.let { add("body" to it.reference.convert()) }
}

private fun Identifier.sanitize() = when (this) {
    is FieldIdentifier ->
        value
            .split(".", " ", "-")
            .joinToString("") { it.firstToUpper() }
            .firstToLower()

    is DefinitionIdentifier ->
        value
            .split("-")
            .joinToString("") { it.firstToUpper() }
}
