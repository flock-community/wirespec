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
import community.flock.wirespec.ir.core.FunctionCall
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

    override val extension = FileExtension.Kotlin

    private val wirespecImport = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.kotlin.Wirespec
        |import kotlin.reflect.typeOf
        |
    """.trimMargin()

    override val shared = object : Shared {
        override val packageString = "$DEFAULT_SHARED_PACKAGE_STRING.kotlin"

        private val clientServer = listOf(
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

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val files = super.emit(module, logger)
        return if (emitShared.value) {
            files + File(
                Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.kotlin").toDir() + "Wirespec"),
                listOf(RawElement(shared.source))
            )
        } else {
            files
        }
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val file = super.emit(definition, module, logger)
        val subPackageName = packageName + definition
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase()),
            elements = buildList {
                add(LanguagePackage(subPackageName.value))
                if (module.needImports()) add(RawElement(wirespecImport))
                addAll(file.elements)
            }
        )
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

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        val file = endpoint.convert().sanitizeNames()
        val endpointNamespace = file.findElement<Namespace>()!!
        val body = endpointNamespace.injectCompanionObject(endpoint)
        return if (imports.isNotEmpty()) {
            LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(RawElement(imports), body))
        } else {
            LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
        }
    }

    override fun emit(channel: Channel): File {
        val imports = channel.buildImports()
        val file = channel.convert().sanitizeNames()
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements)
        else file
    }

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        val endpointImport = "import ${packageName.value}.endpoint.${endpoint.identifier.value}"
        val allImports = listOf(imports, endpointImport).filter { it.isNotEmpty() }.joinToString("\n")
        val file = super.emitEndpointClient(endpoint).sanitizeNames()
        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase()),
            elements = buildList {
                add(LanguagePackage(subPackageName.value))
                add(RawElement(wirespecImport))
                if (allImports.isNotEmpty()) add(RawElement(allImports))
                addAll(file.elements)
            }
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        val imports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .joinToString("\n") { "import ${packageName.value}.model.${it.value}" }
        val endpointImports = endpoints
            .joinToString("\n") { "import ${packageName.value}.endpoint.${it.identifier.value}" }
        val clientImports = endpoints
            .joinToString("\n") { "import ${packageName.value}.client.${it.identifier.value}Client" }
        val allImports = listOf(imports, endpointImports, clientImports).filter { it.isNotEmpty() }.joinToString("\n")
        val file = super.emitClient(endpoints, logger).sanitizeNames()
        return File(
            name = Name.of(packageName.toDir() + file.name.pascalCase()),
            elements = buildList {
                add(LanguagePackage(packageName.value))
                add(RawElement(wirespecImport))
                if (allImports.isNotEmpty()) add(RawElement(allImports))
                addAll(file.elements)
            }
        )
    }

    private fun <T : Element> T.sanitizeNames(): T = transform {
        fields { field ->
            field.copy(name = field.name.sanitizeName())
        }
        parameters { param ->
            param.copy(name = Name.of(param.name.camelCase().sanitizeSymbol().sanitizeKeywords()))
        }
        statementAndExpression { stmt, tr ->
            when (stmt) {
                is FieldCall -> FieldCall(
                    receiver = stmt.receiver?.let { tr.transformExpression(it) },
                    field = stmt.field.sanitizeName(),
                )
                is FunctionCall -> if (stmt.name.value() == "validate") {
                    stmt.copy(typeArguments = emptyList()).transformChildren(tr)
                } else stmt.transformChildren(tr)
                is ConstructorStatement -> ConstructorStatement(
                    type = tr.transformType(stmt.type),
                    namedArguments = stmt.namedArguments.map { (name, expr) ->
                        name.sanitizeName() to tr.transformExpression(expr)
                    }.toMap(),
                )
                else -> stmt.transformChildren(tr)
            }
        }
    }

    private fun Name.sanitizeName(): Name {
        val sanitized = if (parts.size > 1) camelCase() else value().sanitizeSymbol()
        return Name(listOf(sanitized.sanitizeKeywords()))
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

    private fun Definition.buildImports() = importReferences()
        .distinctBy { it.value }
        .joinToString("\n") { "import ${packageName.value}.model.${it.value}" }

    private fun Namespace.injectCompanionObject(endpoint: Endpoint): Namespace =
        transform {
            injectAfter { iface: Interface ->
                if (iface.name == Name.of("Handler")) listOf(companionObject(endpoint)) else emptyList()
            }
        }

    private fun companionObject(endpoint: Endpoint): RawElement {
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
