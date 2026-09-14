package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.ir.Case
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Switch
import community.flock.wirespec.compiler.core.ir.Transformer
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.converter.convertConstraint
import community.flock.wirespec.compiler.core.ir.findElement
import community.flock.wirespec.compiler.core.ir.function
import community.flock.wirespec.compiler.core.ir.import
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.ir.transformChildren
import community.flock.wirespec.compiler.core.ir.transformer
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.ir.File as LanguageFile
import community.flock.wirespec.compiler.core.ir.Function as LanguageFunction
import community.flock.wirespec.compiler.core.ir.Type as LanguageType
import community.flock.wirespec.compiler.core.ir.Union as LanguageUnion
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType

internal val rustSelfParam = Parameter(Name.of("&self"), LanguageType.Custom(""))
internal val rustResponsePattern = Regex("Response(\\d+|Default)")

internal fun AstType.buildModelImports(): List<Element> = importReferences().distinctBy { it.value }
    .map { import("super::${it.value.toRustSnakeCase()}", Name.of(it.value).pascalCase()) }

internal fun Endpoint.buildEndpointImports(): List<Element> = importReferences().distinctBy { it.value }
    .map { import("super::super::model::${it.value.toRustSnakeCase()}", Name.of(it.value).pascalCase()) }

internal fun <T : Element> T.convertSimpleRawExpressionsToVariableRefs(): T = transform {
    val identifierPattern = Regex("[a-zA-Z_][a-zA-Z0-9_]*")
    statementAndExpression { s, t ->
        if (s is RawExpression && identifierPattern.matches(s.code) && !s.code.contains(".")) {
            VariableReference(Name.of(s.code))
        } else {
            s.transformChildren(t)
        }
    }
}

internal fun <T : Element> T.stripWirespecPrefix(): T = transform {
    matching<LanguageType.Custom> { type ->
        val pascalName = type.name.pascalCase()
        if (pascalName.startsWith("Wirespec.")) {
            type.copy(name = Name.of(pascalName.removePrefix("Wirespec.")))
        } else {
            type
        }
    }
}

internal fun File.replaceStructWithRefinedFunctions(refined: Refined): File = transform {
    matchingElements { s: Struct ->
        s.copy(elements = listOf(buildValidateFunction(refined), buildToStringFunction(refined)))
    }
}

internal fun File.injectApiStruct(endpoint: Endpoint): File = transform {
    matchingElements<Namespace> { ns ->
        ns.copy(elements = ns.elements + listOf(buildApiStruct(endpoint)))
    }
}

private fun buildValidateFunction(refined: Refined): LanguageFunction {
    val constraintExpr = refined.reference.convertConstraint(
        FieldCall(VariableReference(Name.of("self")), Name.of("value")),
    )
    return function("validate") {
        arg("&self", LanguageType.Custom(""))
        returnType(LanguageType.Boolean)
        returns(constraintExpr)
    }
}

private fun buildToStringFunction(refined: Refined): LanguageFunction {
    val expr = when (refined.reference.type) {
        is Reference.Primitive.Type.String -> "self.value.clone()"
        else -> "format!(\"{}\", self.value)"
    }
    return function("to_string") {
        arg("&self", LanguageType.Custom(""))
        returnType(LanguageType.String)
        returns(RawExpression(expr))
    }
}

internal fun File.flattenForRust(): File {
    val namespace = findElement<Namespace>()!!

    val moduleElements = namespace.elements
        .filter { it is Struct || it is LanguageUnion }
        .map { element ->
            when (element) {
                is LanguageUnion if element.name.pascalCase() == "Response" -> {
                    val members = namespace.elements
                        .filterIsInstance<Struct>()
                        .map { it.name.pascalCase() }
                        .filter { rustResponsePattern.matches(it) }
                        .map { LanguageType.Custom(it) }
                    element.copy(members = members, typeParameters = emptyList())
                }

                is LanguageUnion -> element.copy(typeParameters = emptyList())
                else -> element
            }
        }
    val classElements = namespace.elements.filterNot { it is Struct || it is LanguageUnion }

    return LanguageFile(
        namespace.name,
        moduleElements + Namespace(namespace.name, classElements, namespace.extends),
    )
}

