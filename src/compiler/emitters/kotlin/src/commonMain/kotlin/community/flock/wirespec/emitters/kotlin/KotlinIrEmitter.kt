package community.flock.wirespec.emitters.kotlin

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.transformer.SanitizationConfig
import community.flock.wirespec.ir.transformer.sanitizeFieldName
import community.flock.wirespec.ir.transformer.sanitizeNames
import community.flock.wirespec.ir.emit.withSharedSource
import community.flock.wirespec.ir.emit.placeInPackage
import community.flock.wirespec.ir.emit.prependImports
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
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertClientServer
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.Constructor
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.withLabelField
import community.flock.wirespec.ir.generator.KotlinGenerator
import community.flock.wirespec.ir.generator.generateKotlin
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.ir.core.Enum as LanguageEnum
import community.flock.wirespec.ir.core.File as LanguageFile
import community.flock.wirespec.ir.core.Package as LanguagePackage
import community.flock.wirespec.ir.core.Type as LanguageType

open class KotlinIrEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : IrEmitter, HasPackageName {

    override val generator = KotlinGenerator

    override val extension = FileExtension.Kotlin

    private val wirespecImports = listOf(
        import("$DEFAULT_SHARED_PACKAGE_STRING.kotlin", "Wirespec"),
        import("kotlin.reflect", "typeOf"),
        import("kotlin.reflect", "KClass"),
    )

    private val sanitizationConfig: SanitizationConfig by lazy {
        SanitizationConfig(
            reservedKeywords = reservedKeywords,
            escapeKeyword = { it.addBackticks() },
            fieldNameCase = { name ->
                val sanitized = if (name.parts.size > 1) name.camelCase() else name.value().sanitizeSymbol()
                Name(listOf(sanitized))
            },
            parameterNameCase = { name -> Name(listOf(name.camelCase().sanitizeSymbol())) },
            sanitizeSymbol = { it.sanitizeSymbol() },
            extraStatementTransforms = { stmt, tr ->
                when (stmt) {
                    is FunctionCall -> if (stmt.name.value() == "validate") {
                        stmt.copy(typeArguments = emptyList()).transformChildren(tr)
                    } else stmt.transformChildren(tr)
                    is ConstructorStatement -> ConstructorStatement(
                        type = tr.transformType(stmt.type),
                        namedArguments = stmt.namedArguments.map { (name, expr) ->
                            sanitizationConfig.sanitizeFieldName(name) to tr.transformExpression(expr)
                        }.toMap(),
                    )
                    else -> stmt.transformChildren(tr)
                }
            },
        )
    }

    override val shared = object : Shared {
        override val packageString = "$DEFAULT_SHARED_PACKAGE_STRING.kotlin"

        private val clientServer = AstShared(packageString).convertClientServer()

        override val source = AstShared(packageString)
            .convert()
            .transform {
                matchingElements { file: LanguageFile ->
                    val (packageElements, rest) = file.elements.partition { it is LanguagePackage }
                    file.copy(elements = packageElements + import("kotlin.reflect", "KType") + import("kotlin.reflect", "KClass") + rest)
                }
                injectAfter { namespace: Namespace ->
                    if (namespace.name == Name.of("Wirespec")) clientServer
                    else emptyList()
                }
            }
            .generateKotlin()
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> =
        super.emit(module, logger).withSharedSource(emitShared) {
            File(
                Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.kotlin").toDir() + "Wirespec"),
                listOf(RawElement(shared.source))
            )
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val file = super.emit(definition, module, logger)
        return file
            .prependImports(wirespecImports.takeIf { module.needImports() })
            .placeInPackage(packageName = packageName, definition = definition)
    }

    override fun emit(type: Type, module: Module): File =
        type.convertWithValidation(module)
            .sanitizeNames(sanitizationConfig)
            .transform {
                matchingElements { struct: Struct ->
                    if (struct.fields.isEmpty()) struct.copy(constructors = listOf(Constructor(emptyList(), emptyList())))
                    else struct
                }
            }

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .sanitizeNames(sanitizationConfig)
        .transform {
            matchingElements { languageEnum: LanguageEnum ->
                languageEnum
                    .withLabelField(sanitizeEntry = { it.sanitizeEnum() })
            }
        }

    override fun emit(union: Union): File = union
        .convert()
        .sanitizeNames(sanitizationConfig)

    override fun emit(refined: Refined): File {
        val file = refined.convert().sanitizeNames(sanitizationConfig)
        val struct = file.findElement<Struct>()
        val updatedStruct = struct?.copy(
            fields = struct.fields.map { f -> f.copy(isOverride = true) },
            elements = struct.elements.map { element ->
                if (element is LanguageFunction) {
                    element.copy(isOverride = true)
                } else element
            },
        )
        return LanguageFile(Name.of(refined.identifier.sanitize()), updatedStruct?.let { listOf(it) } ?: emptyList())
    }

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        val file = endpoint.convert()
        val endpointNamespace = file.findElement<Namespace>()!!
        val body = endpointNamespace.injectCompanionObject(endpoint)
        val sanitized = LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
            .sanitizeNames(sanitizationConfig)
        return if (imports.isNotEmpty()) {
            sanitized.copy(elements = imports + sanitized.elements)
        } else {
            sanitized
        }
    }

