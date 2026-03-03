package community.flock.wirespec.emitters.rust

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertConstraint
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.Case
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Switch
import community.flock.wirespec.ir.core.Transformer
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.flattenNestedStructs
import community.flock.wirespec.ir.core.`interface`
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.transformer
import community.flock.wirespec.ir.generator.RustGenerator
import community.flock.wirespec.ir.generator.generateRust
import community.flock.wirespec.ir.core.Enum as LanguageEnum
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.File as LanguageFile
import community.flock.wirespec.ir.core.Type as LanguageType
import community.flock.wirespec.ir.core.Union as LanguageUnion

open class RustIrEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared()
) : IrEmitter {

    override val generator = RustGenerator

    val modelImport = """
        |use super::super::wirespec::*;
        |use regex;
        |
    """.trimMargin()

    val endpointImport = """
        |use super::super::wirespec::*;
        |use regex;
        |
    """.trimMargin()

    override val extension = FileExtension.Rust

    override val shared = object : Shared {
        override val packageString = "shared"

        private val rustImports = listOf(
            RawElement("use std::any::TypeId;\nuse std::collections::HashMap;"),
        )

        private val requestHeaders = `interface`("RequestHeaders") {
            extends(LanguageType.Custom("Headers"))
        }

        private val responseHeaders = `interface`("ResponseHeaders") {
            extends(LanguageType.Custom("Headers"))
        }

        private val client = RawElement(
            """
            pub trait Client {
                type Transport: Transportation;
                type Ser: Serialization;
                fn transport(&self) -> &Self::Transport;
                fn serialization(&self) -> &Self::Ser;
            }
            """.trimIndent()
        )

        private val server = RawElement(
            """
            pub trait Server {
                type Req;
                type Res;
                fn path_template(&self) -> &'static str;
                fn method(&self) -> Method;
            }
            """.trimIndent()
        )

        /** Names of interfaces that need Rust-specific RawElement replacements */
        private val rawElementInterfaces = setOf(
            "Enum", "Refined", "Request", "Response",
            "BodySerializer", "BodyDeserializer",
            "PathSerializer", "PathDeserializer",
            "ParamSerializer", "ParamDeserializer",
            "Transportation",
        )

        private fun rustRawElement(name: String): RawElement = when (name) {
            "Enum" -> RawElement(
                """
                pub trait Enum: Sized {
                    fn label(&self) -> &str;
                    fn from_label(s: &str) -> Option<Self>;
                }
                """.trimIndent()
            )
            "Refined" -> RawElement(
                """
                pub trait Refined<T> {
                    fn value(&self) -> &T;
                    fn validate(&self) -> bool;
                }
                """.trimIndent()
            )
            "Request" -> RawElement(
                """
                pub trait Request<T> {
                    fn path(&self) -> &dyn Path;
                    fn method(&self) -> &Method;
                    fn queries(&self) -> &dyn Queries;
                    fn headers(&self) -> &dyn RequestHeaders;
                    fn body(&self) -> &T;
                }
                """.trimIndent()
            )
            "Response" -> RawElement(
                """
                pub trait Response<T> {
                    fn status(&self) -> i32;
                    fn headers(&self) -> &dyn ResponseHeaders;
                    fn body(&self) -> &T;
                }
                """.trimIndent()
            )
            "BodySerializer" -> RawElement(
                """
                pub trait BodySerializer {
                    fn serialize_body<T: 'static>(&self, t: &T, r#type: TypeId) -> Vec<u8>;
                }
                """.trimIndent()
            )
            "BodyDeserializer" -> RawElement(
                """
                pub trait BodyDeserializer {
                    fn deserialize_body<T: 'static>(&self, raw: &[u8], r#type: TypeId) -> T;
                }
                """.trimIndent()
            )
            "PathSerializer" -> RawElement(
                """
                pub trait PathSerializer {
                    fn serialize_path<T: std::fmt::Display>(&self, t: &T, r#type: TypeId) -> String;
                }
                """.trimIndent()
            )
            "PathDeserializer" -> RawElement(
                """
                pub trait PathDeserializer {
                    fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, r#type: TypeId) -> T where T::Err: std::fmt::Debug;
                }
                """.trimIndent()
            )
            "ParamSerializer" -> RawElement(
                """
                pub trait ParamSerializer {
                    fn serialize_param<T: 'static>(&self, value: &T, r#type: TypeId) -> Vec<String>;
                }
                """.trimIndent()
            )
            "ParamDeserializer" -> RawElement(
                """
                pub trait ParamDeserializer {
                    fn deserialize_param<T: 'static>(&self, values: &[String], r#type: TypeId) -> T;
                }
                """.trimIndent()
            )
            "Transportation" -> RawElement(
                """
                pub trait Transportation {
                    async fn transport(&self, request: &RawRequest) -> RawResponse;
                }
                """.trimIndent()
            )
            else -> throw IllegalArgumentException("Unknown Rust raw element: $name")
        }

        private val wirespecFile = AstShared(packageString)
            .convert()
            .transform {
                // Extract elements from Namespace("Wirespec") to top level, strip Package
                matchingElements { file: LanguageFile ->
                    val namespace = file.elements.filterIsInstance<Namespace>().first()
                    file.copy(elements = rustImports + namespace.elements)
                }

                // Replace Method enum with Rust-specific version (#[default] on GET, Default derive)
                matchingElements { enum: LanguageEnum ->
                    if (enum.name == Name.of("Method")) {
                        RawElement(
                            """
                            #[derive(Debug, Clone, Default, PartialEq)]
                            pub enum Method {
                                #[default]
                                GET,
                                PUT,
                                POST,
                                DELETE,
                                OPTIONS,
                                HEAD,
                                PATCH,
                                TRACE,
                            }
                            """.trimIndent()
                        )
                    } else enum
                }

                // Replace interfaces with Rust-specific RawElements, inject
                // RequestHeaders/ResponseHeaders after Request/Response, and
                // append Client/Server at the end — all in a single pass
                matchingElements { file: LanguageFile ->
                    val newElements = file.elements.flatMap { element ->
                        if (element is Interface) {
                            val name = element.name.pascalCase()
                            if (name in rawElementInterfaces) {
                                buildList {
                                    add(rustRawElement(name))
                                    if (name == "Request") add(requestHeaders)
                                    if (name == "Response") add(responseHeaders)
                                }
                            } else {
                                listOf(element)
                            }
                        } else {
                            listOf(element)
                        }
                    } + client + server
                    file.copy(elements = newElements)
                }
            }
            // Inject derive macros before structs (outside transform to avoid recursion)
            .let { file ->
                LanguageFile(file.name, file.elements.flatMap { element ->
                    if (element is Struct) {
                        val derive = when (element.name.pascalCase()) {
                            "RawRequest", "RawResponse" -> "#[derive(Debug, Clone, PartialEq)]"
                            else -> "#[derive(Debug, Clone, Default, PartialEq)]"
                        }
                        listOf(LanguageFile(element.name, listOf(RawElement(derive), element)))
                    } else listOf(element)
                })
            }

        override val source: String = wirespecFile
            .transform {
                // Add &self parameter to functions inside interfaces (Rust trait methods need &self)
                matchingElements { iface: Interface ->
                    iface.transform {
                        matchingElements { fn: LanguageFunction ->
                            val hasSelf = fn.parameters.any { it.name.value() == "&self" || it.name.value() == "self" }
                            if (!hasSelf) {
                                fn.copy(
                                    parameters = listOf(
                                        Parameter(Name.of("&self"), LanguageType.Custom(""))
                                    ) + fn.parameters,
                                )
                            } else fn
                        }
                    }
                }
            }
            .let { file ->
                file.elements.joinToString("\n\n") { element ->
                    element.generateRust().trimEnd('\n')
                } + "\n"
            }
    }

    fun sort(definition: Definition) = when (definition) {
        is Enum -> 1
        is Refined -> 2
        is Type -> 3
        is Union -> 4
        is Endpoint -> 5
        is Channel -> 6
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val statements = module.statements.sortedBy(::sort).toNonEmptyListOrNull()!!
        return super.emit(module.copy(statements = statements), logger).let { files ->
            fun emitMod(def: Definition) = "pub mod ${def.identifier.sanitize()};"
            val modRs = File(
                Name.of(packageName.toDir() + "mod"),
                listOf(RawElement("#![allow(warnings)]\npub mod model;\npub mod endpoint;\npub mod wirespec;"))
            )
            val modEndpoint = File(
                Name.of(packageName.toDir() + "endpoint/" + "mod"),
                listOf(RawElement(module.statements.filterIsInstance<Endpoint>().joinToString("\n") { emitMod(it) }))
            )
            val modModel = File(
                Name.of(packageName.toDir() + "model/" + "mod"),
                listOf(RawElement(module.statements.filterIsInstance<Model>().joinToString("\n") { emitMod(it) }))
            )
            val shared = File(Name.of(packageName.toDir() + "wirespec"), listOf(RawElement(shared.source)))
            if (emitShared.value)
                files + modRs + modEndpoint + modModel + shared
            else
                files + modRs
        }
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val subPackageName = packageName + definition
        val importHeader = when (definition) {
            is Endpoint -> endpointImport
            else -> modelImport
        }
        return super.emit(definition, module, logger).let { file ->
            File(
                name = Name.of(subPackageName.toDir() + file.name.pascalCase().toSnakeCase()),
                elements = listOf(RawElement(importHeader)) + file.elements.flatMap { element ->
                    if (element is Struct) listOf(RawElement("#[derive(Debug, Clone, Default, PartialEq)]"), element)
                    else listOf(element)
                }
            )
        }
    }

    fun Identifier.sanitize() = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
        .let { if (this is FieldIdentifier) it.sanitizeKeywords() else it }
        .toSnakeCase()

    fun String.toSnakeCase(): String = Name.of(this).snakeCase()

    fun String.sanitizeKeywords() = if (this in reservedKeywords) "r#$this" else this

    private fun Name.toSnakeCaseName(): Name = Name.of(Name(parts).snakeCase().sanitizeKeywords())

    // --- Reusable transform helpers ---

    private fun <T : Element> T.sanitizeNames(): T = transform {
        apply(transformer {
            parameter { param, _ ->
                val name = param.name.value()
                if (name == "self" || name == "&self") param
                else param.copy(name = param.name.toSnakeCaseName())
            }
            statementAndExpression { stmt, tr ->
                when (stmt) {
                    is FieldCall -> FieldCall(
                        receiver = stmt.receiver?.let { tr.transformExpression(it) },
                        field = stmt.field.toSnakeCaseName(),
                    )
                    is ConstructorStatement -> ConstructorStatement(
                        type = tr.transformType(stmt.type),
                        namedArguments = stmt.namedArguments
                            .map { (k, v) -> k.toSnakeCaseName() to tr.transformExpression(v) }
                            .toMap(),
                    )
                    else -> stmt.transformChildren(tr)
                }
            }
        })
    }

    private fun <T : Element> T.injectSelfReceiver(fieldNames: Set<String>): T {
        val selfReceiver = transformer {
            statementAndExpression { s, t ->
                if (s is FieldCall && s.receiver == null && s.field.camelCase() in fieldNames) {
                    FieldCall(receiver = VariableReference(Name.of("self")), field = s.field)
                } else s.transformChildren(t)
            }
        }
        return transform {
            matchingElements { fn: LanguageFunction ->
                if (fn.name == Name.of("validate")) {
                    fn.copy(
                        parameters = listOf(Parameter(Name.of("&self"), LanguageType.Custom(""))),
                        body = fn.body.map { selfReceiver.transformStatement(it) },
                    )
                } else fn
            }
        }
    }

    private fun <T : Element> T.stripWirespecPrefix(): T = transform {
        matching<LanguageType.Custom> { type ->
            if (type.name.startsWith("Wirespec.")) type.copy(name = type.name.removePrefix("Wirespec."))
            else type
        }
    }

    private fun File.prependImports(imports: String): File =
        if (imports.isNotEmpty()) copy(elements = listOf(RawElement(imports)) + elements)
        else this

    private fun Type.buildModelImports(): String =
        importReferences().distinctBy { it.value }
            .joinToString("\n") { "use super::${it.value.toSnakeCase()}::${it.value};" }

    private fun Endpoint.buildEndpointImports(): String =
        importReferences().distinctBy { it.value }
            .joinToString("\n") { "use super::super::model::${it.value.toSnakeCase()}::${it.value};" }

    // --- Per-definition emit methods ---

    override fun emit(type: Type, module: Module): File =
        type.convertWithValidation(module)
            .injectSelfReceiver(type.shape.value.map { it.identifier.value }.toSet())
            .sanitizeNames()
            .prependImports(type.buildModelImports())

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .transform {
            matchingElements { languageEnum: LanguageEnum ->
                languageEnum.copy(
                    entries = languageEnum.entries.map {
                        LanguageEnum.Entry(Name.of(it.name.value().sanitizeEnum().sanitizeKeywords()), listOf("\"${it.name.value()}\""))
                    },
                )
            }
        }

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_")
        .toPascalCase()
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    fun String.toPascalCase(): String = split("_").joinToString("") { s ->
        s.replaceFirstChar { it.uppercaseChar() }
    }

    override fun emit(union: Union): File =
        union.convert()

    override fun emit(refined: Refined): File =
        refined.convert()
            .transform {
                matchingElements { s: Struct ->
                    s.copy(elements = listOf(buildValidateFunction(refined), buildToStringFunction(refined)))
                }
            }

    private fun buildValidateFunction(refined: Refined): LanguageFunction {
        val constraintExpr = refined.reference.convertConstraint(
            FieldCall(VariableReference(Name.of("self")), Name.of("value"))
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

    override fun emit(endpoint: Endpoint): File =
        endpoint.convert()
            .flattenForRust()
            .stripWirespecPrefix()
            .rustifyEndpoint(endpoint)
            .sanitizeNames()
            .prependImports(endpoint.buildEndpointImports())

    override fun emit(channel: Channel): File =
        channel.convert()

    // --- Endpoint transform helpers ---

    private fun File.flattenForRust(): File {
        val namespace = findElement<Namespace>()!!
        val flattened = namespace.flattenNestedStructs()
        val responsePattern = RESPONSE_PATTERN

        val moduleElements = flattened.elements
            .filter { it is Struct || it is LanguageUnion }
            .map { element ->
                when {
                    element is LanguageUnion && element.name.pascalCase() == "Response" -> {
                        val members = flattened.elements
                            .filterIsInstance<Struct>()
                            .map { it.name.pascalCase() }
                            .filter { responsePattern.matches(it) }
                            .map { LanguageType.Custom(it) }
                        element.copy(members = members, typeParameters = emptyList())
                    }
                    // Rust enums don't use type parameters for union variants
                    element is LanguageUnion -> element.copy(typeParameters = emptyList())
                    else -> element
                }
            }
        val classElements = flattened.elements.filterNot { it is Struct || it is LanguageUnion }

        return LanguageFile(
            namespace.name,
            moduleElements + Namespace(namespace.name, classElements, namespace.extends),
        )
    }

    private fun fixRawExpressionIdentifiers(): Transformer {
        val identifierPattern = Regex("[a-zA-Z_][a-zA-Z0-9_]*")
        return transformer {
            statementAndExpression { s, t ->
                if (s is RawExpression && identifierPattern.matches(s.code) && !s.code.contains(".")) {
                    VariableReference(Name.of(s.code))
                } else s.transformChildren(t)
            }
        }
    }

    private fun fixResponseSwitchPatterns(): Transformer {
        val responsePattern = RESPONSE_PATTERN
        return transformer {
            statement { s, t ->
                if (s is Switch && s.variable?.camelCase() == "r") {
                    val transformedCases = s.cases.map { case ->
                        val typeName = (case.type as? LanguageType.Custom)?.name
                        if (typeName != null && responsePattern.matches(typeName)) {
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
                } else s.transformChildren(t)
            }
        }
    }

    private fun fixConstructorCalls(): Transformer {
        val responsePattern = RESPONSE_PATTERN
        return transformer {
            statementAndExpression { s, t ->
                if (s is ConstructorStatement) {
                    val typeName = (s.type as? LanguageType.Custom)?.name
                    val transformedArgs = s.namedArguments.mapValues { t.transformExpression(it.value) }
                    when {
                        typeName != null && responsePattern.matches(typeName) -> {
                            FunctionCall(
                                name = Name(listOf("Response::$typeName")),
                                arguments = mapOf(Name.of("inner") to FunctionCall(
                                    name = Name(listOf("$typeName::new")),
                                    arguments = transformedArgs,
                                )),
                            )
                        }
                        typeName == "Request" -> {
                            FunctionCall(
                                name = Name(listOf("Request::new")),
                                arguments = transformedArgs,
                            )
                        }
                        else -> s.transformChildren(t)
                    }
                } else s.transformChildren(t)
            }
        }
    }

    private fun File.rustifyEndpoint(endpoint: Endpoint): File = transform {
        // Convert simple-identifier RawExpressions to VariableReference
        apply(fixRawExpressionIdentifiers())

        // Remove Handler/Call self-referential extends
        matchingElements<Interface> { iface ->
            if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) iface.copy(extends = emptyList()) else iface
        }

        // Strip generics from Response type references
        matching<LanguageType.Custom> { type ->
            if (type.name.startsWith("Response") && type.generics.isNotEmpty()) type.copy(generics = emptyList()) else type
        }

        // Fix response Switch patterns (Response200 → Response::Response200)
        apply(fixResponseSwitchPatterns())

        // Convert ConstructorStatement → FunctionCall for Response/Request types
        apply(fixConstructorCalls())

        // Fix Serializer/Deserializer parameter types to &impl
        parametersWhere(
            predicate = { (it.type as? LanguageType.Custom)?.name in setOf("Serializer", "Deserializer") },
            transform = { it.copy(type = LanguageType.Custom("&impl ${(it.type as LanguageType.Custom).name}")) },
        )

        // Snake_case Handler/Call method names and add &self receiver
        matchingElements<Interface> { iface ->
            if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) {
                iface.transform {
                    matchingElements { fn: LanguageFunction ->
                        fn.copy(
                            name = Name.of(fn.name.snakeCase()),
                            parameters = listOf(Parameter(Name.of("&self"), LanguageType.Custom(""))) + fn.parameters,
                        )
                    }
                }
            } else iface
        }

        // Generate blanket Client impl for Handler
        matchingElements<Namespace> { ns ->
            val handler = ns.elements.filterIsInstance<Interface>().firstOrNull { it.name == Name.of("Handler") }
            if (handler != null) {
                val method = handler.elements.filterIsInstance<LanguageFunction>().firstOrNull()
                if (method != null) {
                    val methodName = method.name.snakeCase()
                    ns.copy(elements = ns.elements + listOf(RawElement("""
                        impl<C: Client> Handler for C {
                            async fn $methodName(&self, request: Request) -> Response {
                                let raw = to_raw_request(self.serialization(), request);
                                let resp = self.transport().transport(&raw).await;
                                from_raw_response(self.serialization(), resp)
                            }
                        }
                    """.trimIndent())))
                } else ns
            } else ns
        }

        // Generate Api struct with Server impl
        matchingElements<Namespace> { ns ->
            ns.copy(elements = ns.elements + listOf(RawElement(endpoint.generateApiStruct())))
        }
    }

    private fun Endpoint.generateApiStruct(): String {
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

    companion object : Keywords {
        private val RESPONSE_PATTERN = Regex("Response(\\d+|Default)")
        override val reservedKeywords = setOf(
            "as", "break", "const", "continue", "crate",
            "else", "enum", "extern", "false", "fn",
            "for", "if", "impl", "in", "let",
            "loop", "match", "mod", "move", "mut",
            "pub", "ref", "return", "self", "Self",
            "static", "struct", "super", "trait", "true",
            "type", "unsafe", "use", "where", "while",
            "async", "await", "dyn", "abstract", "become",
            "box", "do", "final", "macro", "override",
            "priv", "typeof", "unsized", "virtual", "yield",
            "try",
        )
    }
}