internal fun fixResponseSwitchPatterns(): Transformer = transformer {
    statement { s, t ->
        if (s !is Switch || s.variable?.camelCase() != "r") return@statement s.transformChildren(t)
        val transformedCases = s.cases.map { case ->
            val typeName = (case.type as? LanguageType.Custom)?.name?.pascalCase()
            if (typeName != null && rustResponsePattern.matches(typeName)) {
                Case(
                    value = RawExpression("Response::$typeName(${s.variable!!.snakeCase()})"),
                    body = case.body.map { t.transformStatement(it) },
                    type = null,
                )
            } else {
                Case(
                    value = t.transformExpression(case.value),
                    body = case.body.map { t.transformStatement(it) },
                    type = case.type?.let { t.transformType(it) },
                )
            }
        }
        s.copy(
            expression = t.transformExpression(s.expression),
            cases = transformedCases,
            default = null,
        )
    }
}

internal fun fixConstructorCalls(): Transformer = transformer {
    statementAndExpression { s, t ->
        if (s !is ConstructorStatement) return@statementAndExpression s.transformChildren(t)
        val typeName = (s.type as? LanguageType.Custom)?.name?.pascalCase()
        val transformedArgs = s.namedArguments.mapValues { t.transformExpression(it.value) }
        when {
            typeName != null && rustResponsePattern.matches(typeName) -> FunctionCall(
                name = Name(listOf("Response::$typeName")),
                arguments = mapOf(
                    Name.of("inner") to FunctionCall(
                        name = Name(listOf("$typeName::new")),
                        arguments = transformedArgs,
                    ),
                ),
            )
            typeName == "Request" -> FunctionCall(
                name = Name(listOf("Request::new")),
                arguments = transformedArgs,
            )
            else -> s.transformChildren(t)
        }
    }
}

internal fun <T : Element> T.stripHandlerExtends(): T = transform {
    matchingElements<Interface> { iface ->
        if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) iface.copy(extends = emptyList()) else iface
    }
}

internal fun <T : Element> T.stripResponseGenerics(): T = transform {
    matching<LanguageType.Custom> { type ->
        if (type.name.pascalCase().startsWith("Response") && type.generics.isNotEmpty()) type.copy(generics = emptyList()) else type
    }
}

internal fun <T : Element> T.injectSelfToHandlerMethods(): T = transform {
    matchingElements<Interface> { iface ->
        if (iface.name != Name.of("Handler") && iface.name != Name.of("Call")) return@matchingElements iface
        iface.transform {
            matchingElements { fn: LanguageFunction ->
                fn.copy(
                    name = Name.of(fn.name.snakeCase()),
                    parameters = listOf(rustSelfParam) + fn.parameters,
                )
            }
        }
    }
}

internal fun <T : Element> T.injectHandlerImplForClient(): T = transform {
    matchingElements<Namespace> { ns ->
        val handler = ns.elements.filterIsInstance<Interface>().firstOrNull { it.name == Name.of("Handler") }
            ?: return@matchingElements ns
        val method = handler.elements.filterIsInstance<LanguageFunction>().firstOrNull()
            ?: return@matchingElements ns
        val methodName = method.name.snakeCase()
        ns.copy(
            elements = ns.elements + listOf(
                RawElement(
                    """
                    impl<C: Client> Handler for C {
                        async fn $methodName(&self, request: Request) -> Response {
                            let raw = to_raw_request(self.serialization(), request);
                            let resp = self.transport().transport(&raw).await;
                            from_raw_response(self.serialization(), resp)
                        }
                    }
                    """.trimIndent(),
                ),
            ),
        )
    }
}

internal fun <T : Element> T.injectResponseFromImpls(): T = transform {
    matchingElements<LanguageFile> { file ->
        file.copy(
            elements = file.elements.flatMap { element ->
                if (element is LanguageUnion && element.name.pascalCase() == "Response" && element.members.isNotEmpty()) {
                    listOf(element) + element.members.map { member ->
                        val memberName = member.name.pascalCase()
                        RawElement("impl From<$memberName> for Response { fn from(value: $memberName) -> Self { Response::$memberName(value) } }\n")
                    }
                } else {
                    listOf(element)
                }
            },
        )
    }
}

private fun buildApiStruct(endpoint: Endpoint): RawElement = RawElement(endpoint.generateApiStructCode())

private fun Endpoint.generateApiStructCode(): String {
    val pathTemplate = path.joinToString("/") { segment ->
        when (segment) {
            is Endpoint.Segment.Literal -> segment.value
            is Endpoint.Segment.Param -> "{${segment.identifier.value}}"
        }
    }.let { "/$it" }
    val methodName = method.name
    return """
        pub struct Api;
        impl Server for Api {
            type Req = Request;
            type Res = Response;
            fn path_template(&self) -> &'static str { "$pathTemplate" }
            fn method(&self) -> Method { Method::$methodName }
        }
    """.trimIndent()
}

private fun String.toRustSnakeCase(): String = Name.of(this).snakeCase()
