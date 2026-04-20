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
import community.flock.wirespec.ir.converter.requestParameters
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertConstraint
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.Case
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.ArrayIndexCall
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Literal
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
import community.flock.wirespec.ir.emit.SanitizationConfig
import community.flock.wirespec.ir.emit.sanitizeFieldName
import community.flock.wirespec.ir.emit.sanitizeNames
import community.flock.wirespec.ir.extensions.IrExtension
import community.flock.wirespec.ir.generator.RustGenerator
import community.flock.wirespec.ir.generator.generateRust
import community.flock.wirespec.ir.core.Enum as LanguageEnum
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.File as LanguageFile
import community.flock.wirespec.ir.core.Type as LanguageType
import community.flock.wirespec.ir.core.Union as LanguageUnion

private val selfParam = Parameter(Name.of("&self"), LanguageType.Custom(""))
private val RESPONSE_PATTERN = Regex("Response(\\d+|Default)")

open class RustIrEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared()
) : IrEmitter {

    override val generator = RustGenerator

    override val extension = FileExtension.Rust

    private val sanitizationConfig: SanitizationConfig by lazy {
        SanitizationConfig(
            reservedKeywords = reservedKeywords - setOf("self", "Self"),
            escapeKeyword = { "r#$it" },
            escapeFieldKeywords = true,
            fieldNameCase = { name -> Name.of(Name(name.parts).snakeCase()) },
            parameterNameCase = { name ->
                val value = name.value()
                if (value == "self" || value == "&self") name
                else Name.of(Name(name.parts).snakeCase())
            },
            sanitizeSymbol = { it },
            extraStatementTransforms = { stmt, tr ->
                when (stmt) {
                    is ConstructorStatement -> ConstructorStatement(
                        type = tr.transformType(stmt.type),
                        namedArguments = stmt.namedArguments
                            .map { (k, v) -> sanitizationConfig.sanitizeFieldName(k) to tr.transformExpression(v) }
                            .toMap(),
                    )
                    else -> stmt.transformChildren(tr)
                }
            },
        )
    }

    override fun transformTestFile(file: File): File = file.transform {
        apply(borrowSerializationArgs())
        apply(fixResponseSwitchPatterns())
    }

    private val modelImport = """
        |use super::super::wirespec::*;
        |use regex;
        |
    """.trimMargin()

    private val endpointImport = """
        |use super::super::wirespec::*;
        |use regex;
        |
    """.trimMargin()

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

        private val enumTrait = `interface`("Enum") {
            extends(LanguageType.Custom("Sized"))
            function("label") {
                arg("&self", LanguageType.Custom(""))
                returnType(LanguageType.Custom("&str"))
            }
            function("from_label", isStatic = true) {
                arg("s", LanguageType.Custom("&str"))
                returnType(LanguageType.Custom("Option<Self>"))
            }
        }

        private val refinedTrait = `interface`("Refined") {
            typeParam(type("T"))
            function("value") {
                arg("&self", LanguageType.Custom(""))
                returnType(type("T").borrow())
            }
            function("validate") {
                arg("&self", LanguageType.Custom(""))
                returnType(boolean)
            }
        }

        private val requestTrait = `interface`("Request") {
            typeParam(type("T"))
            field("path", type("Path").borrowDyn())
            field("method", type("Method").borrow())
            field("queries", type("Queries").borrowDyn())
            field("headers", type("RequestHeaders").borrowDyn())
            field("body", type("T").borrow())
        }

        private val responseTrait = `interface`("Response") {
            typeParam(type("T"))
            field("status", integer32)
            field("headers", type("ResponseHeaders").borrowDyn())
            field("body", type("T").borrow())
        }

        private val rawElementInterfaces = mapOf(
            "BodySerializer" to RawElement("pub trait BodySerializer {\n    fn serialize_body<T: 'static>(&self, t: &T, r#type: TypeId) -> Vec<u8>;\n}"),
            "BodyDeserializer" to RawElement("pub trait BodyDeserializer {\n    fn deserialize_body<T: 'static>(&self, raw: &[u8], r#type: TypeId) -> T;\n}"),
            "PathSerializer" to RawElement("pub trait PathSerializer {\n    fn serialize_path<T: std::fmt::Display>(&self, t: &T, r#type: TypeId) -> String;\n}"),
            "PathDeserializer" to RawElement("pub trait PathDeserializer {\n    fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, r#type: TypeId) -> T where T::Err: std::fmt::Debug;\n}"),
            "ParamSerializer" to RawElement("pub trait ParamSerializer {\n    fn serialize_param<T: 'static>(&self, value: &T, r#type: TypeId) -> Vec<String>;\n}"),
            "ParamDeserializer" to RawElement("pub trait ParamDeserializer {\n    fn deserialize_param<T: 'static>(&self, values: &[String], r#type: TypeId) -> T;\n}"),
        )

        private val transportationTrait = `interface`("Transportation") {
            asyncFunction("transport") {
                arg("&self", LanguageType.Custom(""))
                arg("request", type("RawRequest").borrow())
                returnType(type("RawResponse"))
            }
        }

        private val dslTraits = mapOf(
            "Enum" to enumTrait,
            "Refined" to refinedTrait,
            "Request" to requestTrait,
            "Response" to responseTrait,
            "Transportation" to transportationTrait,
        )

        private val wirespecFile = AstShared(packageString)
            .convert()
            .transform {
                matchingElements { file: LanguageFile ->
                    val namespace = file.elements.filterIsInstance<Namespace>().first()
                    file.copy(elements = rustImports + namespace.elements)
                }
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
                matchingElements { file: LanguageFile ->
                    val newElements = file.elements.flatMap { element ->
                        if (element is Interface) {
                            val name = element.name.pascalCase()
                            when {
                                name in dslTraits -> buildList {
                                    add(dslTraits[name]!!)
                                    if (name == "Request") add(requestHeaders)
                                    if (name == "Response") add(responseHeaders)
                                }
                                name in rawElementInterfaces -> listOf(rawElementInterfaces[name]!!)
                                else -> listOf(element)
                            }
                        } else {
                            listOf(element)
                        }
                    } + client + server
                    file.copy(elements = newElements)
                }
            }
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
                matchingElements { iface: Interface ->
                    iface.transform {
                        matchingElements { fn: LanguageFunction ->
                            val hasSelf = fn.parameters.any { it.name.value() == "&self" || it.name.value() == "self" }
                            if (!hasSelf && !fn.isStatic) {
                                fn.copy(parameters = listOf(selfParam) + fn.parameters)
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

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val statements = module.statements.sortedBy(::sort).toNonEmptyListOrNull()!!
        return super.emit(module.copy(statements = statements), logger).let { files ->
            fun emitMod(def: Definition) = "pub mod ${def.identifier.sanitize()};"
            val endpoints = module.statements.filterIsInstance<Endpoint>()
            val endpointMods = endpoints.joinToString("\n") { emitMod(it) }
            val clientMod = if (endpoints.isNotEmpty()) "\npub mod client;" else ""
            val modRs = File(
                Name.of(packageName.toDir() + "mod"),
                listOf(RawElement("#![allow(warnings)]\npub mod model;\npub mod endpoint;${clientMod}\npub mod wirespec;"))
            )
            val modEndpoint = File(
                Name.of(packageName.toDir() + "endpoint/" + "mod"),
                listOf(RawElement(endpointMods))
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
        val file = super.emit(definition, module, logger)
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase().toSnakeCase()),
            elements = listOf(RawElement(importHeader)) + file.elements.flatMap { element ->
                if (element is Struct) listOf(RawElement("#[derive(Debug, Clone, Default, PartialEq)]"), element)
                else listOf(element)
            }
        )
    }

    override fun emit(type: Type, module: Module): File =
        type.convertWithValidation(module)
            .injectSelfReceiver(type.shape.value.map { it.identifier.value }.toSet())
            .sanitizeNames(sanitizationConfig)
            .prependImports(type.buildModelImports())

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .transform {
            matchingElements { languageEnum: LanguageEnum ->
                languageEnum.copy(
                    entries = languageEnum.entries.map {
                        it.copy(name = Name.of(it.name.value().sanitizeEnum().sanitizeKeywords()))
                    },
                )
            }
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

    override fun emit(endpoint: Endpoint): File =
        endpoint.convert()
            .flattenForRust()
            .stripWirespecPrefix()
            .transform {
                val identifierPattern = Regex("[a-zA-Z_][a-zA-Z0-9_]*")
                statementAndExpression { s, t ->
                    if (s is RawExpression && identifierPattern.matches(s.code) && !s.code.contains(".")) {
                        VariableReference(Name.of(s.code))
                    } else s.transformChildren(t)
                }
            }
            .stripHandlerExtends()
            .stripResponseGenerics()
            .transform { apply(fixResponseSwitchPatterns()) }
            .transform { apply(fixConstructorCalls()) }
            .transform { apply(borrowSerializationArgs()) }
            .transform {
                parametersWhere(
                    predicate = { (it.type as? LanguageType.Custom)?.name?.dotted() in setOf("Serializer", "Deserializer") },
                    transform = { it.copy(type = (it.type as LanguageType.Custom).borrowImpl()) },
                )
            }
            .injectSelfToHandlerMethods()
            .injectHandlerImplForClient(endpoint)
            .injectResponseFromImpls()
            .transform {
                matchingElements<Namespace> { ns ->
                    ns.copy(elements = ns.elements + listOf(buildApiStruct(endpoint)))
                }
            }
            .sanitizeNames(sanitizationConfig)
            .prependImports(endpoint.buildEndpointImports())

    override fun emit(channel: Channel): File =
        channel.convert()

    override val extensions: List<IrExtension> = listOf(
        RustClientIrExtension(
            packageName = packageName,
            buildClientParams = { it.buildClientParams() },
        ),
    )

    private fun Identifier.sanitize(): String = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
        .let { if (this is FieldIdentifier) it.sanitizeKeywords() else it }
        .toSnakeCase()

    private fun String.toSnakeCase(): String = Name.of(this).snakeCase()

    private fun String.toPascalCase(): String = split("_").joinToString("") { s ->
        s.replaceFirstChar { it.uppercaseChar() }
    }

    private fun String.sanitizeKeywords() = if (this in reservedKeywords) "r#$this" else this

    private fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_")
        .toPascalCase()
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    private fun sort(definition: Definition) = when (definition) {
        is Enum -> 1
        is Refined -> 2
        is Type -> 3
        is Union -> 4
        is Endpoint -> 5
        is Channel -> 6
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

    private fun <T : Element> T.injectSelfReceiver(fieldNames: Set<String>): T = transform {
        matchingElements { fn: LanguageFunction ->
            if (fn.name == Name.of("validate")) {
                fn.copy(parameters = listOf(selfParam)).transform {
                    statementAndExpression { s, t ->
                        if (s is FieldCall && s.receiver == null && s.field.camelCase() in fieldNames) {
                            FieldCall(receiver = VariableReference(Name.of("self")), field = s.field)
                        } else s.transformChildren(t)
                    }
                }
            } else fn
        }
    }

    private fun <T : Element> T.stripWirespecPrefix(): T = transform {
        matching<LanguageType.Custom> { type ->
            val rendered = type.name.dotted()
            if (rendered.startsWith("Wirespec.")) type.copy(name = Name.fromDotted(rendered.removePrefix("Wirespec.")))
            else type
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

    private fun LanguageType.toRustTypeString(): String = when (this) {
        is LanguageType.String -> "String"
        is LanguageType.Boolean -> "bool"
        is LanguageType.Integer -> when (precision) {
            community.flock.wirespec.ir.core.Precision.P32 -> "i32"
            community.flock.wirespec.ir.core.Precision.P64 -> "i64"
        }
        is LanguageType.Number -> when (precision) {
            community.flock.wirespec.ir.core.Precision.P32 -> "f32"
            community.flock.wirespec.ir.core.Precision.P64 -> "f64"
        }
        is LanguageType.Bytes -> "Vec<u8>"
        is LanguageType.Unit -> "()"
        is LanguageType.Any -> "Box<dyn std::any::Any>"
        is LanguageType.Array -> "Vec<${elementType.toRustTypeString()}>"
        is LanguageType.Dict -> "std::collections::HashMap<${keyType.toRustTypeString()}, ${valueType.toRustTypeString()}>"
        is LanguageType.Nullable -> "Option<${type.toRustTypeString()}>"
        is LanguageType.Custom -> name.dotted()
        is LanguageType.Wildcard -> "_"
        is LanguageType.Reflect -> "std::any::TypeId"
        is LanguageType.IntegerLiteral -> "i32"
        is LanguageType.StringLiteral -> "String"
    }

    private fun Endpoint.buildClientParams(): Pair<String, String> {
        val params = requestParameters()
        val paramsStr = if (params.isNotEmpty()) {
            ", " + params.joinToString(", ") { (name, type) ->
                "${name.snakeCase().sanitizeKeywords()}: ${type.toRustTypeString()}"
            }
        } else ""
        val argsStr = if (params.isNotEmpty()) {
            params.joinToString(", ") { (name, _) -> name.snakeCase().sanitizeKeywords() }
        } else ""
        return paramsStr to argsStr
    }

    private fun File.flattenForRust(): File {
        val namespace = findElement<Namespace>()!!
        val flattened = namespace.flattenNestedStructs()

        val moduleElements = flattened.elements
            .filter { it is Struct || it is LanguageUnion }
            .map { element ->
                when {
                    element is LanguageUnion && element.name.pascalCase() == "Response" -> {
                        val members = flattened.elements
                            .filterIsInstance<Struct>()
                            .map { it.name.pascalCase() }
                            .filter { RESPONSE_PATTERN.matches(it) }
                            .map { LanguageType.Custom(it) }
                        element.copy(members = members, typeParameters = emptyList())
                    }
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

    private val serializationMethodNames = setOf(
        "serialize_path", "serialize_param", "serialize_body",
        "deserialize_path", "deserialize_param", "deserialize_body",
    )

    private fun Expression.toRawCode(): String = when (this) {
        is VariableReference -> name.snakeCase().sanitizeKeywords()
        is FieldCall -> {
            val recv = receiver?.let { "${it.toRawCode()}." } ?: ""
            "$recv${field.snakeCase().sanitizeKeywords()}"
        }
        is ArrayIndexCall -> {
            val lit = index as? Literal
            val idx = when {
                lit != null && (lit.type is LanguageType.Integer || lit.type is LanguageType.Number) -> "${lit.value}"
                lit != null && lit.type is LanguageType.String -> "\"${lit.value}\""
                else -> index.toRawCode()
            }
            "${receiver.toRawCode()}[$idx]"
        }
        is ConstructorStatement -> {
            val typeName = (type as? LanguageType.Custom)?.name?.dotted() ?: type.toString()
            val args = namedArguments.entries.joinToString(", ") { "${it.key.snakeCase()}: ${it.value.toRawCode()}" }
            if (args.isEmpty()) "$typeName {}" else "$typeName { $args }"
        }
        is Literal -> when {
            type is LanguageType.String -> "String::from(\"$value\")"
            type is LanguageType.Boolean -> "$value"
            type is LanguageType.Integer -> "${value}"
            type is LanguageType.Number -> "${value}"
            else -> "$value"
        }
        is RawExpression -> code
        else -> error("Unsupported expression type in toRawCode: ${this::class.simpleName}")
    }

    private fun Expression.toBorrowedRaw(): RawExpression = RawExpression("&${toRawCode()}")

    private fun borrowSerializationArgs(): Transformer = transformer {
        statementAndExpression { s, t ->
            if (s is FunctionCall && s.name.snakeCase() in serializationMethodNames) {
                val newArgs = s.arguments.entries.mapIndexed { idx, (key, value) ->
                    val transformed = t.transformExpression(value)
                    if (idx == 0 && !(transformed is VariableReference && transformed.name.value() == "it")) {
                        key to transformed.toBorrowedRaw()
                    } else {
                        key to transformed
                    }
                }.toMap()
                s.copy(arguments = newArgs)
            } else s.transformChildren(t)
        }
    }

    private fun fixResponseSwitchPatterns(): Transformer = transformer {
        statement { s, t ->
            if (s is Switch && s.variable?.camelCase() == "r") {
                val transformedCases = s.cases.map { case ->
                    val typeName = (case.type as? LanguageType.Custom)?.name?.dotted()
                    if (typeName != null && RESPONSE_PATTERN.matches(typeName)) {
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

    private fun fixConstructorCalls(): Transformer = transformer {
        statementAndExpression { s, t ->
            if (s is ConstructorStatement) {
                val typeName = (s.type as? LanguageType.Custom)?.name?.dotted()
                val transformedArgs = s.namedArguments.mapValues { t.transformExpression(it.value) }
                when {
                    typeName != null && RESPONSE_PATTERN.matches(typeName) -> {
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

    private fun <T : Element> T.stripHandlerExtends(): T = transform {
        matchingElements<Interface> { iface ->
            if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) iface.copy(extends = emptyList()) else iface
        }
    }

    private fun <T : Element> T.stripResponseGenerics(): T = transform {
        matching<LanguageType.Custom> { type ->
            if (type.name.dotted().startsWith("Response") && type.generics.isNotEmpty()) type.copy(generics = emptyList()) else type
        }
    }

    private fun <T : Element> T.injectSelfToHandlerMethods(): T = transform {
        matchingElements<Interface> { iface ->
            if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) {
                iface.transform {
                    matchingElements { fn: LanguageFunction ->
                        fn.copy(
                            name = Name.of(fn.name.snakeCase()),
                            parameters = listOf(selfParam) + fn.parameters,
                        )
                    }
                }
            } else iface
        }
    }

    private fun <T : Element> T.injectHandlerImplForClient(endpoint: Endpoint): T = transform {
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
    }

    private fun <T : Element> T.injectResponseFromImpls(): T = transform {
        matchingElements<LanguageFile> { file ->
            file.copy(elements = file.elements.flatMap { element ->
                if (element is LanguageUnion && element.name.pascalCase() == "Response" && element.members.isNotEmpty()) {
                    listOf(element) + element.members.map { member ->
                        val memberName = member.name.dotted()
                        RawElement("impl From<$memberName> for Response { fn from(value: $memberName) -> Self { Response::$memberName(value) } }\n")
                    }
                } else listOf(element)
            })
        }
    }

    private fun buildApiStruct(endpoint: Endpoint): RawElement = RawElement(endpoint.generateApiStruct())

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
        fun VariableReference.borrow(): VariableReference = VariableReference(Name(listOf("&${name.snakeCase()}")))
        fun LanguageType.Custom.borrow(): LanguageType.Custom = copy(name = Name.fromDotted("&${name.dotted()}"))
        fun LanguageType.Custom.borrowDyn(): LanguageType.Custom = copy(name = Name.fromDotted("&dyn ${name.dotted()}"))
        fun LanguageType.Custom.borrowImpl(): LanguageType.Custom = copy(name = Name.fromDotted("&impl ${name.dotted()}"))
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
