package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.core.ir.ArrayIndexCall
import community.flock.wirespec.compiler.core.ir.Case
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.Precision
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Switch
import community.flock.wirespec.compiler.core.ir.Transformer
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.converter.convertConstraint
import community.flock.wirespec.compiler.core.ir.findElement
import community.flock.wirespec.compiler.core.ir.function
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.ir.transformChildren
import community.flock.wirespec.compiler.core.ir.transformer
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.ir.Function as LanguageFunction
import community.flock.wirespec.compiler.core.ir.Type as LanguageType
import community.flock.wirespec.compiler.core.ir.Union as LanguageUnion

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

internal fun String.sanitizeKeywords(): String = if (this in RustEmitter.reservedKeywords) "r#$this" else this

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
    LanguageType.Any -> "Box<dyn std::any::Any>"
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
        s.copy(elements = listOf(refined.validateFunction(), refined.toStringFunction()))
    }
}

private fun Refined.validateFunction(): LanguageFunction = function("validate") {
    arg("&self", LanguageType.Custom(""))
    returnType(LanguageType.Boolean)
    returns(reference.convertConstraint(FieldCall(VariableReference(Name.of("self")), Name.of("value"))))
}

private fun Refined.toStringFunction(): LanguageFunction = function("to_string") {
    arg("&self", LanguageType.Custom(""))
    returnType(LanguageType.String)
    returns(
        RawExpression(
            when (reference.type) {
                is Reference.Primitive.Type.String -> "self.value.clone()"
                else -> "format!(\"{}\", self.value)"
            },
        ),
    )
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

internal fun <T : Element> T.injectHandlerImplForClient(): T = transform {
    matchingElements<Namespace> { ns ->
        ns.findElement<Interface> { it.name == Name.of("Handler") }
            ?.findElement<LanguageFunction>()
            ?.let { method ->
                ns.copy(
                    elements = ns.elements + RawElement(
                        """
                        impl<C: Client> Handler for C {
                            async fn ${method.name.snakeCase()}(&self, request: Request) -> Response {
                                let raw = to_raw_request(self.serialization(), request);
                                let resp = self.transport().transport(&raw).await;
                                from_raw_response(self.serialization(), resp)
                            }
                        }
                        """.trimIndent(),
                    ),
                )
            }
            ?: ns
    }
}

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
