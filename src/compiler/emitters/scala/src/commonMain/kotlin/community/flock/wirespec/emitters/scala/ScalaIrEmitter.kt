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
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertConstraint
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.Constructor
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.TypeParameter
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.flattenNestedStructs
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.`interface`
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.withLabelField
import community.flock.wirespec.ir.emit.IrEmitter
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

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.scala.Wirespec
        |import scala.reflect.ClassTag
        |
    """.trimMargin()

    override val extension = FileExtension.Scala

    override val shared = object : Shared {
        override val packageString = "$DEFAULT_SHARED_PACKAGE_STRING.scala"

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
                    file.copy(elements = packageElements + LanguageImport("scala.reflect", LanguageType.Custom("ClassTag")) + rest)
                }
                matchingElements { ns: Namespace ->
                    if (ns.name == Name.of("Wirespec")) {
                        val newElements = mutableListOf<Element>()
                        for (element in ns.elements) {
                            if (element is Interface && element.name.pascalCase() in setOf("Request", "Response")) {
                                val nestedHeaders = element.elements.filterIsInstance<Interface>()
                                    .firstOrNull { it.name.pascalCase() == "Headers" }
                                if (nestedHeaders != null) {
                                    newElements.add(Namespace(element.name, listOf(nestedHeaders)))
                                    newElements.add(
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
                                    newElements.add(element)
                                }
                            } else {
                                newElements.add(element)
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
        super.emit(module, logger).let {
            if (emitShared.value) it + File(
                Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.scala").toDir() + "Wirespec"),
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
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()
        .let { if (this is FieldIdentifier) it.sanitizeKeywords() else it }

    fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    fun String.sanitizeKeywords() = if (this in reservedKeywords) addBackticks() else this

    override fun emit(type: Type, module: Module): File =
        type.convertWithValidation(module)
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
        val file = endpoint.convert()
        val endpointNamespace = file.findElement<Namespace>()!!
        val flattened = endpointNamespace.flattenNestedStructs()
        val requestIsObject = isRequestObject(flattened)
        val body = flattened
            .injectHandleFunction()
            .withClientServerObjects(endpoint, requestIsObject)

        return if (imports.isNotEmpty()) LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(RawElement(imports), body))
        else LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
    }

    private fun isRequestObject(namespace: Namespace): Boolean {
        val requestStruct = namespace.elements.filterIsInstance<Struct>()
            .firstOrNull { it.name.pascalCase() == "Request" } ?: return false
        return (requestStruct.constructors.size == 1 && requestStruct.constructors.single().parameters.isEmpty()) ||
            (requestStruct.fields.isEmpty() && requestStruct.constructors.isEmpty())
    }

    private fun Namespace.injectHandleFunction(): Namespace {
        return transform {
            matchingElements { iface: Interface ->
                if (iface.name == Name.of("Handler")) {
                    iface.copy(
                        typeParameters = listOf(TypeParameter(LanguageType.Custom("F[_]"))),
                    ).transform {
                        matchingElements { fn: LanguageFunction ->
                            fn.copy(
                                isAsync = false,
                                returnType = fn.returnType?.let { LanguageType.Custom("F", generics = listOf(it)) },
                            )
                        }
                    }
                } else iface
            }
        }
    }

    private fun Namespace.withClientServerObjects(endpoint: Endpoint, requestIsObject: Boolean): Namespace {
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
        return copy(elements = elements + clientObject + serverObject)
    }

    private fun Definition.emitChannelImports() = importReferences()
        .distinctBy { it.value }
        .map { "import ${packageName.value}.model.${it.value}" }.joinToString("\n") { it.trimStart() }

    override fun emit(channel: Channel): File {
        val imports = channel.emitChannelImports()
        val file = channel.convert()
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements)
        else file
    }

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
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

    override fun emit(refined: Refined): File {
        val file = refined.convert()
        val struct = file.findElement<Struct>()!!
        val toStringExpr = when (refined.reference.type) {
            is Reference.Primitive.Type.String -> "value"
            else -> "value.toString"
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
