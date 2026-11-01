package community.flock.wirespec.emitters.rust

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.converter.convertToGenerator
import community.flock.wirespec.compiler.core.ir.converter.requestParameters
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.collectCustomTypeNames
import community.flock.wirespec.compiler.core.ir.fieldList
import community.flock.wirespec.compiler.core.ir.findElement
import community.flock.wirespec.compiler.core.ir.import
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.ir.transformChildren
import community.flock.wirespec.compiler.core.ir.emit.IrEmitter
import community.flock.wirespec.compiler.core.ir.generator.Generator
import community.flock.wirespec.compiler.core.ir.generator.RustGenerator
import community.flock.wirespec.compiler.core.ir.transformer.SanitizationConfig
import community.flock.wirespec.compiler.core.ir.transformer.sanitizeEnumEntries
import community.flock.wirespec.compiler.core.ir.transformer.sanitizeFieldName
import community.flock.wirespec.compiler.core.ir.transformer.sanitizeNames
import community.flock.wirespec.compiler.core.ir.transformer.sortKey
import community.flock.wirespec.compiler.core.ir.Enum as LanguageEnum
import community.flock.wirespec.compiler.core.ir.Type as LanguageType

private const val STRUCT_DERIVE = "#[derive(Debug, Clone, Default, PartialEq)]"

private val wirespecImports = listOf(
    import("super::super::wirespec", "*"),
    import("", "regex"),
)

