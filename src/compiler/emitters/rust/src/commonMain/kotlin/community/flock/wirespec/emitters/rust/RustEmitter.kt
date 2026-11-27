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
import community.flock.wirespec.compiler.core.ir.transformer.withLiteralConstructor
import community.flock.wirespec.compiler.core.ir.Enum as LanguageEnum
import community.flock.wirespec.compiler.core.ir.Type as LanguageType
import community.flock.wirespec.compiler.core.ir.Union as LanguageUnion

private const val STRUCT_DERIVE = "#[derive(Debug, Clone, Default, PartialEq)]"

private val requiredDirectories = listOf("model", "endpoint")

// Only declared in `mod.rs` when the specification has a definition that is emitted there.
private val optionalDirectories = listOf("channel", "rpc", "generator")

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
            reservedKeywords = reservedKeywords - unescapableKeywords,
            escapeKeyword = { "r#$it" },
            escapeFieldKeywords = true,
            fieldNameCase = { name -> Name.of(Name(name.parts).snakeCase()) },
            parameterNameCase = { name ->
                val value = name.value()
                if (value == "&self") name
                else Name.of(Name(name.parts).snakeCase())
            },
            // Rust identifiers may not start with a digit; prefix an underscore when they do
            // (e.g. a `1st-line` field becomes `_1st_line`).
            // A keyword that has no raw identifier gets a trailing underscore instead: `self` becomes `self_`.
            sanitizeSymbol = { symbol ->
                when {
                    symbol.firstOrNull()?.isDigit() == true -> "_$symbol"
                    symbol in unescapableKeywords -> "${symbol}_"
                    else -> symbol
                }
            },
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
        .transform {
            apply(fixResponseSwitchPatterns())
            apply(associatedGeneratorCalls)
        }
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
                is Interface if element.name == Name.of("Generator") -> listOf(sharedGenerator)
                is Interface if element.isGeneratorField() -> emptyList()
                is Struct if element.isGeneratorField() -> emptyList()
                is Interface -> listOf(sharedTraits[element.name.pascalCase()] ?: element.withSelfReceivers())
                is Struct -> listOfNotNull(sharedDerive.takeIf { element.fieldList().all { it.type.isDerivable() } }, element)
                else -> listOf(element)
            }
        }
        .let { File(Name.of("Wirespec"), sharedImports + it + sharedAnyValue + sharedErrorAndServer) }

    /** The `mod.rs` files list the modules of every file of the specification, so they are written from all that is emitted. */
    override fun emitIndex(files: List<File>): List<File> {
        val modules = (requiredDirectories + optionalDirectories).associateWith { files.modulesIn(it) }
        val declared = requiredDirectories + listOfNotNull("client".takeIf { modules.getValue("endpoint").isNotEmpty() }) +
            optionalDirectories.filter { modules.getValue(it).isNotEmpty() } + "wirespec"
        val root = modFile("", "#![allow(warnings)]\n" + declared.declarations())
        return if (emitShared.value) {
            listOf(root) + modules.filter { (directory, names) -> directory in requiredDirectories || names.isNotEmpty() }
                .map { (directory, names) -> modFile("$directory/", names.declarations()) }
        } else {
            listOf(root)
        }
    }

    private fun modFile(directory: String, content: String): File = File(Name.of("${packageName.toDir()}${directory}mod"), listOf(RawElement(content)))

    private fun List<File>.modulesIn(directory: String): List<String> = map { it.name.value().removePrefix(packageName.toDir()) }
        .filter { it.startsWith("$directory/") }
        .map { it.removePrefix("$directory/") }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> = super.emit(module.copy(statements = module.statements.sortedBy { it.sortKey() }.toNonEmptyListOrNull()!!), logger)

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val file = super.emit(definition, module, logger).applyBorrowRules().withAnyValues()
        return File(
            name = Name.of((packageName + definition).toDir() + file.name.pascalCase().toSnakeCase()),
            elements = wirespecImports + file.elements.withStructDerives(),
        )
    }

    override fun emitGenerator(definition: Definition, module: Module): File? = (definition as? Model)?.let { emitGenerator(it, module) }

    private fun emitGenerator(model: Model, module: Module): File = when (model) {
        is Type -> model.convertToGenerator(module)
        is Enum -> model.convertToGenerator()
        is Refined -> model.convertToGenerator()
        is Union -> model.convertToGenerator()
    }.sanitizeNames(sanitizationConfig).let { generatorFile ->
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
            .filterNot { it == "${model.identifier.value}Generator" }
            .partition { it.endsWith("Generator") }
        File(
            name = Name.of((packageName + "generator").toDir() + generatorFile.name.pascalCase().toSnakeCase()),
            elements = wirespecImports +
                types.map { import("super::super::model::${it.toSnakeCase()}", it) } +
                generators.map { import("super::${it.toSnakeCase()}", it) } +
                generatorFile.toRustGenerator(model).elements,
        )
    }

    override fun emit(type: Type, module: Module): File = type.convert()
        .sanitizeNames(sanitizationConfig)
        .withLiteralConstructor()
        .withImports(type.importReferences().toImports("super::"))

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .sanitizeEnumEntries(sanitizeEntry = { it.sanitizeEnum().sanitizeKeywords() })

    override fun emit(union: Union): File = union.convert()
        .withImports(union.importReferences().toImports("super::"))
        .withDefaultOfFirstMember()

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
        .withErrorsReturned()
        .injectResponseFromImpls()
        .injectApiStruct(endpoint)
        .sanitizeNames(sanitizationConfig)
        .withRenamedConstructorArguments()
        .withImports(endpoint.importReferences().toImports("super::super::model::"))

    override fun emit(channel: Channel): File = channel.convert()
        .withSelfReceivers()
        .withImports(channel.importReferences().toImports("super::super::model::"))

    override fun emit(rpc: Rpc): File = rpc.convert().sanitizeNames(sanitizationConfig).withSelfReceivers().let { file ->
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
                async fn $module(&self$params) -> Result<$module::Response, WirespecError> {
                    let request = $module::Request::new($args);
                    let raw_request = $namespace::to_raw_request(self.serialization, request);
                    let raw_response = self.transportation.transport(&raw_request).await?;
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
                async fn $module(&self$params) -> Result<$module::Response, WirespecError> {
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

private fun List<String>.declarations(): String = joinToString("\n") { "pub mod $it;" }

private fun String.sanitizeEnum(): String = split("-", ", ", ".", " ", "//", "_")
    .joinToString("") { s -> s.replaceFirstChar { it.uppercaseChar() } }
    .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

private fun List<Reference.Custom>.toImports(path: String): List<Element> = distinctBy { it.value }
    .map { import("$path${it.value.toSnakeCase()}", Name.of(it.value).pascalCase()) }

private fun File.withImports(imports: List<Element>): File = copy(elements = imports + elements)

/**
 * Structs derive `Default`, so a union they hold needs one too. `#[default]` is only allowed on a unit variant, so the
 * union gets a hand-written `Default` that takes its first member.
 */
private fun File.withDefaultOfFirstMember(): File = findElement<LanguageUnion>()
    ?.let { union -> union.members.firstOrNull()?.let { "${union.name.pascalCase()}::${it.name.pascalCase()}" } }
    ?.let { copy(elements = elements + RawElement("impl Default for ${name.pascalCase()} { fn default() -> Self { $it(Default::default()) } }")) }
    ?: this

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
            fn deserialize_body<T: 'static>(&self, raw: &[u8], r#type: TypeId) -> Result<T, WirespecError>;
        }
    """,
    "PathSerializer" to """
        pub trait PathSerializer {
            fn serialize_path<T: std::fmt::Display>(&self, t: &T, r#type: TypeId) -> String;
        }
    """,
    "PathDeserializer" to """
        pub trait PathDeserializer {
            fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, r#type: TypeId) -> Result<T, WirespecError> where T::Err: std::fmt::Debug;
        }
    """,
    "ParamSerializer" to """
        pub trait ParamSerializer {
            fn serialize_param<T: 'static>(&self, value: &T, r#type: TypeId) -> Vec<String>;
        }
    """,
    "ParamDeserializer" to """
        pub trait ParamDeserializer {
            fn deserialize_param<T: 'static>(&self, values: &[String], r#type: TypeId) -> Result<T, WirespecError>;
        }
    """,
    "Transportation" to """
        pub trait Transportation {
            async fn transport(&self, request: &RawRequest) -> Result<RawResponse, WirespecError>;
        }
    """,
).mapValues { (_, code) -> RawElement(code.trimIndent()) }

private fun Element.isGeneratorField(): Boolean = when (this) {
    is Interface -> name.pascalCase().startsWith("GeneratorField")
    is Struct -> name.pascalCase().startsWith("GeneratorField")
    else -> false
}

private val generatorFields = mapOf(
    "String" to "String",
    "Integer64" to "i64",
    "Integer32" to "i32",
    "Number64" to "f64",
    "Number32" to "f32",
    "Boolean" to "bool",
    "Bytes" to "Vec<u8>",
    "Enum" to "String",
    "Union" to "String",
)

private val generatorFieldsOfGenerated = mapOf(
    "Array" to "Vec<T>",
    "Nullable" to "Option<T>",
    "Dict" to "HashMap<String, T>",
    "Shape" to "T",
)

private fun generatorFieldImpl(field: String, value: String, generics: String = "", arguments: String = "") = """
    |impl$generics GeneratorField<$value> for GeneratorField$field$arguments {
    |    fn generate(&self, generator: &impl Generator, path: Vec<String>) -> $value {
    |        generator.generate_${field.lowercase()}(path, self)
    |    }
    |}
""".trimMargin()

/**
 * The other languages hand every field to one `generate` function, which finds out what kind of field it got at
 * runtime. Rust cannot, so a `Generator` has a function for each kind of field, and a field knows which one generates
 * it: `generate` only passes the field on. A field of a generated value borrows the generator in its `generate`
 * closure, hence the lifetime.
 */
private val sharedGenerator = RawElement(
    """
    |pub type Annotations = Vec<HashMap<String, Box<dyn std::any::Any>>>;
    |pub trait GeneratorField<T> {
    |    fn generate(&self, generator: &impl Generator, path: Vec<String>) -> T;
    |}
    |pub struct GeneratorFieldString {
    |    pub regex: Option<String>,
    |    pub annotations: Annotations,
    |}
    |pub struct GeneratorFieldInteger64 {
    |    pub min: Option<i64>,
    |    pub max: Option<i64>,
    |    pub annotations: Annotations,
    |}
    |pub struct GeneratorFieldInteger32 {
    |    pub min: Option<i32>,
    |    pub max: Option<i32>,
    |    pub annotations: Annotations,
    |}
    |pub struct GeneratorFieldNumber64 {
    |    pub min: Option<f64>,
    |    pub max: Option<f64>,
    |    pub annotations: Annotations,
    |}
    |pub struct GeneratorFieldNumber32 {
    |    pub min: Option<f32>,
    |    pub max: Option<f32>,
    |    pub annotations: Annotations,
    |}
    |pub struct GeneratorFieldBoolean {
    |    pub annotations: Annotations,
    |}
    |pub struct GeneratorFieldBytes {
    |    pub annotations: Annotations,
    |}
    |pub struct GeneratorFieldEnum {
    |    pub values: Vec<String>,
    |    pub annotations: Annotations,
    |    pub r#type: TypeId,
    |}
    |pub struct GeneratorFieldUnion {
    |    pub variants: Vec<String>,
    |    pub annotations: Annotations,
    |    pub r#type: TypeId,
    |}
    |pub struct GeneratorFieldArray<'a, T> {
    |    pub generate: Box<dyn Fn(Vec<String>) -> T + 'a>,
    |}
    |pub struct GeneratorFieldNullable<'a, T> {
    |    pub generate: Box<dyn Fn(Vec<String>) -> T + 'a>,
    |}
    |pub struct GeneratorFieldDict<'a, T> {
    |    pub generate: Box<dyn Fn(Vec<String>) -> T + 'a>,
    |}
    |pub struct GeneratorFieldShape<'a, T> {
    |    pub annotations: HashMap<String, Annotations>,
    |    pub generate: Box<dyn Fn(Vec<String>) -> T + 'a>,
    |    pub r#type: TypeId,
    |}
    |${generatorFields.entries.joinToString("\n") { (field, value) -> generatorFieldImpl(field, value) }}
    |${generatorFieldsOfGenerated.entries.joinToString("\n") { (field, value) -> generatorFieldImpl(field, value, generics = "<'a, T>", arguments = "<'a, T>") }}
    |// A field of type `Any` is generated as a string.
    |impl GeneratorField<AnyValue> for GeneratorFieldString {
    |    fn generate(&self, generator: &impl Generator, path: Vec<String>) -> AnyValue {
    |        AnyValue::new(generator.generate_string(path, self))
    |    }
    |}
    |pub trait Generator: Sized {
    |${generatorFields.entries.joinToString("\n") { (field, value) -> "    fn generate_${field.lowercase()}(&self, path: Vec<String>, field: &GeneratorField$field) -> $value;" }}
    |${generatorFieldsOfGenerated.entries.joinToString("\n") { (field, value) -> "    fn generate_${field.lowercase()}<T>(&self, path: Vec<String>, field: &GeneratorField$field<T>) -> $value;" }}
    |    fn generate<T>(&self, path: Vec<String>, field: impl GeneratorField<T>) -> T {
    |        field.generate(self, path)
    |    }
    |}
    """.trimMargin(),
)

/**
 * A model derives `Clone`, `Default` and `PartialEq`, which a `Box<dyn Any>` has none of. So a value of type `Any` is
 * shared instead of owned: a clone points to the same value, and two values are equal when they are the same value.
 */
private val sharedAnyValue = RawElement(
    """
    |#[derive(Debug, Clone)]
    |pub struct AnyValue(pub std::sync::Arc<dyn std::any::Any + Send + Sync>);
    |impl AnyValue {
    |    pub fn new<T: std::any::Any + Send + Sync>(value: T) -> Self {
    |        AnyValue(std::sync::Arc::new(value))
    |    }
    |    pub fn downcast_ref<T: std::any::Any>(&self) -> Option<&T> {
    |        self.0.downcast_ref::<T>()
    |    }
    |}
    |impl Default for AnyValue {
    |    fn default() -> Self {
    |        AnyValue::new(())
    |    }
    |}
    |impl PartialEq for AnyValue {
    |    fn eq(&self, other: &Self) -> bool {
    |        std::sync::Arc::ptr_eq(&self.0, &other.0)
    |    }
    |}
    """.trimMargin(),
)

/**
 * What can go wrong with a message from outside. The generated code returns the first four when it reads a raw
 * request or response. `Deserialization` and `Transport` are for the deserializers and the transport you provide.
 */
private val sharedErrorAndServer = listOf(
    RawElement(
        """
        #[derive(Debug, Clone, PartialEq)]
        pub enum WirespecError {
            MissingPathSegment(usize),
            MissingParameter(String),
            MissingBody,
            UnexpectedStatus(i32),
            Deserialization(String),
            Transport(String),
        }
        impl std::fmt::Display for WirespecError {
            fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
                match self {
                    WirespecError::MissingPathSegment(index) => write!(f, "Path segment {} is missing", index),
                    WirespecError::MissingParameter(name) => write!(f, "Parameter {} is missing", name),
                    WirespecError::MissingBody => write!(f, "Body is missing"),
                    WirespecError::UnexpectedStatus(status) => write!(f, "Cannot match response with status: {}", status),
                    WirespecError::Deserialization(message) => write!(f, "Cannot deserialize: {}", message),
                    WirespecError::Transport(message) => write!(f, "Cannot transport: {}", message),
                }
            }
        }
        impl std::error::Error for WirespecError {}
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
