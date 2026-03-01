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
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
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
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Switch
import community.flock.wirespec.ir.core.Transformer
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.flattenNestedStructs
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
        override val source = """
            |use std::any::TypeId;
            |use std::collections::HashMap;
            |
            |pub trait Model {
            |    fn validate(&self) -> Vec<String>;
            |}
            |
            |pub trait Enum: Sized {
            |    fn label(&self) -> &str;
            |    fn from_label(s: &str) -> Option<Self>;
            |}
            |
            |pub trait Endpoint {}
            |
            |pub trait Channel {}
            |
            |pub trait Refined<T> {
            |    fn value(&self) -> &T;
            |    fn validate(&self) -> bool;
            |}
            |
            |pub trait Path {}
            |
            |pub trait Queries {}
            |
            |pub trait Headers {}
            |
            |pub trait Handler {}
            |
            |#[derive(Debug, Clone, Default, PartialEq)]
            |pub enum Method {
            |    #[default]
            |    GET,
            |    PUT,
            |    POST,
            |    DELETE,
            |    OPTIONS,
            |    HEAD,
            |    PATCH,
            |    TRACE,
            |}
            |
            |pub trait Request<T> {
            |    fn path(&self) -> &dyn Path;
            |    fn method(&self) -> &Method;
            |    fn queries(&self) -> &dyn Queries;
            |    fn headers(&self) -> &dyn RequestHeaders;
            |    fn body(&self) -> &T;
            |}
            |
            |pub trait RequestHeaders: Headers {}
            |
            |pub trait Response<T> {
            |    fn status(&self) -> i32;
            |    fn headers(&self) -> &dyn ResponseHeaders;
            |    fn body(&self) -> &T;
            |}
            |
            |pub trait ResponseHeaders: Headers {}
            |
            |pub trait BodySerializer {
            |    fn serialize_body<T: 'static>(&self, t: &T, r#type: TypeId) -> Vec<u8>;
            |}
            |
            |pub trait BodyDeserializer {
            |    fn deserialize_body<T: 'static>(&self, raw: &[u8], r#type: TypeId) -> T;
            |}
            |
            |pub trait BodySerialization: BodySerializer + BodyDeserializer {}
            |
            |pub trait PathSerializer {
            |    fn serialize_path<T: std::fmt::Display>(&self, t: &T, r#type: TypeId) -> String;
            |}
            |
            |pub trait PathDeserializer {
            |    fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, r#type: TypeId) -> T where T::Err: std::fmt::Debug;
            |}
            |
            |pub trait PathSerialization: PathSerializer + PathDeserializer {}
            |
            |pub trait ParamSerializer {
            |    fn serialize_param<T: 'static>(&self, value: &T, r#type: TypeId) -> Vec<String>;
            |}
            |
            |pub trait ParamDeserializer {
            |    fn deserialize_param<T: 'static>(&self, values: &[String], r#type: TypeId) -> T;
            |}
            |
            |pub trait ParamSerialization: ParamSerializer + ParamDeserializer {}
            |
            |pub trait Serializer: BodySerializer + PathSerializer + ParamSerializer {}
            |
            |pub trait Deserializer: BodyDeserializer + PathDeserializer + ParamDeserializer {}
            |
            |pub trait Serialization: Serializer + Deserializer {}
            |
            |#[derive(Debug, Clone, PartialEq)]
            |pub struct RawRequest {
            |    pub method: String,
            |    pub path: Vec<String>,
            |    pub queries: HashMap<String, Vec<String>>,
            |    pub headers: HashMap<String, Vec<String>>,
            |    pub body: Option<Vec<u8>>,
            |}
            |
            |#[derive(Debug, Clone, PartialEq)]
            |pub struct RawResponse {
            |    pub status_code: i32,
            |    pub headers: HashMap<String, Vec<String>>,
            |    pub body: Option<Vec<u8>>,
            |}
            |
            |pub trait Transportation {
            |    fn transport(&self, request: &RawRequest) -> RawResponse;
            |}
            |
            |pub trait Client {
            |    type Transport: Transportation;
            |    type Ser: Serialization;
            |    fn transport(&self) -> &Self::Transport;
            |    fn serialization(&self) -> &Self::Ser;
            |}
            |
            |pub trait Server {
            |    type Req;
            |    type Res;
            |    fn path_template(&self) -> &'static str;
            |    fn method(&self) -> Method;
            |}
            |""".trimMargin()
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
        return super.emit(module.copy(statements = statements), logger).let {
            fun emitMod(def: Definition) = "pub mod ${def.identifier.sanitize()};"
            val modRs = File(
                Name.of(packageName.toDir() + "mod"),
                listOf(RawElement("#![allow(warnings)]\npub mod model;\npub mod endpoint;\npub mod wirespec;"))
            )
            val modEndpoint = File(
                Name.of(packageName.toDir() + "endpoint/" + "mod"),
                listOf(RawElement(module.statements.filter { s -> s is Endpoint }.map { stmt -> emitMod(stmt) }.joinToString("\n")))
            )
            val modModel = File(
                Name.of(packageName.toDir() + "model/" + "mod"),
                listOf(RawElement(module.statements.filter { s -> s is Model }.map { stmt -> emitMod(stmt) }.joinToString("\n")))
            )
            val shared = File(Name.of(packageName.toDir() + "wirespec"), listOf(RawElement(shared.source)))
            if (emitShared.value)
                it + modRs + modEndpoint + modModel + shared
            else
                it + modRs
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
                elements = listOf(RawElement(importHeader)) + file.elements
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

    fun String.toSnakeCase(): String {
        if (isEmpty()) return this
        return buildString {
            for ((i, c) in this@toSnakeCase.withIndex()) {
                if (c.isUpperCase() && i > 0 && this@toSnakeCase[i - 1].isLowerCase()) {
                    append('_')
                }
                append(c.lowercaseChar())
            }
        }
    }

    fun String.sanitizeKeywords() = if (this in reservedKeywords) "r#$this" else this

    private fun Name.toSnakeCaseName(): Name = Name.of(Name(parts).snakeCase().sanitizeKeywords())

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

    override fun emit(type: Type, module: Module): File {
        val imports = type.importReferences().distinctBy { it.value }
            .joinToString("\n") { "use super::${it.value.toSnakeCase()}::${it.value};" }
        val fieldNames = type.shape.value.map { it.identifier.value }.toSet()
        val addSelfReceiver = transformer {
            statementAndExpression { s, t ->
                if (s is FieldCall && s.receiver == null && s.field.camelCase() in fieldNames) {
                    FieldCall(receiver = VariableReference(Name.of("self")), field = s.field)
                } else {
                    s.transformChildren(t)
                }
            }
        }
        val file = type.convertWithValidation(module)
            .transform {
                matchingElements { fn: LanguageFunction ->
                    if (fn.name == Name.of("validate")) {
                        val transformedBody = fn.body.map { addSelfReceiver.transformStatement(it) }
                        fn.copy(
                            parameters = listOf(community.flock.wirespec.ir.core.Parameter(Name.of("&self"), LanguageType.Custom(""))),
                            body = transformedBody,
                        )
                    } else fn
                }
            }
            .sanitizeNames()
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements)
        else file
    }

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

    override fun emit(refined: Refined): File {
        val file = refined.convert()
        val constraintExpr = refined.reference.convertConstraint(FieldCall(VariableReference(Name.of("self")), Name.of("value")))
        val validate = function("validate") {
            arg("&self", LanguageType.Custom(""))
            returnType(LanguageType.Boolean)
            returns(constraintExpr)
        }
        val toStringExpr = when (refined.reference.type) {
            is Reference.Primitive.Type.String -> "self.value.clone()"
            else -> "format!(\"{}\", self.value)"
        }
        val toString = function("to_string") {
            arg("&self", LanguageType.Custom(""))
            returnType(LanguageType.String)
            returns(RawExpression(toStringExpr))
        }
        return file
            .transform {
                matchingElements { s: Struct ->
                    s.copy(elements = listOf(validate, toString))
                }
            }
    }

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.importReferences().distinctBy { it.value }
            .joinToString("\n") { "use super::super::model::${it.value.toSnakeCase()}::${it.value};" }
        val converted = endpoint.convert().findElement<Namespace>()!!
        val (moduleElements, classElements) = flattenEndpointForRust(converted)
        val endpointClass = Namespace(
            name = converted.name,
            elements = classElements,
            extends = converted.extends,
        )
        val elements = buildList {
            if (imports.isNotEmpty()) add(RawElement(imports))
            addAll(moduleElements)
            add(endpointClass)
        }
        val file = LanguageFile(converted.name, elements)

        // Step 1b: Convert simple-identifier RawExpressions to VariableReference for proper snake_case
        val identifierPattern = Regex("[a-zA-Z_][a-zA-Z0-9_]*")
        val fixRawExprToVarRef = transformer {
            statementAndExpression { s, t ->
                if (s is RawExpression && identifierPattern.matches(s.code) && !s.code.contains(".")) {
                    VariableReference(Name.of(s.code))
                } else s.transformChildren(t)
            }
        }

        // Step 2: Strip "Wirespec." prefix from all Type.Custom names
        val stripWirespecPrefix = transformer {
            type { type, t ->
                val transformed = if (type is LanguageType.Custom && type.name.startsWith("Wirespec.")) {
                    type.copy(name = type.name.removePrefix("Wirespec."))
                } else type
                transformed.transformChildren(t)
            }
        }

        // Step 5: Rename statusCode → status_code in fields, field calls, and constructor args
        val renameStatusCode = transformer {
            field { field, t ->
                val renamed = if (field.name == Name.of("statusCode")) field.copy(name = Name.of("status_code")) else field
                renamed.transformChildren(t)
            }
            statementAndExpression { s, t ->
                when {
                    s is FieldCall && s.field == Name.of("statusCode") ->
                        FieldCall(receiver = s.receiver?.let { t.transformExpression(it) }, field = Name.of("status_code"))
                    s is ConstructorStatement && Name.of("statusCode") in s.namedArguments ->
                        s.copy(
                            type = t.transformType(s.type),
                            namedArguments = s.namedArguments
                                .mapKeys { if (it.key == Name.of("statusCode")) Name.of("status_code") else it.key }
                                .mapValues { t.transformExpression(it.value) },
                        )
                    else -> s.transformChildren(t)
                }
            }
        }

        return file
            .transform {
                apply(fixRawExprToVarRef)         // Step 1b: Fix raw identifier expressions
                apply(stripWirespecPrefix)        // Step 2: Strip Wirespec. prefix from types
                // Step 3: Remove self-referential Handler extends
                matchingElements<Interface> { iface ->
                    if (iface.name == Name.of("Handler")) iface.copy(extends = emptyList()) else iface
                }
                // Step 4a: Strip unused type parameters from Response unions
                matchingElements<LanguageUnion> { union ->
                    if (union.name.pascalCase().startsWith("Response")) union.copy(typeParameters = emptyList()) else union
                }
                // Step 4b: Strip generics from Response type references
                matching<LanguageType.Custom> { type ->
                    if (type.name.startsWith("Response") && type.generics.isNotEmpty()) type.copy(generics = emptyList()) else type
                }
                apply(renameStatusCode)           // Step 5: Rename statusCode → status_code
                // Step 6: Fix response Switch patterns (Response200 → Response::Response200)
                apply(transformer {
                    statement { s, t ->
                        if (s is Switch && s.variable?.camelCase() == "r") {
                            val responsePattern = Regex("Response(\\d+|Default)")
                            val transformedCases = s.cases.map { case ->
                                val typeName = (case.type as? LanguageType.Custom)?.name
                                if (typeName != null && responsePattern.matches(typeName)) {
                                    val varBinding = s.variable!!.snakeCase()
                                    Case(
                                        value = RawExpression("Response::$typeName($varBinding)"),
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
                                default = null, // All enum variants are matched; unreachable default removed
                            )
                        } else {
                            s.transformChildren(t)
                        }
                    }
                })
                // Step 7c: Fix constructors → ::new() calls for Response and Request types
                apply(run {
                    val responsePattern = Regex("Response(\\d+|Default)")
                    fun transformConstructor(cs: ConstructorStatement, t: Transformer): FunctionCall? {
                        val typeName = (cs.type as? LanguageType.Custom)?.name
                        return when {
                            typeName != null && responsePattern.matches(typeName) -> {
                                val transformedArgs = cs.namedArguments.mapValues { t.transformExpression(it.value) }
                                val inner = FunctionCall(
                                    name = Name(listOf("$typeName::new")),
                                    arguments = transformedArgs,
                                )
                                FunctionCall(
                                    name = Name(listOf("Response::$typeName")),
                                    arguments = mapOf(Name.of("inner") to inner),
                                )
                            }
                            typeName == "Request" -> {
                                val transformedArgs = cs.namedArguments.mapValues { t.transformExpression(it.value) }
                                FunctionCall(
                                    name = Name(listOf("Request::new")),
                                    arguments = transformedArgs,
                                )
                            }
                            else -> null
                        }
                    }
                    transformer {
                        statementAndExpression { s, t ->
                            if (s is ConstructorStatement) transformConstructor(s, t) ?: s.transformChildren(t)
                            else s.transformChildren(t)
                        }
                    }
                })
                // Step 8: Fix Serializer/Deserializer parameter types to &impl
                parametersWhere(
                    predicate = { (it.type as? LanguageType.Custom)?.name in setOf("Serializer", "Deserializer") },
                    transform = { it.copy(type = LanguageType.Custom("&impl ${(it.type as LanguageType.Custom).name}")) },
                )
                // Step 9: Snake_case Handler method names and add &self receiver
                matchingElements<Interface> { iface ->
                    if (iface.name == Name.of("Handler")) {
                        iface.transform {
                            matchingElements { fn: LanguageFunction ->
                                fn.copy(
                                    name = Name.of(fn.name.snakeCase()),
                                    parameters = listOf(community.flock.wirespec.ir.core.Parameter(Name.of("&self"), LanguageType.Custom(""))) + fn.parameters,
                                )
                            }
                        }
                    } else iface
                }
                // Step 10: Generate blanket Client impl for Handler
                matchingElements<Namespace> { ns ->
                    val handlerInterface = ns.elements.filterIsInstance<Interface>().firstOrNull { it.name == Name.of("Handler") }
                    if (handlerInterface != null) {
                        val method = handlerInterface.elements.filterIsInstance<LanguageFunction>().firstOrNull()
                        if (method != null) {
                            val methodName = method.name.snakeCase()
                            val implCode = """
                                impl<C: Client> Handler for C {
                                    fn $methodName(&self, request: Request) -> Response {
                                        let raw = to_raw_request(self.serialization(), request);
                                        let resp = self.transport().transport(&raw);
                                        from_raw_response(self.serialization(), resp)
                                    }
                                }
                            """.trimIndent()
                            ns.copy(elements = ns.elements + listOf(RawElement(implCode)))
                        } else ns
                    } else ns
                }
                // Step 11: Generate Api struct with Server impl and conversion methods
                matchingElements<Namespace> { ns ->
                    ns.copy(elements = ns.elements + listOf(RawElement(endpoint.generateApiStruct())))
                }
            }
    }

    private fun flattenEndpointForRust(converted: Namespace): Pair<List<Element>, List<Element>> {
        val flattened = converted.flattenNestedStructs()
        val responsePattern = Regex("Response(\\d+|Default)")
        val moduleElements = flattened.elements
            .filter { it is Struct || it is LanguageUnion }
            .map { element ->
                if (element is LanguageUnion && element.name.pascalCase() == "Response") {
                    val members = flattened.elements
                        .filterIsInstance<Struct>()
                        .map { it.name.pascalCase() }
                        .filter { responsePattern.matches(it) }
                        .map { LanguageType.Custom(it) }
                    element.copy(members = members)
                } else {
                    element
                }
            }
        val classElements = flattened.elements.filter { it !is Struct && it !is LanguageUnion }
        return Pair(moduleElements, classElements)
    }

    override fun emit(channel: Channel): File =
        channel.convert()

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