public open class RustEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared()
) : IrEmitter {

    override val generator: Generator = RustGenerator

    override val extension: FileExtension = FileExtension.Rust

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
            // Rust identifiers may not start with a digit; prefix an underscore when they do
            // (e.g. a `1st-line` field becomes `_1st_line`).
            sanitizeSymbol = { if (it.firstOrNull()?.isDigit() == true) "_$it" else it },
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

    override fun transformTestFile(file: File): File = file
        .transform { apply(fixResponseSwitchPatterns()) }
        .applyBorrowRules()

    override fun emitShared(): File? = if (emitShared.value) {
        File(Name.of(packageName.toDir() + "wirespec"), listOf(RawElement(generator.generate(sharedFile()))))
    } else {
        null
    }

    private fun sharedFile(): File = PackageName("shared").convert()
        .findElement<Namespace>()!!
        .elements
        .flatMap { element ->
            when (element) {
                is LanguageEnum if element.name == Name.of("Method") -> listOf(sharedMethod)
                is Interface -> listOf(sharedTraits[element.name.pascalCase()] ?: element.withSelfReceivers())
                is Struct -> listOfNotNull(sharedDerive.takeIf { element.fieldList().all { it.type.isDerivable() } }, element)
                else -> listOf(element)
            }
        }
        .let { File(Name.of("Wirespec"), sharedImports + it + sharedClientAndServer) }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val files = super.emit(module.copy(statements = module.statements.sortedBy { it.sortKey() }.toNonEmptyListOrNull()!!), logger)
        val endpoints = module.statements.filterIsInstance<Endpoint>()
        val clientMod = if (endpoints.isNotEmpty()) "\npub mod client;" else ""
        val modRs = File(
            Name.of(packageName.toDir() + "mod"),
            listOf(RawElement("#![allow(warnings)]\npub mod model;\npub mod endpoint;${clientMod}\npub mod wirespec;")),
        )
        return if (emitShared.value) {
            files + modRs + modFile("endpoint/", endpoints) + modFile("model/", module.statements.filterIsInstance<Model>())
        } else {
            files + modRs
        }
    }

    private fun modFile(directory: String, definitions: List<Definition>): File = File(
        Name.of(packageName.toDir() + directory + "mod"),
        listOf(RawElement(definitions.joinToString("\n") { "pub mod ${it.identifier.moduleName()};" })),
    )

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val file = super.emit(definition, module, logger).applyBorrowRules()
        return File(
            name = Name.of((packageName + definition).toDir() + file.name.pascalCase().toSnakeCase()),
            elements = wirespecImports + file.elements.withStructDerives(),
        )
    }

    override fun emitGenerator(definition: Definition, module: Module): File? = when (definition) {
        is Type -> definition.convertToGenerator(module)
        is Enum -> definition.convertToGenerator()
        is Refined -> definition.convertToGenerator()
        is Union -> definition.convertToGenerator()
        is Endpoint, is Channel, is Rpc, is Part -> null
    }?.sanitizeNames(sanitizationConfig)?.let { generatorFile ->
        val generatorReferences = buildSet {
            generatorFile.transform {
                expression { expr, t ->
                    if (expr is RawExpression && expr.code.endsWith("Generator")) add(expr.code)
                    expr.transformChildren(t)
                }
            }
        }
        val (generators, types) = generatorFile.collectCustomTypeNames()
            .filterNot { it.startsWith("Wirespec.") || it == "Wirespec" }
            .map { it.substringBefore('<') }
            .plus(generatorReferences)
            .distinct()
            .filterNot { it == "${definition.identifier.value}Generator" }
            .partition { it.endsWith("Generator") }
        File(
            name = Name.of((packageName + "generator").toDir() + generatorFile.name.pascalCase().toSnakeCase()),
            elements = wirespecImports +
                types.map { import("super::super::model::${it.toSnakeCase()}", it) } +
                generators.map { import("super::${it.toSnakeCase()}", it) } +
                generatorFile.elements,
        )
    }

    override fun emit(type: Type, module: Module): File = type.convert()
        .sanitizeNames(sanitizationConfig)
        .withImports(type.importReferences().toImports("super::"))

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .sanitizeEnumEntries(sanitizeEntry = { it.sanitizeEnum().sanitizeKeywords() })

    override fun emit(union: Union): File = union.convert()

    override fun emit(refined: Refined): File = refined
        .convert()
        .replaceStructWithRefinedFunctions(refined)

    override fun emit(endpoint: Endpoint): File = endpoint
        .convert()
        .flattenForRust()
        .stripWirespecPrefix()
        .convertSimpleRawExpressionsToVariableRefs()
        .rustHandlerTraits()
        .stripResponseGenerics()
        .transform {
            apply(fixResponseSwitchPatterns())
            apply(fixConstructorCalls())
        }
        .injectHandlerImplForClient()
        .injectResponseFromImpls()
        .injectApiStruct(endpoint)
        .sanitizeNames(sanitizationConfig)
        .withImports(endpoint.importReferences().toImports("super::super::model::"))

    override fun emit(channel: Channel): File = channel.convert()

    override fun emit(rpc: Rpc): File = rpc.convert().let { file ->
        file.copy(
            elements = rpc.importReferences().toImports("super::super::model::") + file.elements.map { element ->
                if (element is Namespace) element.copy(elements = element.elements.withStructDerives()) else element
            },
        )
    }

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val module = endpoint.identifier.value.toSnakeCase()
        val namespace = "$module::${endpoint.identifier.value}"
        val (params, args) = endpoint.clientParameters()
        val code = """
            pub struct ${endpoint.clientName}<'a, S: Serialization, T: Transportation> {
                pub serialization: &'a S,
                pub transportation: &'a T,
            }
            impl<'a, S: Serialization, T: Transportation> $namespace::Call for ${endpoint.clientName}<'a, S, T> {
                async fn $module(&self$params) -> $module::Response {
                    let request = $module::Request::new($args);
                    let raw_request = $namespace::to_raw_request(self.serialization, request);
                    let raw_response = self.transportation.transport(&raw_request).await;
                    $namespace::from_raw_response(self.serialization, raw_response)
                }
            }
        """.trimIndent()
        return File(
            name = Name.of((packageName + "client").toDir() + endpoint.clientName.toSnakeCase()),
            elements = listOf(import("super::super::wirespec", "*"), import("super::super::endpoint", module)) +
                endpoint.importReferences().toImports("super::super::model::") +
                RawElement(code),
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        logger.info("Emitting main Client for ${endpoints.size} endpoints")
        val modDeclarations = endpoints.joinToString("\n") { "pub mod ${it.clientName.toSnakeCase()};" }
        val modelImports = endpoints.flatMap { it.importReferences() }
            .filter { ref -> endpoints.none { it.identifier.value == ref.value } }
            .toImports("super::model::")
        val endpointAndClientImports = endpoints.flatMap {
            listOf(import("super::endpoint", it.identifier.value.toSnakeCase()), import(it.clientName.toSnakeCase(), it.clientName))
        }
        val implementations = endpoints.joinToString("\n") { endpoint ->
            val module = endpoint.identifier.value.toSnakeCase()
            val (params, args) = endpoint.clientParameters()
            """
            impl<S: Serialization, T: Transportation> $module::${endpoint.identifier.value}::Call for Client<S, T> {
                async fn $module(&self$params) -> $module::Response {
                    ${endpoint.clientName} { serialization: &self.serialization, transportation: &self.transportation }
                        .$module($args).await
                }
            }
            """.trimIndent()
        }
        val code = "pub struct Client<S: Serialization, T: Transportation> {\n    pub serialization: S,\n    pub transportation: T,\n}\n$implementations"
        return File(
            name = Name.of(packageName.toDir() + "client"),
            elements = listOf(RawElement(modDeclarations), import("super::wirespec", "*")) + modelImports + endpointAndClientImports + RawElement(code),
        )
    }

    private val Endpoint.clientName: String get() = "${identifier.value}Client"

    private fun Endpoint.clientParameters(): Pair<String, String> = requestParameters()
        .map { (name, type) -> name.snakeCase().sanitizeKeywords() to type.rustName() }
        .let { params -> params.joinToString("") { (name, type) -> ", $name: $type" } to params.joinToString(", ") { it.first } }

    public companion object : Keywords by RustGenerator {
        public fun VariableReference.borrow(): VariableReference = VariableReference(Name(listOf("&${name.snakeCase()}")))
    }
}

