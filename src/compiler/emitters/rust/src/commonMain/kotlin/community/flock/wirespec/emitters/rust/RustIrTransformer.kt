package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.core.ir.ArrayIndexCall
import community.flock.wirespec.compiler.core.ir.Case
import community.flock.wirespec.compiler.core.ir.Cast
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.ErrorStatement
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.LiteralList
import community.flock.wirespec.compiler.core.ir.LiteralMap
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.NullCheck
import community.flock.wirespec.compiler.core.ir.NullableMap
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.Precision
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.ReturnStatement
import community.flock.wirespec.compiler.core.ir.Statement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Switch
import community.flock.wirespec.compiler.core.ir.Transformer
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.converter.convertConstraint
import community.flock.wirespec.compiler.core.ir.findElement
import community.flock.wirespec.compiler.core.ir.function
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.ir.transformChildren
import community.flock.wirespec.compiler.core.ir.transformer
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.ir.Function as LanguageFunction
import community.flock.wirespec.compiler.core.ir.Type as LanguageType
import community.flock.wirespec.compiler.core.ir.Union as LanguageUnion
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumDefinition
import community.flock.wirespec.compiler.core.parse.ast.Refined as RefinedDefinition
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionDefinition

private val rustSelfParam = Parameter(Name.of("&self"), LanguageType.Custom(""))
private val rustResponsePattern = Regex("Response(\\d+|Default)")
private val serializerTraits = setOf("Serializer", "Deserializer")
private val serializationFunctions = setOf(
    "serialize_path",
    "serialize_param",
    "serialize_body",
    "deserialize_path",
    "deserialize_param",
    "deserialize_body",
)

internal fun String.toSnakeCase(): String = Name.of(this).snakeCase()

/** `self`, `Self`, `super` and `crate` cannot be written as a raw identifier such as `r#self`. */
internal val unescapableKeywords = setOf("self", "Self", "super", "crate")

internal fun String.sanitizeKeywords(): String = when (this) {
    in unescapableKeywords -> "${this}_"
    in RustEmitter.reservedKeywords -> "r#$this"
    else -> this
}

/**
 * Function parameters borrow strings as `&str` and (de)serializers as `&impl`, and the value handed to a
 * (de)serialization function is passed by reference.
 */
internal fun <T : Element> T.applyBorrowRules(): T = transform {
    apply(borrowParameters)
    apply(borrowSerializationArguments)
}

private val borrowParameters: Transformer = transformer {
    element { element, transformer ->
        when {
            // The user-facing `Call` trait takes owned values, so they can be moved into the `Request` constructor.
            element is Interface && element.name == Name.of("Call") -> element
            element is LanguageFunction -> element.copy(parameters = element.parameters.map { it.copy(type = it.type.borrowed()) }).transformChildren(transformer)
            else -> element.transformChildren(transformer)
        }
    }
}

private fun LanguageType.borrowed(): LanguageType = when {
    this is LanguageType.Nullable -> copy(type = type.borrowed())
    this == LanguageType.String -> LanguageType.Custom("&str")
    this is LanguageType.Custom && name.pascalCase() in serializerTraits -> copy(name = Name(listOf("&impl ${name.pascalCase()}")))
    else -> this
}

private val borrowSerializationArguments: Transformer = transformer {
    statementAndExpression { statement, transformer ->
        if (statement is FunctionCall && statement.name.snakeCase() in serializationFunctions) {
            statement.copy(
                arguments = statement.arguments.entries.mapIndexed { index, (name, value) ->
                    // `it` is bound by `.as_ref().map(|it| ...)`, so it already is a reference.
                    name to transformer.transformExpression(value).let { argument ->
                        if (index == 0 && !(argument is VariableReference && argument.name.value() == "it")) RawExpression("&${argument.toRawCode()}") else argument
                    }
                }.toMap(),
            )
        } else {
            statement.transformChildren(transformer)
        }
    }
}

private fun Expression.toRawCode(): String = when (this) {
    is VariableReference -> name.snakeCase().sanitizeKeywords()
    is FieldCall -> "${receiver?.let { "${it.toRawCode()}." }.orEmpty()}${field.snakeCase().sanitizeKeywords()}"
    is ArrayIndexCall -> {
        val key = (index as? Literal)?.takeIf { it.type is LanguageType.String }?.let { "\"${it.value}\"" } ?: index.toRawCode()
        "${receiver.toRawCode()}[$key]"
    }
    is ConstructorStatement -> {
        val typeName = (type as? LanguageType.Custom)?.name?.pascalCase() ?: type.toString()
        val args = namedArguments.entries.joinToString(", ") { "${it.key.snakeCase()}: ${it.value.toRawCode()}" }
        if (args.isEmpty()) "$typeName {}" else "$typeName { $args }"
    }
    is Literal -> if (type is LanguageType.String) "String::from(\"$value\")" else "$value"
    is RawExpression -> code
    else -> error("Unsupported expression type in toRawCode: ${this::class.simpleName}")
}

