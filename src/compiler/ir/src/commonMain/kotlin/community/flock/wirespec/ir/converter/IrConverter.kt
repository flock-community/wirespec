package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.flattenListDict
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.ir.core.ArrayIndexCall
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.EnumReference
import community.flock.wirespec.ir.core.EnumValueCall
import community.flock.wirespec.ir.core.ErrorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FlatMapIndexed
import community.flock.wirespec.ir.core.FunctionBuilder
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.IfExpression
import community.flock.wirespec.ir.core.InterfaceBuilder
import community.flock.wirespec.ir.core.Lambda
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
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.Precision
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.StringTemplate
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.TypeDescriptor
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.`interface`
import community.flock.wirespec.ir.core.transformMatchingElements
import community.flock.wirespec.compiler.core.parse.ast.Channel as ChannelWirespec
import community.flock.wirespec.compiler.core.parse.ast.Definition as DefinitionWirespec
import community.flock.wirespec.compiler.core.parse.ast.Endpoint as EndpointWirespec
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumWirespec
import community.flock.wirespec.compiler.core.parse.ast.Field as FieldWirespec
import community.flock.wirespec.compiler.core.parse.ast.Graphql as GraphqlWirespec
import community.flock.wirespec.compiler.core.parse.ast.Reference as ReferenceWirespec
import community.flock.wirespec.compiler.core.parse.ast.Refined as RefinedWirespec
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
    is GraphqlWirespec -> convert()
}

fun PackageName.convert(): File = file("Wirespec") {
    `package`(value)
    namespace("Wirespec") {
        `interface`("Model")
        `interface`("Shape") {
            extends(Type.Custom("Model"))
            function("validate") {
                returnType(list(string))
            }
        }
        `interface`("Enum") {
            extends(Type.Custom("Model"))
            field("label", string)
        }
        `interface`("Refined") {
            extends(Type.Custom("Model"))
            typeParam(type("T"))
            field("value", type("T"))
            function("validate") {
                returnType(boolean)
            }
        }
        `interface`("Endpoint")
        `interface`("Channel")
        `interface`("GraphQL")
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
        struct(Name("Graph", "QL", "Error", "Location")) {
            field("line", integer)
            field("column", integer)
        }
        struct(Name("Graph", "QL", "Error")) {
            field("message", string)
            field("locations", list(type("GraphQLErrorLocation")).nullable())
            field("path", list(Type.Any).nullable())
            field("extensions", Type.Nullable(Type.Any))
        }
        `interface`("Cancellable") {
            function("cancel") {
                returnType(unit)
            }
        }
        `interface`(Name("Stream", "Transportation")) {
            function("stream") {
                returnType(type("Cancellable"))
                arg("request", type("RawRequest"))
                arg(Name("on", "Next"), Type.Function(listOf(type("RawResponse")), unit))
            }
        }
        `interface`("GeneratorField", isSealed = true) {
            // T is upper-bounded at `Any?` (i.e. unbounded) so GeneratorFieldNullable<T>
            // can extend GeneratorField<T?>.
            typeParam(type("T"), Type.Nullable(Type.Any))
        }
        struct("GeneratorFieldString") {
            implements(type("GeneratorField", string))
            field("regex", string.nullable())
            field("annotations", list(dict(string, Type.Any)))
        }
        struct("GeneratorFieldInteger64") {
            implements(type("GeneratorField", integer64))
            field("min", integer64.nullable())
            field("max", integer64.nullable())
            field("annotations", list(dict(string, Type.Any)))
        }
        struct("GeneratorFieldInteger32") {
            implements(type("GeneratorField", integer32))
            field("min", integer32.nullable())
            field("max", integer32.nullable())
            field("annotations", list(dict(string, Type.Any)))
        }
        struct("GeneratorFieldNumber64") {
            implements(type("GeneratorField", number64))
            field("min", number64.nullable())
            field("max", number64.nullable())
            field("annotations", list(dict(string, Type.Any)))
        }
        struct("GeneratorFieldNumber32") {
            implements(type("GeneratorField", number32))
            field("min", number32.nullable())
            field("max", number32.nullable())
            field("annotations", list(dict(string, Type.Any)))
        }
        struct("GeneratorFieldBoolean") {
            implements(type("GeneratorField", boolean))
            field("annotations", list(dict(string, Type.Any)))
        }
        struct("GeneratorFieldBytes") {
            implements(type("GeneratorField", bytes))
            field("annotations", list(dict(string, Type.Any)))
        }
        struct("GeneratorFieldEnum") {
            implements(type("GeneratorField", string))
            field("values", list(string))
            field("annotations", list(dict(string, Type.Any)))
            field("type", reflect)
        }
        struct("GeneratorFieldUnion") {
            implements(type("GeneratorField", string))
            field("variants", list(string))
            field("annotations", list(dict(string, Type.Any)))
            field("type", reflect)
        }
        struct("GeneratorFieldArray") {
            // `generate` takes the path-for-this-element and returns a single
            // element; the seeded generator decides how many elements to put in
            // the resulting List<T> and what indices to attach to their paths.
            typeParam(type("T"))
            implements(type("GeneratorField", list(type("T"))))
            field("generate", Type.Function(listOf(list(string)), type("T")))
        }
        struct("GeneratorFieldNullable") {
            typeParam(type("T"))
            implements(type("GeneratorField", type("T").nullable()))
            field("generate", Type.Function(listOf(list(string)), type("T")))
        }
        struct("GeneratorFieldShape") {
            typeParam(type("T"))
            implements(type("GeneratorField", type("T")))
            field("annotations", dict(string, list(dict(string, Type.Any))))
            field("generate", Type.Function(listOf(list(string)), type("T")))
            field("type", reflect)
        }
        struct("GeneratorFieldDict") {
            // `generate` takes the path-for-this-entry and returns a single
            // value; the seeded generator decides keys and entry count.
            typeParam(type("V"))
            implements(type("GeneratorField", dict(string, type("V"))))
            field("generate", Type.Function(listOf(list(string)), type("V")))
        }
        `interface`("Generator") {
            function("generate") {
                // T is unbounded (`Any?`) so the result type can be nullable.
                typeParam(type("T"), Type.Nullable(Type.Any))
                returnType(type("T"))
                arg("path", list(string))
                arg("field", type("GeneratorField", type("T")))
            }
        }
    }
}