private fun Identifier.moduleName(): String = value
    .split(".", " ")
    .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
    .joinToString("")
    .filter { it.isLetterOrDigit() || it == '_' }
    .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
    .toSnakeCase()

private fun String.sanitizeEnum(): String = split("-", ", ", ".", " ", "//", "_")
    .joinToString("") { s -> s.replaceFirstChar { it.uppercaseChar() } }
    .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

private fun List<Reference.Custom>.toImports(path: String): List<Element> = distinctBy { it.value }
    .map { import("$path${it.value.toSnakeCase()}", Name.of(it.value).pascalCase()) }

private fun File.withImports(imports: List<Element>): File = copy(elements = imports + elements)

private fun List<Element>.withStructDerives(): List<Element> = flatMap { if (it is Struct) listOf(RawElement(STRUCT_DERIVE), it) else listOf(it) }

// `Box<dyn Any>` and `Box<dyn Fn(..)>` implement neither `Clone` nor `PartialEq`.
private fun LanguageType.isDerivable(): Boolean = when (this) {
    LanguageType.Any, is LanguageType.Function -> false
    is LanguageType.Custom -> generics.all { it.isDerivable() }
    is LanguageType.Nullable -> type.isDerivable()
    is LanguageType.Array -> elementType.isDerivable()
    is LanguageType.Dict -> keyType.isDerivable() && valueType.isDerivable()
    is LanguageType.Integer, is LanguageType.Number, LanguageType.String, LanguageType.Boolean, LanguageType.Bytes,
    LanguageType.Unit, LanguageType.Wildcard, LanguageType.Reflect, is LanguageType.IntegerLiteral, is LanguageType.StringLiteral,
    -> true
}

private val sharedImports = listOf(
    import("std::any", "TypeId"),
    import("std::collections", "HashMap"),
)

private val sharedDerive = RawElement("#[derive(Debug, Clone, PartialEq)]")

private val sharedMethod = RawElement(
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
    """.trimIndent(),
)

// The shared traits whose Rust signatures differ from what the generator renders from the shared IR.
private val sharedTraits: Map<String, RawElement> = mapOf(
    "Enum" to """
        pub trait Enum: Sized {
            fn label(&self) -> &str;
            fn from_label(s: &str) -> Option<Self>;
        }
    """,
    "Refined" to """
        pub trait Refined<T> {
            fn value(&self) -> &T;
            fn validate(&self) -> bool;
        }
    """,
    "Request" to """
        pub trait Request<T> {
            fn path(&self) -> &dyn Path;
            fn method(&self) -> &Method;
            fn queries(&self) -> &dyn Queries;
            fn headers(&self) -> &dyn RequestHeaders;
            fn body(&self) -> &T;
        }
        pub trait RequestHeaders: Headers {}
    """,
    "Response" to """
        pub trait Response<T> {
            fn status(&self) -> i32;
            fn headers(&self) -> &dyn ResponseHeaders;
            fn body(&self) -> &T;
        }
        pub trait ResponseHeaders: Headers {}
    """,
    "BodySerializer" to """
        pub trait BodySerializer {
            fn serialize_body<T: 'static>(&self, t: &T, r#type: TypeId) -> Vec<u8>;
        }
    """,
    "BodyDeserializer" to """
        pub trait BodyDeserializer {
            fn deserialize_body<T: 'static>(&self, raw: &[u8], r#type: TypeId) -> T;
        }
    """,
    "PathSerializer" to """
        pub trait PathSerializer {
            fn serialize_path<T: std::fmt::Display>(&self, t: &T, r#type: TypeId) -> String;
        }
    """,
    "PathDeserializer" to """
        pub trait PathDeserializer {
            fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, r#type: TypeId) -> T where T::Err: std::fmt::Debug;
        }
    """,
    "ParamSerializer" to """
        pub trait ParamSerializer {
            fn serialize_param<T: 'static>(&self, value: &T, r#type: TypeId) -> Vec<String>;
        }
    """,
    "ParamDeserializer" to """
        pub trait ParamDeserializer {
            fn deserialize_param<T: 'static>(&self, values: &[String], r#type: TypeId) -> T;
        }
    """,
    "Transportation" to """
        pub trait Transportation {
            async fn transport(&self, request: &RawRequest) -> RawResponse;
        }
    """,
    "Generator" to """
        pub trait Generator {
            fn generate<T>(&self, path: Vec<String>, field: impl GeneratorField<T>) -> T;
        }
    """,
).mapValues { (_, code) -> RawElement(code.trimIndent()) }

private val sharedClientAndServer = listOf(
    RawElement(
        """
        pub trait Client {
            type Transport: Transportation;
            type Ser: Serialization;
            fn transport(&self) -> &Self::Transport;
            fn serialization(&self) -> &Self::Ser;
        }
        """.trimIndent(),
    ),
    RawElement(
        """
        pub trait Server {
            type Req;
            type Res;
            fn path_template(&self) -> &'static str;
            fn method(&self) -> Method;
        }
        """.trimIndent(),
    ),
)
