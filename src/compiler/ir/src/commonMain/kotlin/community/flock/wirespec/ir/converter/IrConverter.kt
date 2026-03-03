package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.ir.core.ArrayIndexCall
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.EnumReference
import community.flock.wirespec.ir.core.EnumValueCall
import community.flock.wirespec.ir.core.ErrorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FlatMapIndexed
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.IfExpression
import community.flock.wirespec.ir.core.ListConcat
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.LiteralMap
import community.flock.wirespec.ir.core.MapExpression
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.NotExpression
import community.flock.wirespec.ir.core.NullCheck
import community.flock.wirespec.ir.core.NullableEmpty
import community.flock.wirespec.ir.core.NullableMap
import community.flock.wirespec.ir.core.NullableOf
import community.flock.wirespec.ir.core.Precision
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.StringTemplate
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.TypeDescriptor
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.transformMatchingElements
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
import community.flock.wirespec.ir.core.Constraint as LanguageConstraint

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

    namespace("Wirespec") {
        `interface`("Model") {
            function("validate") {
                returnType(list(string))
            }
        }
        `interface`("Enum") {
            field("label", string)
        }
        `interface`("Endpoint")
        `interface`("Channel")
        `interface`("Refined") {
            typeParam(type("T"))
            field("value", type("T"))
            function("validate") {
                returnType(boolean)
            }
        }
        `interface`("Path")
        `interface`("Queries")
        `interface`("Headers")
        `interface`("Handler")
        `interface`("Call")

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
            function(Name("serialize", "Body")) {
                returnType(bytes)
                typeParam(type("T"))
                arg("t", type("T"))
                arg("type", reflect)
            }
        }
        `interface`("BodyDeserializer") {
            function(Name("deserialize", "Body")) {
                returnType(type("T"))
                typeParam(type("T"))
                arg("raw", bytes)
                arg("type", reflect)
            }
        }
        `interface`("BodySerialization") {
            extends(type("BodySerializer"))
            extends(type("BodyDeserializer"))
        }
        `interface`("PathSerializer") {
            function(Name("serialize", "Path")) {
                returnType(string)
                typeParam(type("T"))
                arg("t", type("T"))
                arg("type", reflect)
            }
        }
        `interface`("PathDeserializer") {
            function(Name("deserialize", "Path")) {
                returnType(type("T"))
                typeParam(type("T"))
                arg("raw", string)
                arg("type", reflect)
            }
        }
        `interface`("PathSerialization") {
            extends(type("PathSerializer"))
            extends(type("PathDeserializer"))
        }
        `interface`("ParamSerializer") {
            function(Name("serialize", "Param")) {
                returnType(list(string))
                typeParam(type("T"))
                arg("value", type("T"))
                arg("type", reflect)
            }
        }
        `interface`("ParamDeserializer") {
            function(Name("deserialize", "Param")) {
                returnType(type("T"))
                typeParam(type("T"))
                arg("values", list(string))
                arg("type", reflect)
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
            field(Name("status", "Code"), integer)
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

private fun Identifier.toName(): Name = when (this) {
    is FieldIdentifier -> {
        // Split on invalid identifier characters (dashes, dots, spaces) to produce word parts.
        // The emitter's transform phase is responsible for applying language-specific casing.
        val parts = value.split(Regex("[.\\s-]+")).filter { it.isNotEmpty() }
        Name(parts)
    }
    is DefinitionIdentifier -> Name(
        Name.of(value).parts.filter { part -> part.any { it.isLetterOrDigit() } },
    )
}

fun TypeWirespec.convert() = file(identifier.toName()) {
    struct(identifier.toName()) {
        implements(Type.Custom("Wirespec.Model"))
        extends.map { it.convert() }.filterIsInstance<Type.Custom>().forEach { implements(it) }
        shape.value.forEach {
            field(it.identifier.toName(), it.reference.convert())
        }
        function("validate", isOverride = true) {
            returnType(Type.Array(Type.String))
            returns(LiteralList(emptyList(), Type.String))
        }
    }
}

data class FieldValidation(
    val fieldName: Name,
    val fieldPath: String,
    val kind: Kind,
    val isNullable: Boolean,
    val typeName: String,
    val elementIsNullable: Boolean = false,
)

enum class Kind { MODEL, REFINED, MODEL_ARRAY, REFINED_ARRAY }

fun TypeWirespec.convertWithValidation(module: Module): File {
    val fieldValidations = classifyValidatableFields(module)
    val file = convert()
    return if (fieldValidations.isNotEmpty()) {
        file.transformMatchingElements { fn: community.flock.wirespec.ir.core.Function ->
            if (fn.name == Name.of("validate")) {
                fn.copy(body = listOf(ReturnStatement(buildValidateBody(fieldValidations))))
            } else {
                fn
            }
        }
    } else {
        file
    }
}

private fun buildValidateBody(validations: List<FieldValidation>): Expression {
    if (validations.isEmpty()) return LiteralList(emptyList(), Type.String)
    val exprs = validations.map { it.toExpression() }
    return if (exprs.size == 1) exprs.single() else ListConcat(exprs)
}

private fun FieldValidation.toExpression(): Expression {
    val fieldRef: Expression = FieldCall(field = fieldName)
    // When nullable, NullableMap uses "it" as the lambda variable for the unwrapped value
    val valueRef: Expression = if (isNullable) VariableReference(Name.of("it")) else fieldRef
    // typeArguments carries the validated type name (used by TypeScript emitter to derive standalone function name)
    val validateCall = FunctionCall(
        receiver = valueRef,
        name = Name.of("validate"),
        typeArguments = listOf(Type.Custom(typeName)),
    )

    fun stringTemplate(vararg parts: StringTemplate.Part) = StringTemplate(parts.toList())
    fun text(value: String) = StringTemplate.Part.Text(value)
    fun expr(expression: Expression) = StringTemplate.Part.Expr(expression)

    val body: Expression = when (kind) {
        Kind.MODEL -> MapExpression(
            receiver = validateCall,
            variable = Name.of("e"),
            body = stringTemplate(text("$fieldPath."), expr(VariableReference(Name.of("e")))),
        )
        Kind.REFINED -> IfExpression(
            condition = NotExpression(validateCall),
            thenExpr = LiteralList(listOf(Literal(fieldPath, Type.String)), Type.String),
            elseExpr = LiteralList(emptyList(), Type.String),
        )
        Kind.MODEL_ARRAY -> FlatMapIndexed(
            receiver = valueRef,
            indexVar = Name.of("i"),
            elementVar = Name.of("el"),
            body = MapExpression(
                receiver = FunctionCall(
                    receiver = VariableReference(Name.of("el")),
                    name = Name.of("validate"),
                    typeArguments = listOf(Type.Custom(typeName)),
                ),
                variable = Name.of("e"),
                body = stringTemplate(text("$fieldPath["), expr(VariableReference(Name.of("i"))), text("]."), expr(VariableReference(Name.of("e")))),
            ),
        )
        Kind.REFINED_ARRAY -> FlatMapIndexed(
            receiver = valueRef,
            indexVar = Name.of("i"),
            elementVar = Name.of("el"),
            body = IfExpression(
                condition = NotExpression(
                    FunctionCall(
                        receiver = VariableReference(Name.of("el")),
                        name = Name.of("validate"),
                        typeArguments = listOf(Type.Custom(typeName)),
                    ),
                ),
                thenExpr = LiteralList(
                    listOf(stringTemplate(text("$fieldPath["), expr(VariableReference(Name.of("i"))), text("]"))),
                    Type.String,
                ),
                elseExpr = LiteralList(emptyList(), Type.String),
            ),
        )
    }

    return if (isNullable) {
        NullableMap(
            expression = fieldRef,
            body = body,
            alternative = LiteralList(emptyList(), Type.String),
        )
    } else {
        body
    }
}

fun TypeWirespec.classifyValidatableFields(module: Module): List<FieldValidation> = buildList {
    for (field in shape.value) {
        val fieldName = field.identifier.toName()
        val fieldPath = field.identifier.value
        val ref = field.reference
        val isNullable = ref.isNullable
        when (ref) {
            is ReferenceWirespec.Custom -> {
                val typeName = ref.value
                val def = module.statements.firstOrNull {
                    it.identifier.value == typeName
                }
                when (def) {
                    is TypeWirespec -> add(
                        FieldValidation(
                            fieldName = fieldName,
                            fieldPath = fieldPath,
                            kind = Kind.MODEL,
                            isNullable = isNullable,
                            typeName = typeName,
                        ),
                    )
                    is RefinedWirespec -> add(
                        FieldValidation(
                            fieldName = fieldName,
                            fieldPath = fieldPath,
                            kind = Kind.REFINED,
                            isNullable = isNullable,
                            typeName = typeName,
                        ),
                    )
                    else -> {} // enum, union, etc. - skip
                }
            }
            is ReferenceWirespec.Iterable -> {
                val inner = ref.reference
                if (inner is ReferenceWirespec.Custom) {
                    val typeName = inner.value
                    val def = module.statements.firstOrNull {
                        it.identifier.value == typeName
                    }
                    when (def) {
                        is TypeWirespec -> add(
                            FieldValidation(
                                fieldName = fieldName,
                                fieldPath = fieldPath,
                                kind = Kind.MODEL_ARRAY,
                                isNullable = isNullable,
                                typeName = typeName,
                                elementIsNullable = inner.isNullable,
                            ),
                        )
                        is RefinedWirespec -> add(
                            FieldValidation(
                                fieldName = fieldName,
                                fieldPath = fieldPath,
                                kind = Kind.REFINED_ARRAY,
                                isNullable = isNullable,
                                typeName = typeName,
                                elementIsNullable = inner.isNullable,
                            ),
                        )
                        else -> {} // skip
                    }
                }
            }
            else -> {} // Primitive, Dict, Unit, Any - skip
        }
    }
}

fun EnumWirespec.convert() = file(identifier.toName()) {
    enum(identifier.toName(), Type.Custom("Wirespec.Enum")) {
        entries.forEach { entry(it) }
    }
}

fun UnionWirespec.convert() = file(identifier.toName()) {
    union(identifier.toName()) {
        entries.map { it.convert() }.filterIsInstance<Type.Custom>().forEach { member(it.name) }
    }
}

fun RefinedWirespec.convert() = file(identifier.toName()) {
    struct(identifier.toName()) {
        implements(type("Wirespec.Refined", reference.convert()))
        field("value", reference.convert())
        function("validate") {
            returnType(Type.Boolean)
            returns(reference.convertConstraint(VariableReference(Name.of("value"))))
        }
    }
}

fun ChannelWirespec.convert() = file(identifier.toName()) {
    `interface`(identifier.toName()) {
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

    return file(identifier.toName()) {
        namespace(identifier.toName(), type("Wirespec.Endpoint")) {
            // Path record
            struct("Path") {
                implements(type("Wirespec.Path"))
                pathParams.forEach { field(it.identifier.toName(), it.reference.convert()) }
            }

            // Queries record
            struct("Queries") {
                implements(type("Wirespec.Queries"))
                endpoint.queries.forEach { field(it.identifier.toName(), it.reference.convert()) }
            }

            // RequestHeaders record
            struct("RequestHeaders") {
                implements(type("Wirespec.Request.Headers"))
                endpoint.headers.forEach { field(it.identifier.toName(), it.reference.convert()) }
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
                                    it.identifier.toName(),
                                    VariableReference(it.identifier.toName()),
                                )
                            }
                        },
                    )
                    assign("method", EnumReference(Type.Custom("Wirespec.Method"), Name.of(endpoint.method.name)))
                    assign(
                        "queries",
                        construct(type("Queries")) {
                            endpoint.queries.forEach {
                                arg(
                                    it.identifier.toName(),
                                    VariableReference(it.identifier.toName()),
                                )
                            }
                        },
                    )
                    assign(
                        "headers",
                        construct(type("RequestHeaders")) {
                            endpoint.headers.forEach {
                                arg(
                                    it.identifier.toName(),
                                    VariableReference(it.identifier.toName()),
                                )
                            }
                        },
                    )
                    assign("body", if (requestContent != null) VariableReference(Name.of("body")) else construct(Type.Unit))
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
                val statusCode = response.status.toIntOrNull() ?: 0
                val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                struct("Response$statusClassName") {
                    field("status", integer32, isOverride = true)
                    field("headers", type("Headers"), isOverride = true)
                    field("body", bodyType, isOverride = true)
                    struct("Headers") {
                        implements(type("Wirespec.Response.Headers"))
                        response.headers.forEach { field(it.identifier.toName(), it.reference.convert()) }
                    }
                    constructo {
                        response.responseParameters().forEach { (name, type) -> arg(name, type) }
                        assign("status", Literal(statusCode, Type.Integer(Precision.P32)))
                        assign(
                            "headers",
                            construct(type("Headers")) {
                                response.headers.forEach {
                                    arg(
                                        it.identifier.toName(),
                                        VariableReference(it.identifier.toName()),
                                    )
                                }
                            },
                        )
                        assign("body", if (response.content != null) VariableReference(Name.of("body")) else construct(Type.Unit))
                    }
                }
            }

            // Conversion functions at Endpoint interface level
            function(Name("to", "Raw", "Request"), isStatic = true) {
                returnType(type("Wirespec.RawRequest"))
                arg("serialization", type("Wirespec.Serializer"))
                arg("request", type("Request"))
                returns(
                    construct(type("Wirespec.RawRequest")) {
                        arg("method", EnumValueCall(FieldCall(VariableReference(Name.of("request")), Name.of("method"))))
                        arg(
                            "path",
                            LiteralList(
                                values = endpoint.path.map {
                                    when (it) {
                                        is EndpointWirespec.Segment.Literal -> Literal(it.value, Type.String)
                                        is EndpointWirespec.Segment.Param -> FunctionCall(
                                            receiver = VariableReference(Name.of("serialization")),
                                            name = Name("serialize", "Path"),
                                            typeArguments = listOf(it.reference.convert()),
                                            arguments = mapOf(
                                                Name.of("value") to FieldCall(
                                                    FieldCall(VariableReference(Name.of("request")), Name.of("path")),
                                                    it.identifier.toName(),
                                                ),
                                                Name.of("type") to it.reference.toTypeDescriptor(),
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
                                    it.identifier.value to serializeParamExpression(
                                        fieldAccess = FieldCall(
                                            FieldCall(VariableReference(Name.of("request")), Name.of("queries")),
                                            it.identifier.toName(),
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
                                    it.identifier.value to serializeParamExpression(
                                        fieldAccess = FieldCall(
                                            FieldCall(VariableReference(Name.of("request")), Name.of("headers")),
                                            it.identifier.toName(),
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
                                        receiver = VariableReference(Name.of("serialization")),
                                        name = Name("serialize", "Body"),
                                        typeArguments = listOf(it.reference.convert()),
                                        arguments = mapOf(
                                            Name.of("value") to FieldCall(VariableReference(Name.of("request")), Name.of("body")),
                                            Name.of("type") to it.reference.toTypeDescriptor(),
                                        ),
                                    ),
                                )
                            } ?: NullableEmpty,
                        )
                    },
                )
            }

            function(Name("from", "Raw", "Request"), isStatic = true) {
                returnType(type("Request"))
                arg("serialization", type("Wirespec.Deserializer"))
                arg("request", type("Wirespec.RawRequest"))
                returns(
                    construct(type("Request")) {
                        endpoint.path.forEachIndexed { index, segment ->
                            if (segment is EndpointWirespec.Segment.Param) {
                                arg(
                                    segment.identifier.toName(),
                                    FunctionCall(
                                        receiver = VariableReference(Name.of("serialization")),
                                        name = Name("deserialize", "Path"),
                                        typeArguments = listOf(segment.reference.convert()),
                                        arguments = mapOf(
                                            Name.of("value") to ArrayIndexCall(
                                                receiver = FieldCall(VariableReference(Name.of("request")), Name.of("path")),
                                                index = Literal(index, Type.Integer(Precision.P32)),
                                            ),
                                            Name.of("type") to segment.reference.toTypeDescriptor(),
                                        ),
                                    ),
                                )
                            }
                        }
                        endpoint.queries.forEach { field ->
                            arg(
                                field.identifier.toName(),
                                deserializeParamExpression(
                                    map = FieldCall(VariableReference(Name.of("request")), Name.of("queries")),
                                    fieldName = field.identifier.value,
                                    field = field,
                                ),
                            )
                        }
                        endpoint.headers.forEach { field ->
                            arg(
                                field.identifier.toName(),
                                deserializeParamExpression(
                                    map = FieldCall(VariableReference(Name.of("request")), Name.of("headers")),
                                    fieldName = field.identifier.value,
                                    field = field,
                                ),
                            )
                        }
                        endpoint.requests.first().content?.let {
                            arg(
                                "body",
                                NullableMap(
                                    expression = FieldCall(VariableReference(Name.of("request")), Name.of("body")),
                                    body = FunctionCall(
                                        receiver = VariableReference(Name.of("serialization")),
                                        name = Name("deserialize", "Body"),
                                        typeArguments = listOf(it.reference.convert()),
                                        arguments = mapOf(
                                            Name.of("value") to VariableReference(Name.of("it")),
                                            Name.of("type") to it.reference.toTypeDescriptor(),
                                        ),
                                    ),
                                    alternative = ErrorStatement(Literal("body is null", Type.String)),
                                ),
                            )
                        }
                    },
                )
            }

            function(Name("to", "Raw", "Response"), isStatic = true) {
                returnType(type("Wirespec.RawResponse"))
                arg("serialization", type("Wirespec.Serializer"))
                arg("response", type("Response", wildcard))
                switch(VariableReference(Name.of("response")), "r") {
                    endpoint.responses.distinctBy { it.status }.forEach { response ->
                        val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                        case(type("Response$statusClassName")) {
                            returns(
                                construct(type("Wirespec.RawResponse")) {
                                    arg(Name("status", "Code"), FieldCall(VariableReference(Name.of("r")), Name.of("status")))
                                    arg(
                                        "headers",
                                        LiteralMap(
                                            values = response.headers.associate { header ->
                                                header.identifier.value to serializeParamExpression(
                                                    fieldAccess = FieldCall(
                                                        FieldCall(VariableReference(Name.of("r")), Name.of("headers")),
                                                        header.identifier.toName(),
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
                                                    receiver = VariableReference(Name.of("serialization")),
                                                    name = Name("serialize", "Body"),
                                                    arguments = mapOf(
                                                        Name.of("value") to FieldCall(VariableReference(Name.of("r")), Name.of("body")),
                                                        Name.of("type") to content.reference.toTypeDescriptor(),
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
                                FieldCall(VariableReference(Name.of("response")), Name.of("status")),
                            ),
                        )
                    }
                }
            }

            function(Name("from", "Raw", "Response"), isStatic = true) {
                returnType(type("Response", wildcard))
                arg("serialization", type("Wirespec.Deserializer"))
                arg("response", type("Wirespec.RawResponse"))
                switch(FieldCall(receiver = VariableReference(Name.of("response")), field = Name("status", "Code"))) {
                    endpoint.responses.distinctBy { it.status }.filter { it.status.toIntOrNull() != null }
                        .forEach { response ->
                            val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                            case(literal(response.status.toInt())) {
                                returns(
                                    construct(type("Response$statusClassName")) {
                                        response.headers.forEach { header ->
                                            arg(
                                                header.identifier.toName(),
                                                deserializeParamExpression(
                                                    map = FieldCall(VariableReference(Name.of("response")), Name.of("headers")),
                                                    fieldName = header.identifier.value,
                                                    field = header,
                                                ),
                                            )
                                        }
                                        response.content?.let { content ->
                                            arg(
                                                "body",
                                                NullableMap(
                                                    expression = FieldCall(VariableReference(Name.of("response")), Name.of("body")),
                                                    body = FunctionCall(
                                                        receiver = VariableReference(Name.of("serialization")),
                                                        name = Name("deserialize", "Body"),
                                                        typeArguments = listOf(content.reference.convert()),
                                                        arguments = mapOf(
                                                            Name.of("value") to VariableReference(Name.of("it")),
                                                            Name.of("type") to content.reference.toTypeDescriptor(),
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
                                FieldCall(VariableReference(Name.of("response")), Name("status", "Code")),
                            ),
                        )
                    }
                }
            }

            // Handler interface
            `interface`("Handler") {
                extends(type("Wirespec.Handler"))
                asyncFunction(endpoint.identifier.toName()) {
                    arg("request", type("Request"))
                    returnType(type("Response", wildcard))
                }
            }

            // Call interface
            `interface`("Call") {
                extends(type("Wirespec.Call"))
                asyncFunction(endpoint.identifier.toName()) {
                    endpoint.requestParameters().forEach { (name, type) -> arg(name, type) }
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
    is Type.Reflect -> "Type"
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

fun ReferenceWirespec.Primitive.Type.Constraint.convert(value: Expression): LanguageConstraint = when (this) {
    is ReferenceWirespec.Primitive.Type.Constraint.RegExp ->
        LanguageConstraint.RegexMatch(
            pattern = this.value.split("/").drop(1).dropLast(1).joinToString("/"),
            rawValue = this.value,
            value = value,
        )

    is ReferenceWirespec.Primitive.Type.Constraint.Bound ->
        LanguageConstraint.BoundCheck(min = min, max = max, value = value)
}

fun ReferenceWirespec.Primitive.convertConstraint(value: Expression): Expression = when (val t = type) {
    is ReferenceWirespec.Primitive.Type.String -> t.constraint?.convert(value)
    is ReferenceWirespec.Primitive.Type.Integer -> t.constraint?.convert(value)
    is ReferenceWirespec.Primitive.Type.Number -> t.constraint?.convert(value)
    ReferenceWirespec.Primitive.Type.Boolean -> null
    ReferenceWirespec.Primitive.Type.Bytes -> null
} ?: Literal(true, Type.Boolean)

fun ReferenceWirespec.convertConstraint(value: Expression): Expression = when (this) {
    is ReferenceWirespec.Primitive -> convertConstraint(value)
    else -> Literal(true, Type.Boolean)
}

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
            receiver = VariableReference(Name.of("serialization")),
            name = Name("deserialize", "Param"),
            typeArguments = listOf(type.convert()),
            arguments = mapOf(
                Name.of("value") to VariableReference(Name.of("it")),
                Name.of("type") to type.toTypeDescriptor(),
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
        receiver = VariableReference(Name.of("serialization")),
        name = Name("serialize", "Param"),
        typeArguments = listOf(type.convert()),
        arguments = mapOf(
            Name.of("value") to VariableReference(Name.of("it")),
            Name.of("type") to type.toTypeDescriptor(),
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
            receiver = VariableReference(Name.of("serialization")),
            name = Name("serialize", "Param"),
            typeArguments = listOf(type.convert()),
            arguments = mapOf(
                Name.of("value") to fieldAccess,
                Name.of("type") to field.reference.toTypeDescriptor(),
            ),
        )
    }
}

fun EndpointWirespec.requestParameters(): List<Pair<Name, Type>> = buildList {
    path.filterIsInstance<EndpointWirespec.Segment.Param>()
        .forEach { add(it.identifier.toName() to it.reference.convert()) }
    queries.forEach { add(it.identifier.toName() to it.reference.convert()) }
    headers.forEach { add(it.identifier.toName() to it.reference.convert()) }
    requests.first().content?.let { add(Name.of("body") to it.reference.convert()) }
}

fun EndpointWirespec.Response.responseParameters(): List<Pair<Name, Type>> = buildList {
    headers.forEach { add(it.identifier.toName() to it.reference.convert()) }
    content?.let { add(Name.of("body") to it.reference.convert()) }
}