fun PackageName.convertClientServer(): List<Element> = listOf(
    `interface`("ServerEdge") {
        typeParam(type("Req"), type("Request", Type.Wildcard))
        typeParam(type("Res"), type("Response", Type.Wildcard))
        function("from") {
            returnType(type("Req"))
            arg("request", type("RawRequest"))
        }
        function("to") {
            returnType(type("RawResponse"))
            arg("response", type("Res"))
        }
    },
    `interface`("ClientEdge") {
        typeParam(type("Req"), type("Request", Type.Wildcard))
        typeParam(type("Res"), type("Response", Type.Wildcard))
        function("to") {
            returnType(type("RawRequest"))
            arg("request", type("Req"))
        }
        function("from") {
            returnType(type("Res"))
            arg("response", type("RawResponse"))
        }
    },
    `interface`("Client") {
        typeParam(type("Req"), type("Request", Type.Wildcard))
        typeParam(type("Res"), type("Response", Type.Wildcard))
        field("pathTemplate", Type.String)
        field("method", Type.String)
        function("client") {
            returnType(type("ClientEdge", type("Req"), type("Res")))
            arg("serialization", type("Serialization"))
        }
    },
    `interface`("Server") {
        typeParam(type("Req"), type("Request", Type.Wildcard))
        typeParam(type("Res"), type("Response", Type.Wildcard))
        field("pathTemplate", Type.String)
        field("method", Type.String)
        function("server") {
            returnType(type("ServerEdge", type("Req"), type("Res")))
            arg("serialization", type("Serialization"))
        }
    },
)

