package community.flock.wirespec.emitters.kotlin

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.emit.IrEmitter
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
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertConstraint
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.Constructor
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.`interface`
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

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.kotlin.Wirespec
        |import kotlin.reflect.typeOf
        |
    """.trimMargin()

    override val extension = FileExtension.Kotlin

    override val shared = object : Shared {
        override val packageString = "$DEFAULT_SHARED_PACKAGE_STRING.kotlin"

        private val clientServer = buildList {
            add(
                `interface`("ServerEdge") {
                    typeParam(type("Req"), type("Request", LanguageType.Wildcard))
                    typeParam(type("Res"), type("Response", LanguageType.Wildcard))
                    function("from") {
                        returnType(type("Req"))
                        arg("request", type("RawRequest"))
                    }
                    function("to") {
                        returnType(type("RawResponse"))
                        arg("response", type("Res"))
                    }
                },
            )
            add(
                `interface`("ClientEdge") {
                    typeParam(type("Req"), type("Request", LanguageType.Wildcard))
                    typeParam(type("Res"), type("Response", LanguageType.Wildcard))
                    function("to") {
                        returnType(type("RawRequest"))
                        arg("request", type("Req"))
                    }
                    function("from") {
                        returnType(type("Res"))
                        arg("response", type("RawResponse"))
                    }
                },
            )
            add(
                `interface`("Client") {
                    typeParam(type("Req"), type("Request", LanguageType.Wildcard))
                    typeParam(type("Res"), type("Response", LanguageType.Wildcard))
                    field("pathTemplate", LanguageType.String)
                    field("method", LanguageType.String)
                    function("client") {
                        returnType(type("ClientEdge", type("Req"), type("Res")))
                        arg("serialization", type("Serialization"))
                    }
                },
            )
            add(
                `interface`("Server") {
                    typeParam(type("Req"), type("Request", LanguageType.Wildcard))
                    typeParam(type("Res"), type("Response", LanguageType.Wildcard))
                    field("pathTemplate", LanguageType.String)
                    field("method", LanguageType.String)
                    function("server") {
                        returnType(type("ServerEdge", type("Req"), type("Res")))
                        arg("serialization", type("Serialization"))
                    }
                },
            )
        }

        override val source = AstShared(packageString)
            .convert()
            .transform {
                matchingElements { file: LanguageFile ->
                    val (packageElements, rest) = file.elements.partition { it is LanguagePackage }
                    file.copy(elements = packageElements + Import("kotlin.reflect", LanguageType.Custom("KType")) + rest)
                }
                injectAfter { namespace: Namespace ->
                    if (namespace.name == Name.of("Wirespec")) clientServer
                    else emptyList()
                }
            }
            .generateKotlin()
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> =
        super.emit(module, logger).let {
            if (emitShared.value) it + File(
                Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.kotlin").toDir() + "Wirespec"),
                listOf(RawElement(shared.source))
            )
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): File =
        super.emit(definition, module, logger).let { file ->
            val subPackageName = packageName + definition
            File(
                name = Name.of(subPackageName.toDir() + file.name.pascalCase()),
                elements = listOf(LanguagePackage(subPackageName.value)) +
                    (if (module.needImports()) listOf(RawElement(import)) else emptyList()) +
                    file.elements
            )
        }

    fun Identifier.sanitize() = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .sanitizeFirstIsDigit()
        .let { if (this is FieldIdentifier) it.sanitizeKeywords() else it }

    fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    fun String.sanitizeKeywords() = if (this in reservedKeywords) addBackticks() else this

    fun String.sanitizeSymbol() = this
        .split(".", " ", "-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .sanitizeFirstIsDigit()

    private fun Name.sanitizeCamelCase(): Name {
        val sanitized = if (parts.size > 1) {
            camelCase()
        } else {
            value().sanitizeSymbol()
        }
        return Name(listOf(sanitized.sanitizeKeywords()))
    }

    private fun <T : Element> T.sanitizeNames(): T = transform {
        fields { field ->
            field.copy(name = field.name.sanitizeCamelCase())
        }
        parameters { param ->
            param.copy(name = Name.of(param.name.camelCase().sanitizeSymbol().sanitizeKeywords()))
        }
        statementAndExpression { stmt, tr ->
            when (stmt) {
                is FieldCall -> FieldCall(
                    receiver = stmt.receiver?.let { tr.transformExpression(it) },
                    field = stmt.field.sanitizeCamelCase(),
                )
                is ConstructorStatement -> ConstructorStatement(
                    type = tr.transformType(stmt.type),
                    namedArguments = stmt.namedArguments.map { (name, expr) ->
                        name.sanitizeCamelCase() to tr.transformExpression(expr)
                    }.toMap(),
                )
                else -> stmt.transformChildren(tr)
            }
        }
    }

    override fun emit(type: Type, module: Module): File =
        type.convertWithValidation(module)
            .sanitizeNames()
            .transform {
                matchingElements { struct: Struct ->
                    if (struct.fields.isEmpty()) struct.copy(constructors = listOf(Constructor(emptyList(), emptyList())))
                    else struct
                }
            }

    private fun Definition.emitEndpointImports() = importReferences()
        .distinctBy { it.value }
        .map { "import ${packageName.value}.model.${it.value}" }.joinToString("\n") { it.trimStart() }

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.emitEndpointImports()
        val file = endpoint.convert().sanitizeNames()
        val endpointNamespace = file.findElement<Namespace>()!!
        val body = endpointNamespace
            .injectCompanionObject(endpoint)

        return if (imports.isNotEmpty()) LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(RawElement(imports), body))
        else LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
    }

    private fun Namespace.injectCompanionObject(endpoint: Endpoint): Namespace {
        val companion = companionObject(endpoint)

        return transform {
            injectAfter { iface: Interface ->
                if (iface.name == Name.of("Handler")) listOf(companion) else emptyList()
            }
        }
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
        val file = channel.convert().sanitizeNames()
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements)
        else file
    }

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .sanitizeNames()
        .transform {
            matchingElements { languageEnum: LanguageEnum ->
                languageEnum.withLabelField(
                    sanitizeEntry = { it.sanitizeEnum() },
                    labelFieldOverride = true,
                    labelExpression = RawExpression("label"),
                )
            }
        }

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

    override fun emit(union: Union): File = union
        .convert()
        .sanitizeNames()

    override fun emit(refined: Refined): File {
        val file = refined.convert().sanitizeNames()
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
                    returns(refined.reference.convertConstraint(VariableReference(Name.of("value"))))
                },
            ),
        )
        return LanguageFile(Name.of(refined.identifier.sanitize()), listOf(updatedStruct))
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