/** The Rust spelling of a type in the hand-written client code, which does not go through the generator. */
internal fun LanguageType.rustName(): String = when (this) {
    is LanguageType.Custom -> if (generics.isEmpty()) name.pascalCase() else "${name.pascalCase()}<${generics.joinToString(", ") { it.rustName() }}>"
    LanguageType.String -> "String"
    is LanguageType.Nullable -> "Option<${type.rustName()}>"
    is LanguageType.Array -> "Vec<${elementType.rustName()}>"
    is LanguageType.Dict -> "std::collections::HashMap<${keyType.rustName()}, ${valueType.rustName()}>"
    LanguageType.Any -> "AnyValue"
    LanguageType.Bytes -> "Vec<u8>"
    LanguageType.Unit -> "()"
    LanguageType.Boolean -> "bool"
    LanguageType.Wildcard -> "_"
    LanguageType.Reflect -> "std::any::TypeId"
    is LanguageType.Integer -> when (precision) {
        Precision.P32 -> "i32"
        Precision.P64 -> "i64"
    }
    is LanguageType.Number -> when (precision) {
        Precision.P32 -> "f32"
        Precision.P64 -> "f64"
    }
    is LanguageType.IntegerLiteral -> "i32"
    is LanguageType.StringLiteral -> "String"
    is LanguageType.Function -> "Box<dyn Fn(${parameterTypes.joinToString(", ") { it.rustName() }}) -> ${returnType.rustName()}>"
}

/**
 * A parameter such as `self` is renamed to `self_`, but a reference to it is not: it cannot be told apart from the
 * receiver of a method. A constructor has no receiver, so there every such reference is to a parameter.
 */
internal fun <T : Element> T.withRenamedConstructorArguments(): T = transform {
    apply(transformer { constructor { constructor, _ -> constructor.transformChildren(renamedUnescapableReferences) } })
}

private val renamedUnescapableReferences: Transformer = transformer {
    statementAndExpression { statement, transformer ->
        if (statement is VariableReference && statement.name.value() in unescapableKeywords) {
            VariableReference(Name(listOf("${statement.name.value()}_")))
        } else {
            statement.transformChildren(transformer)
        }
    }
}

/** The traits of a channel and an rpc borrow their implementation, so it can hold a connection or other state. */
internal fun File.withSelfReceivers(): File = transform { matchingElements<Interface> { it.withSelfReceivers() } }

/** `AnyValue` is the shared type for a value of type `Any`: unlike `Box<dyn Any>` it can be cloned, defaulted and compared. */
internal fun <T : Element> T.withAnyValues(): T = transform { matching<LanguageType.Any> { LanguageType.Custom("AnyValue") } }

internal fun Interface.withSelfReceivers(): Interface = copy(
    elements = elements.map { if (it is LanguageFunction) it.copy(parameters = listOf(rustSelfParam) + it.parameters) else it },
)

internal fun <T : Element> T.convertSimpleRawExpressionsToVariableRefs(): T = transform {
    val identifierPattern = Regex("[a-zA-Z_][a-zA-Z0-9_]*")
    statementAndExpression { s, t ->
        if (s is RawExpression && identifierPattern.matches(s.code)) VariableReference(Name.of(s.code)) else s.transformChildren(t)
    }
}

internal fun <T : Element> T.stripWirespecPrefix(): T = transform {
    matching<LanguageType.Custom> { type ->
        val pascalName = type.name.pascalCase()
        if (pascalName.startsWith("Wirespec.")) type.copy(name = Name.of(pascalName.removePrefix("Wirespec."))) else type
    }
}

internal fun File.replaceStructWithRefinedFunctions(refined: Refined): File = transform {
    matchingElements { s: Struct ->
        s.copy(elements = listOf(refined.validateFunction(), RawElement(refined.stringConversions())))
    }
}

private fun Refined.validateFunction(): LanguageFunction = function("validate") {
    arg("&self", LanguageType.Custom(""))
    returnType(LanguageType.Boolean)
    returns(reference.convertConstraint(FieldCall(VariableReference(Name.of("self")), Name.of("value"))))
}

/**
 * `Display` and `FromStr`, which the path (de)serializers require of a path parameter. Both delegate to the wrapped
 * value, so `from_str` parses without validating, like constructing the struct does.
 */
