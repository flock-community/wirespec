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
import community.flock.wirespec.ir.core.Constructor
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
import community.flock.wirespec.ir.core.withLabelField
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.transformer.SanitizationConfig
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
                    if (ns.name == Name.of("Wirespec")) {
                        val newElements = ns.elements.flatMap { element ->
                            if (element is Interface && element.name.pascalCase() in setOf("Request", "Response")) {
                                val nestedHeaders = element.elements.filterIsInstance<Interface>()
                                    .firstOrNull { it.name.pascalCase() == "Headers" }
                                if (nestedHeaders != null) {
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
                                } else {
                                    listOf(element)
                                }
                            } else {
                                listOf(element)
                            }
                        }
                        ns.copy(elements = newElements)
                    } else ns
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
                languageEnum.withLabelField(
                    sanitizeEntry = { it.sanitizeEnum() },
                    labelFieldOverride = true,
                    labelExpression = RawExpression("label"),
                )
            }
        }

    override fun emit(union: Union): File = union
        .convert()
        .sanitizeNames(sanitizationConfig)

    override fun emit(refined: Refined): File {
        val file = refined.convert().sanitizeNames(sanitizationConfig)
        val struct = file.findElement<Struct>()!!
        val updatedStruct = struct.copy(
            fields = struct.fields.map { f -> f.copy(isOverride = true) },
            elements = struct.elements.map { element ->
                if (element is LanguageFunction) {
                    element.copy(isOverride = true)
                } else element
            },
        ).transform {
            expression { expr, tr ->
                when {
                    expr is FunctionCall && expr.name.camelCase() == "toString" && expr.receiver != null ->
                        FieldCall(receiver = expr.receiver?.let { tr.transformExpression(it) }, field = Name.of("toString"))
                    else -> expr.transformChildren(tr)
                }
            }
        }
        return LanguageFile(Name.of(refined.identifier.sanitize()), listOf(updatedStruct))
    }

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        val file = endpoint.convert()
        val endpointNamespace = file.findElement<Namespace>()!!
        val flattened = endpointNamespace.flattenNestedStructs()
        val requestIsObject = isRequestObject(flattened)
        val body = flattened
            .injectHandleFunction()
            .let { ns -> buildClientServerObjects(endpoint, requestIsObject, ns) }
        val sanitized = LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
            .sanitizeNames(sanitizationConfig)
        return if (imports.isNotEmpty()) sanitized.copy(elements = imports + sanitized.elements)
        else sanitized
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

    private fun Definition.buildImports(): List<LanguageImport> = importReferences()
        .distinctBy { it.value }
        .map { import("${packageName.value}.model", it.value) }

    private fun <T : Element> T.addIdentityTypeToCall(): T = transform {
        matchingElements { struct: Struct ->
            struct.copy(
                interfaces = struct.interfaces.map { type ->
                    if (type is LanguageType.Custom && type.name.endsWith(".Call")) {
                        type.copy(generics = listOf(LanguageType.Custom("[A] =>> A")))
                    } else type
                }
            )
        }
    }

    private fun isRequestObject(namespace: Namespace): Boolean {
        val requestStruct = namespace.elements.filterIsInstance<Struct>()
            .firstOrNull { it.name.pascalCase() == "Request" } ?: return false
        return (requestStruct.constructors.size == 1 && requestStruct.constructors.single().parameters.isEmpty()) ||
            (requestStruct.fields.isEmpty() && requestStruct.constructors.isEmpty())
    }

    private fun Namespace.injectHandleFunction(): Namespace = transform {
        matchingElements { iface: Interface ->
            if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) {
                iface.copy(
                    typeParameters = listOf(TypeParameter(LanguageType.Custom("F[_]"))),
                    elements = iface.elements.map { element ->
                        if (element is LanguageFunction) {
                            element.copy(
                                isAsync = false,
                                returnType = element.returnType?.let { LanguageType.Custom("F", generics = listOf(it)) },
                            )
                        } else element
                    },
                )
            } else iface
        }
    }

    private fun buildClientServerObjects(endpoint: Endpoint, requestIsObject: Boolean, namespace: Namespace): Namespace {
        val reqType = if (requestIsObject) "Request.type" else "Request"
        val pathTemplate = "/" + endpoint.path.joinToString("/") {
            when (it) {
                is Endpoint.Segment.Literal -> it.value
                is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            }
        }
        val clientObject = raw(
            """
            |object Client extends Wirespec.Client[$reqType, Response[?]] {
            |  override val pathTemplate: String = "$pathTemplate"
            |  override val method: String = "${endpoint.method}"
            |  override def client(serialization: Wirespec.Serialization): Wirespec.ClientEdge[$reqType, Response[?]] = new Wirespec.ClientEdge[$reqType, Response[?]] {
            |    override def to(request: $reqType): Wirespec.RawRequest = toRawRequest(serialization, request)
            |    override def from(response: Wirespec.RawResponse): Response[?] = fromRawResponse(serialization, response)
            |  }
            |}
            """.trimMargin()
        )
        val serverObject = raw(
            """
            |object Server extends Wirespec.Server[$reqType, Response[?]] {
            |  override val pathTemplate: String = "$pathTemplate"
            |  override val method: String = "${endpoint.method}"
            |  override def server(serialization: Wirespec.Serialization): Wirespec.ServerEdge[$reqType, Response[?]] = new Wirespec.ServerEdge[$reqType, Response[?]] {
            |    override def from(request: Wirespec.RawRequest): $reqType = fromRawRequest(serialization, request)
            |    override def to(response: Response[?]): Wirespec.RawResponse = toRawResponse(serialization, response)
            |  }
            |}
            """.trimMargin()
        )
        return namespace.copy(elements = namespace.elements + clientObject + serverObject)
    }

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