    override fun emit(channel: Channel): File {
        val imports = channel.buildImports()
        val file = channel.convert().sanitizeNames(sanitizationConfig)
        return if (imports.isNotEmpty()) file.copy(elements = imports + file.elements)
        else file
    }

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        val endpointImport = import("${packageName.value}.endpoint", endpoint.identifier.value)
        val file = super.emitEndpointClient(endpoint).sanitizeNames(sanitizationConfig)
        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase()),
            elements = buildList {
                add(LanguagePackage(subPackageName.value))
                addAll(wirespecImports)
                addAll(imports)
                add(endpointImport)
                addAll(file.elements)
            }
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        val imports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .map { import("${packageName.value}.model", it.value) }
        val endpointImports = endpoints
            .map { import("${packageName.value}.endpoint", it.identifier.value) }
        val clientImports = endpoints
            .map { import("${packageName.value}.client", "${it.identifier.value}Client") }
        val allImports = imports + endpointImports + clientImports
        val file = super.emitClient(endpoints, logger).sanitizeNames(sanitizationConfig)
        return File(
            name = Name.of(packageName.toDir() + file.name.pascalCase()),
            elements = buildList {
                add(LanguagePackage(packageName.value))
                addAll(wirespecImports)
                addAll(allImports)
                addAll(file.elements)
            }
        )
    }

    private fun Identifier.sanitize(): String = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .sanitizeFirstIsDigit()
        .let { if (this is FieldIdentifier) it.sanitizeKeywords() else it }

    private fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    private fun String.sanitizeKeywords() = if (this in reservedKeywords) addBackticks() else this

    private fun String.sanitizeSymbol(): String = this
        .split(".", " ", "-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .sanitizeFirstIsDigit()

    private fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

    private fun Definition.buildImports(): List<Import> = importReferences()
        .distinctBy { it.value }
        .map { import("${packageName.value}.model", it.value) }

    private fun Namespace.injectCompanionObject(endpoint: Endpoint): Namespace =
        transform {
            injectAfter { iface: Interface ->
                if (iface.name == Name.of("Handler")) listOf(buildCompanionObject(endpoint)) else emptyList()
            }
        }

    private fun buildCompanionObject(endpoint: Endpoint): RawElement {
        val pathTemplate = "/" + endpoint.path.joinToString("/") {
            when (it) {
                is Endpoint.Segment.Literal -> it.value
                is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            }
        }
        return """
            |companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |  override val pathTemplate = "$pathTemplate"
            |  override val method = "${endpoint.method}"
            |  override fun server(serialization: Wirespec.Serialization) = object : Wirespec.ServerEdge<Request, Response<*>> {
            |    override fun from(request: Wirespec.RawRequest) = fromRawRequest(serialization, request)
            |    override fun to(response: Response<*>) = toRawResponse(serialization, response)
            |  }
            |  override fun client(serialization: Wirespec.Serialization) = object : Wirespec.ClientEdge<Request, Response<*>> {
            |    override fun to(request: Request) = toRawRequest(serialization, request)
            |    override fun from(response: Wirespec.RawResponse) = fromRawResponse(serialization, response)
            |  }
            |}
        """.trimMargin().let(::raw)
    }

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "as", "break", "class", "continue", "do",
            "else", "false", "for", "fun", "if",
            "in", "interface", "internal", "is", "null",
            "object", "open", "package", "return", "super",
            "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while", "private", "public"
        )
    }

}