private fun Refined.stringConversions(): String = identifier.value.let { name ->
    """
    impl std::fmt::Display for $name {
        fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
            write!(f, "{}", self.value)
        }
    }
    impl std::str::FromStr for $name {
        type Err = <${reference.convert().rustName()} as std::str::FromStr>::Err;
        fn from_str(s: &str) -> Result<Self, Self::Err> {
            s.parse().map(|value| $name { value })
        }
    }
    """.trimIndent()
}

internal fun File.flattenForRust(): File {
    val namespace = findElement<Namespace>()!!
    val (moduleElements, classElements) = namespace.elements.partition { it is Struct || it is LanguageUnion }
    val responseMembers = namespace.elements
        .filterIsInstance<Struct>()
        .map { it.name.pascalCase() }
        .filter { rustResponsePattern.matches(it) }
        .map { LanguageType.Custom(it) }
    return File(
        namespace.name,
        moduleElements.map { element ->
            when (element) {
                is LanguageUnion if element.name.pascalCase() == "Response" -> element.copy(members = responseMembers, typeParameters = emptyList())
                is LanguageUnion -> element.copy(typeParameters = emptyList())
                else -> element
            }
        } + Namespace(namespace.name, classElements, namespace.extends),
    )
}

internal fun fixResponseSwitchPatterns(): Transformer = transformer {
    statement { s, t ->
        val variable = (s as? Switch)?.variable?.takeIf { it.camelCase() == "r" }
        if (s is Switch && variable != null) {
            s.copy(
                expression = t.transformExpression(s.expression),
                cases = s.cases.map { it.matchingResponse(variable, t) },
                default = null,
            )
        } else {
            s.transformChildren(t)
        }
    }
}

private fun Case.matchingResponse(variable: Name, t: Transformer): Case = (type as? LanguageType.Custom)?.name?.pascalCase()
    ?.takeIf { rustResponsePattern.matches(it) }
    ?.let { Case(value = RawExpression("Response::$it(${variable.snakeCase()})"), body = body.map(t::transformStatement)) }
    ?: transformChildren(t)

internal fun fixConstructorCalls(): Transformer = transformer {
    statementAndExpression { s, t ->
        val typeName = ((s as? ConstructorStatement)?.type as? LanguageType.Custom)?.name?.pascalCase()
        when {
            s !is ConstructorStatement || typeName == null -> s.transformChildren(t)
            rustResponsePattern.matches(typeName) -> FunctionCall(
                name = Name(listOf("Response::$typeName")),
                arguments = mapOf(Name.of("inner") to FunctionCall(name = Name(listOf("$typeName::new")), arguments = s.transformedArguments(t))),
            )
            typeName == "Request" -> FunctionCall(name = Name(listOf("Request::new")), arguments = s.transformedArguments(t))
            else -> s.transformChildren(t)
        }
    }
}

private fun ConstructorStatement.transformedArguments(t: Transformer): Map<Name, Expression> = namedArguments.mapValues { t.transformExpression(it.value) }

/** `Handler` and `Call` become plain traits whose methods take `&self`. */
internal fun <T : Element> T.rustHandlerTraits(): T = transform {
    matchingElements<Interface> { iface ->
        if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) iface.copy(extends = emptyList()).withSelfReceivers() else iface
    }
}

internal fun <T : Element> T.stripResponseGenerics(): T = transform {
    matching<LanguageType.Custom> { type ->
        if (type.name.pascalCase().startsWith("Response") && type.generics.isNotEmpty()) type.copy(generics = emptyList()) else type
    }
}

/**
 * A raw request or response comes from outside, so reading it can fail: a parameter or the body is missing, a value
 * cannot be deserialized, or the status is not in the contract. The functions that read one return the error to the
 * caller instead of panicking, and so does a `Call`, which also has a transport that can fail.
 */
internal fun File.withErrorsReturned(): File = transform {
    apply(
        transformer {
            element { element, transformer ->
                when {
                    element is LanguageFunction && element.name.snakeCase() in readingFunctions -> element.copy(
                        returnType = element.returnType?.orError(),
                        body = element.body.map(returnedErrors::transformStatement),
                    )
                    element is Interface && element.name == Name.of("Call") -> element.copy(
                        elements = element.elements.map { if (it is LanguageFunction) it.copy(returnType = it.returnType?.orError()) else it },
                    )
                    else -> element.transformChildren(transformer)
                }
            }
        },
    )
}

private val readingFunctions = setOf("from_raw_request", "from_raw_response")

