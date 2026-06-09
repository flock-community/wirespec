package community.flock.wirespec.emitters.kotlin

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
import community.flock.wirespec.ir.transformer.ensureEmptyStructHasConstructor
import community.flock.wirespec.ir.transformer.injectEnumLabelField
import community.flock.wirespec.ir.transformer.markMembersAsOverride
import community.flock.wirespec.ir.transformer.sanitizeFieldName
import community.flock.wirespec.ir.transformer.sanitizeNames
import community.flock.wirespec.ir.emit.placeInPackage
import community.flock.wirespec.ir.emit.prependImports
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.irNeedsWirespecImport
import community.flock.wirespec.compiler.core.emit.PackageName
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
import community.flock.wirespec.ir.converter.convertToGenerator
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.collectCustomTypeNames
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.generator.KotlinGenerator
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

    // Model bodies serialize by reflection on the property name, so keep the wire name verbatim
    // and backtick-escape non-identifier names (`house-number`); endpoints keep camelCase.
    private val modelSanitizationConfig: SanitizationConfig by lazy {
        sanitizationConfig.copy(
            fieldNameCase = { name -> Name(listOf(name.value())) },
            sanitizeSymbol = { it.sanitizeFieldSymbol() },
        )
    }

    override fun emitShared(): File? {

        val packageName = PackageName("$DEFAULT_SHARED_PACKAGE_STRING.kotlin")

        val clientServer = packageName.convertClientServer()

        val wirespecShared = packageName.convert()
            .transform {
                matchingElements { file: File ->
                    val (packageElements, rest) = file.elements.partition { it is Package }
                    file.copy(elements = packageElements + import("kotlin.reflect", "KType") + rest)
                }
                injectAfter { namespace: Namespace ->
                    if (namespace.name == Name.of("Wirespec")) clientServer else emptyList()
                }
            }

        return if (emitShared.value) {
            wirespecShared.copy(name = Name.of(packageName.toDir() + wirespecShared.name.value()))
        } else {
            null
        }
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val file = super.emit(definition, module, logger)
        return file
            .prependImports(wirespecImports.takeIf { module.irNeedsWirespecImport() })
            .placeInPackage(packageName = packageName, definition = definition)
    }

    override fun emitGenerator(definition: Definition, module: Module): LanguageFile? {
        val generatorFile = when (definition) {
            is Type -> definition.convertToGenerator(module)
            is Enum -> definition.convertToGenerator()
            is Refined -> definition.convertToGenerator()
            is Union -> definition.convertToGenerator()
            else -> return null
        }
        val sanitized = generatorFile.sanitizeNames(sanitizationConfig)
        val generatorOwnName = "${definition.identifier.value}Generator"
        val modelImports = sanitized.collectCustomTypeNames()
            .asSequence()
            .filterNot { it.startsWith("Wirespec.") || it == "Wirespec" }
            .filterNot { it == generatorOwnName }
            .map { it.substringBefore('<') }
            .distinct()
            .map { import("${packageName.value}.model", it) }
            .toList()
        return sanitized
            .prependImports(wirespecImports + modelImports)
            .placeInPackage(packageName = packageName, subPackage = "generator")
    }

    override fun emit(type: Type, module: Module): File =
        type.convertWithValidation(module)
            .sanitizeNames(modelSanitizationConfig)
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
        val updatedStruct = file.findElement<Struct>()?.markMembersAsOverride()
        return LanguageFile(Name.of(refined.identifier.sanitize()), listOfNotNull(updatedStruct))
    }

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.buildModelImports(packageName)
        val endpointNamespace = endpoint.convert().findElement<Namespace>()!!
        val body = endpointNamespace.injectCompanionObject(endpoint).injectApiAlias()
        return LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
            .sanitizeNames(sanitizationConfig)
            .prependImports(imports.takeIf { it.isNotEmpty() })
    }

    override fun emit(channel: Channel): File = channel
        .convert()
        .sanitizeNames(sanitizationConfig)
        .prependImports(channel.buildModelImports(packageName).takeIf { it.isNotEmpty() })

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.buildModelImports(packageName)
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

    /**
     * Field-name sanitizer for model bodies: preserve the name verbatim (it is the JSON wire key)
     * and only backtick-escape it when it is not a bare Kotlin identifier. Kotlin/JVM allow
     * backtick identifiers containing `-` or starting with a digit, which reflect to the exact name.
     */
    private fun String.sanitizeFieldSymbol(): String = if (isBareIdentifier()) this else addBackticks()

    private fun String.isBareIdentifier(): Boolean =
        isNotEmpty() && (first().isLetter() || first() == '_') && all { it.isLetterOrDigit() || it == '_' }

    private fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

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
