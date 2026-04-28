package community.flock.wirespec.emitters.java

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.needImports
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertClientServer
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.emit.placeInPackage
import community.flock.wirespec.ir.emit.prependImports
import community.flock.wirespec.ir.emit.withSharedSource
import community.flock.wirespec.ir.generator.JavaGenerator
import community.flock.wirespec.ir.generator.generateJava
import community.flock.wirespec.ir.transformer.SanitizationConfig
import community.flock.wirespec.ir.transformer.injectEnumLabelField
import community.flock.wirespec.ir.transformer.sanitizeNames
import community.flock.wirespec.ir.transformer.toGetterAccessors
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType

open class JavaIrEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : IrEmitter, HasPackageName {

    override val generator = JavaGenerator

    override val extension = FileExtension.Java

    override fun transformTestFile(file: File): File = file.transformTypeDescriptors()

    private val wirespecImports = listOf(import("$DEFAULT_SHARED_PACKAGE_STRING.java", "Wirespec"))

    private val sanitizationConfig = SanitizationConfig(
        reservedKeywords = reservedKeywords,
        escapeKeyword = { "_$it" },
        fieldNameCase = { name ->
            if (name.parts.size > 1) Name(listOf(name.camelCase())) else name
        },
        parameterNameCase = { name -> Name(listOf(name.camelCase())) },
        sanitizeSymbol = { it.sanitizeSymbol() },
        extraStatementTransforms = { stmt, tr ->
            when {
                stmt is FunctionCall && stmt.name.value() == "validate" ->
                    stmt.copy(typeArguments = emptyList()).transformChildren(tr)
                else -> stmt.transformChildren(tr)
            }
        },
    )

    override val shared = object : Shared {
        override val packageString: String = "$DEFAULT_SHARED_PACKAGE_STRING.java"

        private val wirespecShared = AstShared(packageString).convert()

        private val imports = listOf(
            import("java.lang.reflect", "Type"),
            import("java.lang.reflect", "ParameterizedType"),
            import("java.util", "List"),
            import("java.util", "Map"),
        )

        private val clientServer = AstShared(packageString).convertClientServer().map {
            it.toGetterAccessors { name ->
                when (name.value()) {
                    "client" -> Name.of("getClient")
                    "server" -> Name.of("getServer")
                    else -> null
                }
            }
        } + listOf(
            raw(
                """
                |public static Type getType(final Class<?> actualTypeArguments, final Class<?> rawType) {
                |  if(rawType != null) {
                |    return new ParameterizedType() {
                |      public Type getRawType() { return rawType; }
                |      public Type[] getActualTypeArguments() { return new Class<?>[]{actualTypeArguments}; }
                |      public Type getOwnerType() { return null; }
                |    };
                |  }
                |  else { return actualTypeArguments; }
                |}
                """.trimMargin(),
            ),
        )

        private val wirespecFile = wirespecShared
            .transform {
                matchingElements { file: File ->
                    val (packageElements, rest) = file.elements.partition { it is Package }
                    file.copy(elements = packageElements + imports + rest)
                }
                injectAfter { namespace: Namespace ->
                    if (namespace.name == Name.of("Wirespec")) clientServer else emptyList()
                }
            }

        override val source: String = wirespecFile.generateJava()
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> =
        super.emit(module, logger).withSharedSource(emitShared) {
            File(
                Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.java").toDir() + "Wirespec"),
                listOf(RawElement(shared.source))
            )
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val file = super.emit(definition, module, logger)
        return file.copy(name = Name.of(file.name.pascalCase().sanitizeSymbol()))
            .prependImports(wirespecImports.takeIf { module.needImports() })
            .placeInPackage(packageName = packageName, definition = definition)
    }

    override fun emit(type: AstType, module: Module): File =
        type.convertWithValidation(module)
            .sanitizeNames(sanitizationConfig)

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .injectEnumLabelField(
            sanitizeEntry = { it.sanitizeEnum() },
            extraElements = {
                listOf(
                    function("label") {
                        returnType(Type.String)
                        returns(VariableReference(Name.of("label")))
                    }
                )
            },
        )
        .sanitizeNames(sanitizationConfig)

    override fun emit(union: Union): File = union
        .convert()
        .sanitizeNames(sanitizationConfig)

    override fun emit(refined: Refined): File = refined
        .convert()
        .applyRefinedStructShape(refined)
        .sanitizeNames(sanitizationConfig)

    override fun emit(endpoint: Endpoint): File = endpoint
        .convert()
        .injectHandleFunction(endpoint)
        .transformTypeDescriptors()
        .sanitizeNames(sanitizationConfig)
        .prependImports(endpoint.buildModelImports(packageName).takeIf { it.isNotEmpty() })

    override fun emit(channel: Channel): File {
        val fullyQualifiedPrefix = if (channel.identifier.value == channel.reference.value) {
            "${packageName.value}.model."
        } else {
            ""
        }
        return channel.convert()
            .sanitizeNames(sanitizationConfig)
            .applyFunctionalInterface(fullyQualifiedPrefix)
    }

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.buildModelImports(packageName)
        val endpointImport = import("${packageName.value}.endpoint", endpoint.identifier.value)
        val endpointName = endpoint.identifier.value

        val file = super.emitEndpointClient(endpoint)
            .sanitizeNames(sanitizationConfig)
            .transformTypeDescriptors()
            .wrapAsyncReturnInThenApply(endpointName)

        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase().sanitizeSymbol()),
            elements = listOf(Package(subPackageName.value)) +
                wirespecImports +
                imports +
                listOf(endpointImport) +
                file.elements
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        val imports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .filter { imp -> endpoints.none { it.identifier.value == imp.value } }
            .map { import("${packageName.value}.model", it.value) }
        val endpointImports = endpoints.map { import("${packageName.value}.endpoint", it.identifier.value) }
        val clientImports = endpoints.map { import("${packageName.value}.client", "${it.identifier.value}Client") }
        val allImports = imports + endpointImports + clientImports
        val file = super.emitClient(endpoints, logger).sanitizeNames(sanitizationConfig)
        return File(
            name = Name.of(packageName.toDir() + file.name.pascalCase().sanitizeSymbol()),
            elements = listOf(Package(packageName.value)) +
                wirespecImports +
                allImports +
                file.elements
        )
    }

    private fun String.sanitizeSymbol(): String = this
        .split(".", " ", "-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .sanitizeFirstIsDigit()

    private fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    private fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    private fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while",
            "true", "false"
        )
    }

}