private const val ERROR = "WirespecError"

// Spelled out, so a model named `Result` cannot take its place.
private fun LanguageType.orError(): LanguageType = LanguageType.Custom(Name(listOf("std::result::Result<${rustName()}, $ERROR>")))

private fun Expression.orReturnError(): FunctionCall = FunctionCall(receiver = this, name = Name(listOf("?")))

private fun Expression.isDeserialization(): Boolean = this is FunctionCall && name.snakeCase() in deserializationFunctions

private val deserializationFunctions = setOf("deserialize_path", "deserialize_param", "deserialize_body")

private val returnedErrors: Transformer = transformer {
    statementAndExpression { statement, transformer ->
        when {
            statement is ReturnStatement -> ReturnStatement(FunctionCall(name = Name(listOf("Ok")), arguments = mapOf(Name.of("value") to transformer.transformExpression(statement.expression))))
            statement is ErrorStatement -> ReturnStatement(RawExpression("Err($ERROR::UnexpectedStatus(response.status_code))"))
            statement is NullCheck && statement.body.isDeserialization() -> statement.copy(alternative = null).orMissing(statement.alternative, statement.expression.missing())
            statement is NullableMap && statement.body.isDeserialization() -> NullCheck(statement.expression, statement.body, alternative = null).orMissing(statement.alternative, statement.expression.missing())
            statement is FunctionCall && statement.isDeserialization() -> statement.copy(arguments = statement.arguments.mapValues { it.value.orMissingSegment() }).orReturnError()
            else -> statement.transformChildren(transformer)
        }
    }
}

/**
 * The value is read inside `map`, which gives an `Option<Result<..>>`: `transpose` turns it inside out, so a value that
 * cannot be read is returned as an error. What is left is an `Option`, and an error when a required value is missing.
 */
private fun Expression.orMissing(alternative: Expression?, missing: String): Statement = FunctionCall(receiver = this, name = Name.of("transpose")).orReturnError()
    .let { read ->
        when (alternative) {
            null -> read
            is ErrorStatement -> FunctionCall(receiver = read, name = Name.of("ok_or"), arguments = mapOf(Name.of("error") to RawExpression(missing))).orReturnError()
            else -> FunctionCall(receiver = read, name = Name.of("unwrap_or"), arguments = mapOf(Name.of("default") to alternative))
        }
    }

private fun Expression.missing(): String = ((this as? ArrayIndexCall)?.index as? Literal)?.value
    ?.let { "$ERROR::MissingParameter(String::from(\"$it\"))" }
    ?: "$ERROR::MissingBody"

// A segment of the path is read by its index, which panics on a path that is too short.
private fun Expression.orMissingSegment(): Expression = (this as? ArrayIndexCall)
    ?.takeIf { it.index is Literal }
    ?.let { RawExpression("${it.receiver.toRawCode()}.get(${(it.index as Literal).value}).ok_or($ERROR::MissingPathSegment(${(it.index as Literal).value}))?") }
    ?: this

internal fun <T : Element> T.injectResponseFromImpls(): T = transform {
    matchingElements<File> { file ->
        file.copy(
            elements = file.elements.flatMap { element ->
                listOf(element) + when {
                    element is LanguageUnion && element.name.pascalCase() == "Response" -> element.members.map { member ->
                        val memberName = member.name.pascalCase()
                        RawElement("impl From<$memberName> for Response { fn from(value: $memberName) -> Self { Response::$memberName(value) } }\n")
                    }
                    else -> emptyList()
                }
            },
        )
    }
}

internal fun File.injectApiStruct(endpoint: Endpoint): File = transform {
    matchingElements<Namespace> { ns -> ns.copy(elements = ns.elements + RawElement(endpoint.apiStruct())) }
}

private fun Endpoint.apiStruct(): String {
    val pathTemplate = path.joinToString("/", prefix = "/") { segment ->
        when (segment) {
            is Endpoint.Segment.Literal -> segment.value
            is Endpoint.Segment.Param -> "{${segment.identifier.value}}"
        }
    }
    return """
        pub struct Api;
        impl Server for Api {
            type Req = Request;
            type Res = Response;
            fn path_template(&self) -> &'static str { "$pathTemplate" }
            fn method(&self) -> Method { Method::${method.name} }
        }
    """.trimIndent()
}

/**
 * The generator of a model is converted for languages with objects: it takes a `Wirespec.Generator`, calls the
 * generator of another model on an object, looks an enum up with `valueOf` and switches on a string. In Rust it
 * borrows any `Generator`, calls the associated function of the other generator, and a union wraps what the
 * generator of its member returns.
 */