private fun Identifier.toName(): Name = when (this) {
    is FieldIdentifier -> {
        // Tokenize into runs of separators (dashes, dots, spaces) and runs of non-separators,
        // keeping the separators as their own parts. This lets `Name.value()` reconstruct the
        // original wire name (e.g. `house-number`) so emitters that preserve it can, while the
        // casing helpers (camelCase/pascalCase/snakeCase) still work because they operate on
        // `wordParts()`, which ignores separator-only parts.
        val parts = Regex("[.\\s-]+|[^.\\s-]+").findAll(value).map { it.value }.toList()
        Name(parts)
    }
    is DefinitionIdentifier -> Name(
        Name.of(value).parts.filter { part -> part.any { it.isLetterOrDigit() } },
    )
}

fun TypeWirespec.convert() = file(identifier.toName()) {
    struct(identifier.toName()) {
        implements(Type.Custom("Wirespec.Shape"))
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
                        else -> {}
                    }
                }
            }
            else -> {}
        }
    }
}

fun EnumWirespec.convert() = file(identifier.toName()) {
    enum(identifier.toName(), Type.Custom("Wirespec.Enum")) {
        entries.forEach { entry(it, "\"$it\"") }
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
        function("toString") {
            returnType(Type.String)
            returns(
                if (reference.type is ReferenceWirespec.Primitive.Type.String) {
                    VariableReference(Name.of("value"))
                } else {
                    FunctionCall(
                        receiver = VariableReference(Name.of("value")),
                        name = Name.of("toString"),
                    )
                },
            )
        }
    }
}

fun ChannelWirespec.convert() = file(identifier.toName()) {
    namespace(identifier.toName(), type("Wirespec.Channel")) {
        `interface`("Sender") {
            function(identifier.toName()) {
                arg("message", reference.convert())
                returnType(unit)
            }
        }
        `interface`("Listener") {
            function(identifier.toName()) {
                arg("handler", Type.Function(listOf(reference.convert()), unit))
                returnType(unit)
            }
        }
    }
}

