package community.flock.wirespec.emitters.scala

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.addBackticks
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
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.TypeParameter
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.flattenNestedStructs
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.`interface`
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.transformer.SanitizationConfig
import community.flock.wirespec.ir.transformer.ensureEmptyStructHasConstructor
import community.flock.wirespec.ir.transformer.injectEnumLabelField
import community.flock.wirespec.ir.transformer.markMembersAsOverride
import community.flock.wirespec.ir.transformer.sanitizeFieldName
import community.flock.wirespec.ir.transformer.sanitizeNames
import community.flock.wirespec.ir.emit.withSharedSource
import community.flock.wirespec.ir.emit.placeInPackage
import community.flock.wirespec.ir.emit.prependImports
import community.flock.wirespec.ir.generator.ScalaGenerator
import community.flock.wirespec.ir.generator.generateScala
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.Enum as LanguageEnum
import community.flock.wirespec.ir.core.File as LanguageFile
import community.flock.wirespec.ir.core.Import as LanguageImport
import community.flock.wirespec.ir.core.Package as LanguagePackage
import community.flock.wirespec.ir.core.Type as LanguageType

open class ScalaIrEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : IrEmitter, HasPackageName {

    override val generator = ScalaGenerator

    override val extension = FileExtension.Scala

    private val wirespecImports = listOf(
        import("$DEFAULT_SHARED_PACKAGE_STRING.scala", "Wirespec"),
        import("scala.reflect", "ClassTag"),
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
                when {
                    stmt is FunctionCall && stmt.name.value() == "validate" ->
                        stmt.copy(typeArguments = emptyList()).transformChildren(tr)
                    stmt is ConstructorStatement -> ConstructorStatement(
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
        override val packageString = "$DEFAULT_SHARED_PACKAGE_STRING.scala"

        private val clientServer = AstShared(packageString).convertClientServer()

        override val source = AstShared(packageString)
            .convert()
            .transform {
                matchingElements { file: LanguageFile ->
                    val (packageElements, rest) = file.elements.partition { it is LanguagePackage }
                    file.copy(elements = packageElements + import("scala.reflect", "ClassTag") + rest)
                }
                matchingElements { ns: Namespace ->
                    if (ns.name != Name.of("Wirespec")) return@matchingElements ns
                    val newElements = ns.elements.flatMap { element ->
                        if (element !is Interface || element.name.pascalCase() !in setOf("Request", "Response")) {
                            return@flatMap listOf(element)
                        }
                        val nestedHeaders = element.elements.filterIsInstance<Interface>()
                            .firstOrNull { it.name.pascalCase() == "Headers" }
                            ?: return@flatMap listOf(element)
                        listOf(
                            Namespace(element.name, listOf(nestedHeaders)),
                            element.copy(
                                elements = element.elements.filter {
                                    !(it is Interface && it.name.pascalCase() == "Headers")
                                },
                                fields = element.fields.map { f ->
                                    if (f.name.value() == "headers") {
                                        f.copy(type = LanguageType.Custom("${element.name.pascalCase()}.Headers"))
                                    } else f
                                },
                            ),
                        )
                    }
                    ns.copy(elements = newElements)
                }
                injectAfter { namespace: Namespace ->
                    if (namespace.name == Name.of("Wirespec")) clientServer
                    else emptyList()
                }
            }
            .generateScala()
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> =
        super.emit(module, logger).withSharedSource(emitShared) {
            File(
                Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.scala").toDir() + "Wirespec"),
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
            .ensureEmptyStructHasConstructor()

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .sanitizeNames(sanitizationConfig)
        .injectEnumLabelField(sanitizeEntry = { it.sanitizeEnum() })

    override fun emit(union: Union): File = union
        .convert()
        .sanitizeNames(sanitizationConfig)

    override fun emit(refined: Refined): File {
        val file = refined.convert().sanitizeNames(sanitizationConfig)
        val updatedStruct = file.findElement<Struct>()!!
            .markMembersAsOverride()
            .convertToStringCallsToFieldAccess()
        return LanguageFile(Name.of(refined.identifier.sanitize()), listOf(updatedStruct))
    }

    override fun emit(endpoint: Endpoint): File {
        val endpointNamespace = endpoint.convert().findElement<Namespace>()!!
        val flattened = endpointNamespace.flattenNestedStructs()
        val body = flattened
            .injectHandleFunction()
            .appendClientServerObjects(endpoint, isRequestObject(flattened))
        return LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
            .sanitizeNames(sanitizationConfig)
            .prependImports(endpoint.buildModelImports(packageName).takeIf { it.isNotEmpty() })
    }

    override fun emit(channel: Channel): File = channel
        .convert()
        .sanitizeNames(sanitizationConfig)
        .prependImports(channel.buildModelImports(packageName).takeIf { it.isNotEmpty() })

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.buildModelImports(packageName)
        val endpointImport = import("${packageName.value}.endpoint", endpoint.identifier.value)
        val file = super.emitEndpointClient(endpoint).sanitizeNames(sanitizationConfig).addIdentityTypeToCall()
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
        val file = super.emitClient(endpoints, logger).sanitizeNames(sanitizationConfig).addIdentityTypeToCall()
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

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "abstract", "case", "class", "def", "do",
            "else", "extends", "false", "final", "for",
            "forSome", "if", "implicit", "import", "lazy",
            "match", "new", "null", "object", "override",
            "package", "private", "protected", "return", "sealed",
            "super", "this", "throw", "trait", "true",
            "try", "type", "val", "var", "while",
            "with", "yield", "given", "using", "enum",
            "export", "then",
        )
    }

}