internal fun File.toRustGenerator(model: Model): File = stripWirespecPrefix()
    .transform {
        parametersWhere({ it.type.isGenerator() }) { it.copy(type = LanguageType.Custom(Name(listOf("&impl Generator")))) }
        apply(associatedGeneratorCalls)
        apply(boxedAnnotationValues)
    }
    .let { file ->
        when (model) {
            is EnumDefinition -> file.transform { apply(enumFromLabel(model)) }
            is UnionDefinition -> file.transform { apply(unionOfMembers(model)) }
            is TypeDefinition, is RefinedDefinition -> file
        }
    }

private fun LanguageType.isGenerator(): Boolean = this is LanguageType.Custom && name.pascalCase() == "Generator"

private fun Expression.isGeneratorOfModel(): Boolean = this is RawExpression && code.endsWith("Generator")

internal val associatedGeneratorCalls: Transformer = transformer {
    statementAndExpression { statement, transformer ->
        val generator = ((statement as? FunctionCall)?.receiver as? RawExpression)?.takeIf { it.isGeneratorOfModel() }
        if (statement is FunctionCall && generator != null) {
            statement.copy(receiver = null, name = Name(listOf("${generator.code}::${statement.name.snakeCase()}"))).transformChildren(transformer)
        } else {
            statement.transformChildren(transformer)
        }
    }
}

// An annotation holds values of any type, a `Box<dyn Any>` in Rust, and a value does not turn into a box by itself.
private val boxedAnnotationValues: Transformer = transformer {
    statementAndExpression { statement, transformer ->
        when (val transformed = statement.transformChildren(transformer)) {
            is LiteralMap if transformed.valueType == LanguageType.Any -> transformed.copy(values = transformed.values.mapValues { it.value.boxed() })
            is LiteralList if transformed.type == LanguageType.Any -> transformed.copy(values = transformed.values.map { it.boxed() })
            else -> transformed
        }
    }
}

private fun Expression.boxed(): Expression = Cast(FunctionCall(name = Name(listOf("Box::new")), arguments = mapOf(Name.of("value") to this)), LanguageType.Any)

private fun Expression.asStr(): Expression = FunctionCall(receiver = this, name = Name.of("as_str"))

/** A Rust enum is looked up with `from_label`. An enum without entries has no `from_label` and no value to generate. */
private fun enumFromLabel(enum: EnumDefinition): Transformer = Name.of(enum.identifier.value).pascalCase().let { name ->
    transformer {
        statementAndExpression { statement, transformer ->
            when {
                statement is ReturnStatement && enum.entries.isEmpty() -> ErrorStatement(Literal("$name has no entries", LanguageType.String))
                statement is FunctionCall && statement.name == Name.of("valueOf") -> FunctionCall(
                    receiver = FunctionCall(
                        name = Name(listOf("$name::from_label")),
                        arguments = statement.arguments.mapValues { transformer.transformExpression(it.value).asStr() },
                    ),
                    name = Name.of("expect"),
                    arguments = mapOf(Name.of("message") to RawExpression("\"Unknown $name\"")),
                )
                else -> statement.transformChildren(transformer)
            }
        }
    }
}

/**
 * A `match` takes a string slice with string literals as patterns and needs a catch-all arm, which replaces the error
 * after the switch. Every arm wraps the member it generates in the variant of the union.
 */
private fun unionOfMembers(union: UnionDefinition): Transformer = Name.of(union.identifier.value).pascalCase().let { name ->
    transformer {
        element { element, transformer ->
            if (element is LanguageFunction) element.copy(body = element.body.filterNot { it is ErrorStatement }).transformChildren(transformer) else element.transformChildren(transformer)
        }
        statement { statement, transformer ->
            if (statement is Switch) {
                statement.copy(
                    expression = statement.expression.asStr(),
                    cases = statement.cases.map { it.wrappedInVariantOf(name) },
                    default = listOf(ErrorStatement(Literal("Unknown variant of $name", LanguageType.String))),
                )
            } else {
                statement.transformChildren(transformer)
            }
        }
    }
}

private fun Case.wrappedInVariantOf(union: String): Case = (value as? Literal)?.value?.toString()
    ?.let { member -> copy(value = RawExpression("\"$member\""), body = body.map { it.wrappedIn("$union::${Name.of(member).pascalCase()}") }) }
    ?: this

private fun Statement.wrappedIn(variant: String): Statement = (this as? ReturnStatement)
    ?.let { ReturnStatement(FunctionCall(name = Name(listOf(variant)), arguments = mapOf(Name.of("value") to it.expression))) }
    ?: this
