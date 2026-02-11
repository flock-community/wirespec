package community.flock.wirespec.emitters.kotlin

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.language.emit.IrEmitter
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.needImports
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
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.converter.convertConstraint
import community.flock.wirespec.language.converter.convertWithValidation
import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.Constructor
import community.flock.wirespec.language.core.File
import community.flock.wirespec.language.core.Import
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.RawElement
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.Static
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.findAll
import community.flock.wirespec.language.core.findElement
import community.flock.wirespec.language.core.function
import community.flock.wirespec.language.core.injectAfter
import community.flock.wirespec.language.core.raw
import community.flock.wirespec.language.core.renameType
import community.flock.wirespec.language.core.transformMatchingElements
import community.flock.wirespec.language.generator.generateJava
import community.flock.wirespec.language.generator.generateKotlin
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.language.core.Enum as LanguageEnum
import community.flock.wirespec.language.core.File as LanguageFile
import community.flock.wirespec.language.core.Field as LanguageField
import community.flock.wirespec.language.core.Function as LanguageFunction
import community.flock.wirespec.language.core.Package as LanguagePackage
import community.flock.wirespec.language.core.Type as LanguageType

open class KotlinIrEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : IrEmitter, HasPackageName {

    override fun File.generate(): String = generateKotlin()

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.kotlin.Wirespec
        |import kotlin.reflect.typeOf
        |
    """.trimMargin()

    override val extension = FileExtension.Kotlin

    override val shared = object : Shared {
        val clientServer = """
            |interface ServerEdge<Req : Request<*>, Res : Response<*>> {
            |    fun from(request: RawRequest): Req
            |    fun to(response: Res): RawResponse
            |}
            |interface ClientEdge<Req : Request<*>, Res : Response<*>> {
            |    fun to(request: Req): RawRequest
            |    fun from(response: RawResponse): Res
            |}
            |interface Client<Req : Request<*>, Res : Response<*>> {
            |    val pathTemplate: String
            |    val method: String
            |    fun client(serialization: Serialization): ClientEdge<Req, Res>
            |}
            |interface Server<Req : Request<*>, Res : Response<*>> {
            |    val pathTemplate: String
            |    val method: String
            |    fun server(serialization: Serialization): ServerEdge<Req, Res>
            |}
        """.trimMargin()
        override val packageString = "$DEFAULT_SHARED_PACKAGE_STRING.kotlin"
        override val source = AstShared(packageString)
            .convert()
            .renameType("Type", "KType")
            .transformMatchingElements { file: LanguageFile ->
                val (packageElements, rest) = file.elements.partition { it is LanguagePackage }
                file.copy(elements = packageElements + Import("kotlin.reflect", LanguageType.Custom("KType")) + rest)
            }
            .injectAfter { static: Static ->
                if (static.name == "Wirespec") listOf(RawElement(clientServer))
                else emptyList()
            }
            .generateKotlin()
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> =
        super.emit(module, logger).let {
            if (emitShared.value) it + File(
                PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.kotlin").toDir() + "Wirespec",
                listOf(RawElement(shared.source))
            )
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): File =
        super.emit(definition, module, logger).let { file ->
            val subPackageName = packageName + definition
            File(
                name = subPackageName.toDir() + file.name,
                elements = listOf(LanguagePackage(subPackageName.value)) +
                    (if (module.needImports()) listOf(RawElement(import)) else emptyList()) +
                    file.elements
            )
        }

    fun Identifier.sanitize() = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()
        .let { if (this is FieldIdentifier) it.sanitizeKeywords() else it }

    fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    fun String.sanitizeKeywords() = if (this in reservedKeywords) addBackticks() else this

    override fun emit(type: Type, module: Module): File =
        type.convertWithValidation(module)
            .renameType("Type", "KType")
            .transformMatchingElements { struct: Struct ->
                if (struct.fields.isEmpty()) struct.copy(constructors = listOf(Constructor(emptyList(), emptyList())))
                else struct
            }

    private fun Definition.emitEndpointImports() = importReferences()
        .distinctBy { it.value }
        .map { "import ${packageName.value}.model.${it.value}" }.joinToString("\n") { it.trimStart() }

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.emitEndpointImports()
        val file = endpoint.convert().renameType("Type", "KType")
        val endpointInterface = file.findElement<Interface>()!!
        val body = endpointInterface
            .injectCompanionObject(endpoint)
            .toStatic()

        return if (imports.isNotEmpty()) LanguageFile(endpoint.identifier.sanitize(), listOf(RawElement(imports), body))
        else LanguageFile(endpoint.identifier.sanitize(), listOf(body))
    }

    private fun Interface.injectCompanionObject(endpoint: Endpoint): Interface {
        val companion = companionObject(endpoint)
        val rawHandleFunction = raw(emitHandleFunction(endpoint))
        val targetFunctionName = endpoint.identifier.value.replaceFirstChar { it.lowercase() }

        return transformMatchingElements { iface: Interface ->
            if (iface.name == "Handler") {
                val transformed = iface.transformMatchingElements { fn: LanguageFunction ->
                    if (fn.name == targetFunctionName) rawHandleFunction else fn
                }
                transformed.injectAfter { iface2: Interface -> listOf(companion) }
            } else {
                iface
            }
        }
    }

    private fun Interface.toStatic(): Static = Static(
        name = name,
        elements = elements,
        extends = extends.firstOrNull(),
    )

    open fun emitHandleFunction(endpoint: Endpoint): String {
        return endpoint.convert().renameType("Type", "KType")
            .findAll<Interface>().firstOrNull { it.name == "Handler" }
            ?.findElement<LanguageFunction>()
            ?.generateKotlin()
            ?.let { it + "\n" }
            ?: ""
    }

    fun companionObject(endpoint: Endpoint): RawElement {
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

    private fun Definition.emitChannelImports() = importReferences()
        .distinctBy { it.value }
        .map { "import ${packageName.value}.model.${it.value}" }.joinToString("\n") { it.trimStart() }

    override fun emit(channel: Channel): File {
        val imports = channel.emitChannelImports()
        val file = channel.convert().renameType("Type", "KType")
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements)
        else file
    }

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .renameType("Type", "KType")
        .transformMatchingElements { languageEnum: LanguageEnum ->
            languageEnum.copy(
                entries = languageEnum.entries.map {
                    LanguageEnum.Entry(it.name.sanitizeEnum(), listOf("\"${it.name}\""))
                },
                fields = listOf(
                    LanguageField("label", LanguageType.String, isOverride = true),
                ),
                constructors = listOf(
                    Constructor(
                        parameters = listOf(
                            community.flock.wirespec.language.core.Parameter(
                                "label",
                                LanguageType.String
                            )
                        ),
                        body = listOf(Assignment("this.label", RawExpression("label"), true)),
                    ),
                ),
                elements = listOf(
                    function("toString", isOverride = true) {
                        returnType(LanguageType.String)
                        returns(RawExpression("label"))
                    },
                ),
            )
        }

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

    override fun emit(union: Union): File = union
        .convert()
        .renameType("Type", "KType")

    override fun emit(refined: Refined): File {
        val file = refined.convert().renameType("Type", "KType")
        val struct = file.findElement<Struct>()!!
        val toStringExpr = when (refined.reference.type) {
            is Reference.Primitive.Type.String -> "value"
            else -> "value.toString()"
        }
        val updatedStruct = struct.copy(
            fields = struct.fields.map { f -> f.copy(isOverride = true) },
            elements = listOf(
                function("toString", isOverride = true) {
                    returnType(LanguageType.String)
                    returns(RawExpression(toStringExpr))
                },
                function("validate", isOverride = true) {
                    returnType(LanguageType.Boolean)
                    returns(refined.reference.convertConstraint(VariableReference("value")))
                },
            ),
        )
        return LanguageFile(refined.identifier.sanitize(), listOf(updatedStruct))
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