fun GraphqlWirespec.convert(module: Module? = null): File {
    val graphql = this
    val name = identifier.toName()
    val document = buildDocument(module)
    val allInputs = inputs + document.liftedInputs
    val outputType = output.convert()

    return file(name) {
        namespace(name, type("Wirespec.GraphQL")) {
            struct("Input") {
                allInputs.forEach { field(it.identifier.toName(), it.reference.convert()) }
            }
            struct("Data") {
                field(graphql.operation.toName(), outputType)
            }
            struct("Result") {
                field("data", type("Data").nullable())
                field("errors", list(type("Wirespec.GraphQLError")).nullable())
            }
            struct("RequestBody") {
                field("query", string)
                field("variables", type("Input"))
            }
            function(Name.of("document"), isStatic = true) {
                returnType(string)
                returns(Literal(document.text, Type.String))
            }
            function(Name("to", "Raw", "Request"), isStatic = true) {
                returnType(type("Wirespec.RawRequest"))
                arg("serialization", type("Wirespec.Serializer"))
                arg("input", type("Input"))
                returns(
                    construct(type("Wirespec.RawRequest")) {
                        arg("method", Literal("POST", Type.String))
                        arg("path", LiteralList(listOf(Literal("graphql", Type.String)), Type.String))
                        arg("queries", LiteralMap(emptyMap(), Type.String, Type.Custom("List<String>")))
                        arg("headers", LiteralMap(emptyMap(), Type.String, Type.Custom("List<String>")))
                        arg(
                            "body",
                            NullableOf(
                                FunctionCall(
                                    receiver = VariableReference(Name.of("serialization")),
                                    name = Name("serialize", "Body"),
                                    typeArguments = listOf(Type.Custom("RequestBody")),
                                    arguments = mapOf(
                                        Name.of("value") to ConstructorStatement(
                                            type = Type.Custom("RequestBody"),
                                            namedArguments = mapOf(
                                                Name.of("query") to FunctionCall(name = Name.of("document"), arguments = emptyMap()),
                                                Name.of("variables") to VariableReference(Name.of("input")),
                                            ),
                                        ),
                                        Name.of("type") to TypeDescriptor(Type.Custom("RequestBody")),
                                    ),
                                ),
                            ),
                        )
                    },
                )
            }
            function(Name("from", "Raw", "Response"), isStatic = true) {
                returnType(type("Result"))
                arg("serialization", type("Wirespec.Deserializer"))
                arg("response", type("Wirespec.RawResponse"))
                returns(
                    NullableMap(
                        expression = FieldCall(VariableReference(Name.of("response")), Name.of("body")),
                        body = FunctionCall(
                            receiver = VariableReference(Name.of("serialization")),
                            name = Name("deserialize", "Body"),
                            typeArguments = listOf(Type.Custom("Result")),
                            arguments = mapOf(
                                Name.of("value") to VariableReference(Name.of("it")),
                                Name.of("type") to TypeDescriptor(Type.Custom("Result")),
                            ),
                        ),
                        alternative = ErrorStatement(Literal("body is null", Type.String)),
                    ),
                )
            }
            `interface`("Handler") {
                extends(type("Wirespec.Handler"))
                graphqlHandlerFunction(graphql, name)
            }
            `interface`("Call") {
                extends(type("Wirespec.Call"))
                graphqlHandlerFunction(graphql, name)
            }
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

            // Individual response records (Response200, Response201, etc.) with hoisted Response<Status>Headers
            endpoint.responses.distinctBy { it.status }.forEach { response ->
                val bodyType = response.content?.reference?.convert() ?: Type.Unit
                val statusCode = response.status.toIntOrNull() ?: 0
                val statusClassName = response.status.replaceFirstChar { it.uppercaseChar() }
                val statusPrefix = response.status.first()
                val contentTypeName = bodyType.toTypeName()
                val headersName = "Response${statusClassName}Headers"
                struct(headersName) {
                    implements(type("Wirespec.Response.Headers"))
                    response.headers.forEach { field(it.identifier.toName(), it.reference.convert()) }
                }
                struct("Response$statusClassName") {
                    implements(type("Response${statusPrefix}XX", bodyType))
                    implements(type("Response$contentTypeName"))
                    field("status", Type.IntegerLiteral(statusCode), isOverride = true)
                    field("headers", type(headersName), isOverride = true)
                    field("body", bodyType, isOverride = true)
                    constructo {
                        response.responseParameters().forEach { (name, type) -> arg(name, type) }
                        assign("status", Literal(statusCode, Type.Integer(Precision.P32)))
                        assign(
                            "headers",
                            construct(type(headersName)) {
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
                                    caseSensitive = false,
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
                                                    caseSensitive = false,
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
    is Type.Custom -> name.pascalCase()
    is Type.Array -> "List${elementType.toTypeName()}"
    is Type.Nullable -> "Optional${type.toTypeName()}"
    is Type.String -> "String"
    is Type.Integer -> "Integer"
    is Type.Number -> "Number"
    is Type.Boolean -> "Boolean"
    is Type.Bytes -> "Bytes"
    is Type.Dict -> "Map"
    is Type.IntegerLiteral -> "Integer"
    is Type.StringLiteral -> "String"
    is Type.Function -> "Function"
}

fun ReferenceWirespec.convert(): Type = when (this) {
    is ReferenceWirespec.Any -> Type.Any
    is ReferenceWirespec.Custom -> Type.Custom(Name.of(value).pascalCase())
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
    caseSensitive: Boolean = true,
): Expression {
    val type = field.reference.copy(isNullable = false)
    val getCall = ArrayIndexCall(
        receiver = map,
        index = Literal(fieldName, Type.String),
        caseSensitive = caseSensitive,
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

fun EndpointWirespec.convertEndpointClient(): File {
    val endpointName = identifier.toName()
    val endpointNameStr = endpointName.value()

    return file(Name.of("${endpointNameStr}Client")) {
        struct(Name.of("${endpointNameStr}Client")) {
            field("serialization", Type.Custom("Wirespec.Serialization"))
            field("transportation", Type.Custom("Wirespec.Transportation"))
            implements(Type.Custom("$endpointNameStr.Call"))

            asyncFunction(endpointName, isOverride = true) {
                requestParameters().forEach { (name, type) -> arg(name, type) }
                returnType(Type.Custom("$endpointNameStr.Response", listOf(Type.Wildcard)))

                assign(
                    "request",
                    ConstructorStatement(
                        type = Type.Custom("$endpointNameStr.Request"),
                        namedArguments = requestParameters().associate { (name, _) ->
                            name to VariableReference(name)
                        },
                    ),
                )

                assign(
                    "rawRequest",
                    FunctionCall(
                        name = Name(listOf("$endpointNameStr.toRawRequest")),
                        arguments = mapOf(
                            Name.of("serialization") to FieldCall(field = Name.of("serialization")),
                            Name.of("request") to VariableReference(Name.of("request")),
                        ),
                    ),
                )

                assign(
                    "rawResponse",
                    FunctionCall(
                        name = Name.of("transport"),
                        receiver = FieldCall(field = Name.of("transportation")),
                        arguments = mapOf(
                            Name.of("request") to VariableReference(Name.of("rawRequest")),
                        ),
                    ),
                )

                returns(
                    FunctionCall(
                        name = Name(listOf("$endpointNameStr.fromRawResponse")),
                        arguments = mapOf(
                            Name.of("serialization") to FieldCall(field = Name.of("serialization")),
                            Name.of("response") to VariableReference(Name.of("rawResponse")),
                        ),
                    ),
                )
            }
        }
    }
}

fun List<EndpointWirespec>.convertClient(graphqls: List<GraphqlWirespec> = emptyList()): File {
    val endpoints = this
    return file(Name.of("Client")) {
        struct(Name.of("Client")) {
            field("serialization", Type.Custom("Wirespec.Serialization"))
            field("transportation", Type.Custom("Wirespec.Transportation"))

            endpoints.forEach { endpoint ->
                implements(Type.Custom("${endpoint.identifier.toName().value()}.Call"))
            }

            endpoints.forEach { endpoint ->
                val endpointName = endpoint.identifier.toName()
                val endpointNameStr = endpointName.value()

                asyncFunction(endpointName, isOverride = true) {
                    endpoint.requestParameters().forEach { (name, type) -> arg(name, type) }
                    returnType(Type.Custom("$endpointNameStr.Response", listOf(Type.Wildcard)))

                    returns(
                        FunctionCall(
                            name = Name(listOf(endpointName.camelCase())),
                            receiver = ConstructorStatement(
                                type = Type.Custom("${endpointNameStr}Client"),
                                namedArguments = mapOf(
                                    Name.of("serialization") to FieldCall(field = Name.of("serialization")),
                                    Name.of("transportation") to FieldCall(field = Name.of("transportation")),
                                ),
                            ),
                            arguments = endpoint.requestParameters().associate { (name, _) ->
                                name to VariableReference(name)
                            },
                        ),
                    )
                }
            }

            graphqls.forEach { graphql ->
                implements(Type.Custom("${graphql.identifier.toName().value()}.Call"))
            }

            graphqls.forEach { graphql ->
                val graphqlName = graphql.identifier.toName()
                val graphqlNameStr = graphqlName.value()

                asyncFunction(graphqlName, isOverride = true) {
                    arg("input", Type.Custom("$graphqlNameStr.Input"))
                    returnType(Type.Custom("$graphqlNameStr.Result"))

                    returns(
                        FunctionCall(
                            name = Name(listOf(graphqlName.camelCase())),
                            receiver = ConstructorStatement(
                                type = Type.Custom("${graphqlNameStr}Client"),
                                namedArguments = mapOf(
                                    Name.of("serialization") to FieldCall(field = Name.of("serialization")),
                                    Name.of("transportation") to FieldCall(field = Name.of("transportation")),
                                ),
                            ),
                            arguments = mapOf(Name.of("input") to VariableReference(Name.of("input"))),
                        ),
                    )
                }
            }
        }
    }
}

private data class GraphqlDocument(val text: String, val liftedInputs: List<FieldWirespec>)

private fun GraphqlWirespec.buildDocument(module: Module?): GraphqlDocument {
    val lifted = mutableListOf<FieldWirespec>()
    val selection = output.selection(module, emptySet(), emptyList(), lifted)
    val allInputs = inputs + lifted
    val variableDefinitions = allInputs
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { "$${it.identifier.value}: ${it.reference.toGraphqlType()}" }
        .orEmpty()
    val operationArguments = inputs
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { "${it.identifier.value}: $${it.identifier.value}" }
        .orEmpty()
    val text = "${kind.name.lowercase()} ${identifier.toName().value()}$variableDefinitions" +
        " { ${operation.value}$operationArguments${selection?.let { " $it" }.orEmpty()} }"
    return GraphqlDocument(text, lifted)
}

private fun ReferenceWirespec.selection(
    module: Module?,
    visited: Set<String>,
    path: List<String>,
    lifted: MutableList<FieldWirespec>,
): String? = when (val root = flattenListDict()) {
    is ReferenceWirespec.Custom -> when (val definition = module?.statements?.find { it.identifier.value == root.value }) {
        is TypeWirespec ->
            if (root.value in visited) {
                "{ __typename }"
            } else {
                definition.shape.value
                    .joinToString(" ") { it.selection(module, visited + root.value, path, lifted) }
                    .let { "{ $it }" }
            }

        is UnionWirespec ->
            definition.entries
                .joinToString(" ") { entry ->
                    "... on ${entry.value}${entry.selection(module, visited + root.value, path, lifted)?.let { " $it" }.orEmpty()}"
                }
                .let { "{ __typename $it }" }

        else -> null
    }

    else -> null
}

private fun FieldWirespec.selection(
    module: Module?,
    visited: Set<String>,
    path: List<String>,
    lifted: MutableList<FieldWirespec>,
): String {
    val fieldPath = path + identifier.value
    val arguments = parameters
        .filter { !it.reference.isNullable }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { parameter ->
            val variableName = (fieldPath + parameter.identifier.value).toDromedaryPath()
            lifted += FieldWirespec(
                annotations = emptyList(),
                identifier = FieldIdentifier(variableName),
                reference = parameter.reference,
            )
            "${parameter.identifier.value}: $$variableName"
        }
        .orEmpty()
    return "${identifier.value}$arguments${reference.selection(module, visited, fieldPath, lifted)?.let { " $it" }.orEmpty()}"
}

private fun List<String>.toDromedaryPath(): String = mapIndexed { index, part ->
    if (index == 0) part else part.replaceFirstChar(Char::uppercaseChar)
}.joinToString("")

private fun ReferenceWirespec.toGraphqlType(): String = when (this) {
    is ReferenceWirespec.Custom -> value
    is ReferenceWirespec.Iterable -> "[${reference.toGraphqlType()}]"
    is ReferenceWirespec.Primitive -> when (type) {
        is ReferenceWirespec.Primitive.Type.String -> "String"
        is ReferenceWirespec.Primitive.Type.Integer -> "Int"
        is ReferenceWirespec.Primitive.Type.Number -> "Float"
        is ReferenceWirespec.Primitive.Type.Boolean -> "Boolean"
        is ReferenceWirespec.Primitive.Type.Bytes -> "String"
    }

    is ReferenceWirespec.Any, is ReferenceWirespec.Unit, is ReferenceWirespec.Dict ->
        error("Reference '$this' cannot be used as a GraphQL variable type")
}.let { if (isNullable) it else "$it!" }

private fun InterfaceBuilder.graphqlHandlerFunction(graphql: GraphqlWirespec, name: Name) = when (graphql.kind) {
    GraphqlWirespec.Kind.Subscription -> function(name) {
        arg("input", Type.Custom("Input"))
        arg(Name("on", "Next"), Type.Function(listOf(Type.Custom("Result")), Type.Unit))
        returnType(Type.Custom("Wirespec.Cancellable"))
    }

    GraphqlWirespec.Kind.Query, GraphqlWirespec.Kind.Mutation -> asyncFunction(name) {
        arg("input", Type.Custom("Input"))
        returnType(Type.Custom("Result"))
    }
}

fun GraphqlWirespec.convertGraphqlClient(): File {
    val graphql = this
    val name = identifier.toName()
    val nameStr = name.value()

    return file(Name.of("${nameStr}Client")) {
        struct(Name.of("${nameStr}Client")) {
            field("serialization", Type.Custom("Wirespec.Serialization"))
            when (graphql.kind) {
                GraphqlWirespec.Kind.Subscription -> field("transportation", Type.Custom("Wirespec.StreamTransportation"))
                else -> field("transportation", Type.Custom("Wirespec.Transportation"))
            }
            implements(Type.Custom("$nameStr.Call"))

            when (graphql.kind) {
                GraphqlWirespec.Kind.Subscription -> function(name, isOverride = true) {
                    arg("input", Type.Custom("$nameStr.Input"))
                    arg(Name("on", "Next"), Type.Function(listOf(Type.Custom("$nameStr.Result")), Type.Unit))
                    returnType(Type.Custom("Wirespec.Cancellable"))
                    assignRawRequest(nameStr)
                    returns(
                        FunctionCall(
                            receiver = FieldCall(field = Name.of("transportation")),
                            name = Name.of("stream"),
                            arguments = mapOf(
                                Name.of("request") to VariableReference(Name.of("rawRequest")),
                                Name("on", "Next") to Lambda(
                                    parameters = listOf(Parameter(Name.of("raw"), Type.Custom("Wirespec.RawResponse"))),
                                    body = FunctionCall(
                                        name = Name(listOf("onNext")),
                                        arguments = mapOf(
                                            Name.of("value") to FunctionCall(
                                                name = Name(listOf("$nameStr.fromRawResponse")),
                                                arguments = mapOf(
                                                    Name.of("serialization") to FieldCall(field = Name.of("serialization")),
                                                    Name.of("response") to VariableReference(Name.of("raw")),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    )
                }

                GraphqlWirespec.Kind.Query, GraphqlWirespec.Kind.Mutation -> asyncFunction(name, isOverride = true) {
                    arg("input", Type.Custom("$nameStr.Input"))
                    returnType(Type.Custom("$nameStr.Result"))
                    assignRawRequest(nameStr)
                    assign(
                        "rawResponse",
                        FunctionCall(
                            receiver = FieldCall(field = Name.of("transportation")),
                            name = Name.of("transport"),
                            arguments = mapOf(
                                Name.of("request") to VariableReference(Name.of("rawRequest")),
                            ),
                        ),
                    )
                    returns(
                        FunctionCall(
                            name = Name(listOf("$nameStr.fromRawResponse")),
                            arguments = mapOf(
                                Name.of("serialization") to FieldCall(field = Name.of("serialization")),
                                Name.of("response") to VariableReference(Name.of("rawResponse")),
                            ),
                        ),
                    )
                }
            }
        }
    }
}

private fun FunctionBuilder.assignRawRequest(nameStr: String) = assign(
    "rawRequest",
    FunctionCall(
        name = Name(listOf("$nameStr.toRawRequest")),
        arguments = mapOf(
            Name.of("serialization") to FieldCall(field = Name.of("serialization")),
            Name.of("input") to VariableReference(Name.of("input")),
        ),
    ),
)

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
