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
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.withLabelField
import community.flock.wirespec.ir.core.`interface`
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.struct
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.generator.JavaGenerator
import community.flock.wirespec.ir.generator.generateJava
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType
import community.flock.wirespec.ir.core.Enum as LanguageEnum
import community.flock.wirespec.ir.core.Function as LanguageFunction

open class JavaIrEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : IrEmitter, HasPackageName {

    override val generator = JavaGenerator

    val wirespecImport = import("$DEFAULT_SHARED_PACKAGE_STRING.java", "Wirespec")

    override val extension = FileExtension.Java

    override val shared = object : Shared {

        override val packageString: String = "$DEFAULT_SHARED_PACKAGE_STRING.java"

        val wirespecShared = AstShared(packageString).convert()

        private val imports = buildList {
            add(import("java.lang.reflect", "Type"))
            add(import("java.lang.reflect", "ParameterizedType"))
            add(import("java.util", "List"))
            add(import("java.util", "Map"))
        }

        private val clientServer = buildList {
            add(
                `interface`("ServerEdge") {
                    typeParam(type("Req"), type("Request", Type.Wildcard))
                    typeParam(type("Res"), type("Response", Type.Wildcard))
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
                    typeParam(type("Req"), type("Request", Type.Wildcard))
                    typeParam(type("Res"), type("Response", Type.Wildcard))
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
                    typeParam(type("Req"), type("Request", Type.Wildcard))
                    typeParam(type("Res"), type("Response", Type.Wildcard))
                    function("getPathTemplate") {
                        returnType(string)
                    }
                    function("getMethod") {
                        returnType(string)
                    }
                    function("getClient") {
                        returnType(type("ClientEdge", type("Req"), type("Res")))
                        arg("serialization", type("Serialization"))
                    }
                },
            )
            add(
                `interface`("Server") {
                    typeParam(type("Req"), type("Request", Type.Wildcard))
                    typeParam(type("Res"), type("Response", Type.Wildcard))
                    function("getPathTemplate") {
                        returnType(string)
                    }
                    function("getMethod") {
                        returnType(string)
                    }
                    function("getServer") {
                        returnType(type("ServerEdge", type("Req"), type("Res")))
                        arg("serialization", type("Serialization"))
                    }
                },
            )
            add(
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
        }

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
        super.emit(module, logger).let {
            if (emitShared.value) it + File(
                Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.java").toDir() + "Wirespec"),
                listOf(RawElement(shared.source))
            )
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): File =
        super.emit(definition, module, logger).let { file ->
            val subPackageName = packageName + definition
            File(
                name = Name.of(subPackageName.toDir() + file.name.pascalCase().sanitizeSymbol()),
                elements = listOf(Package(subPackageName.value)) +
                        (if (module.needImports()) listOf(wirespecImport) else emptyList()) +
                        file.elements
            )
        }

    fun String.sanitizeSymbol() = this
        .split(".", " ", "-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

    fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    private fun <T : Element> T.sanitizeNames(): T = transform {
        fieldsWhere({ true }) { field ->
            field.copy(name = Name(listOf(field.name.value().sanitizeSymbol().sanitizeKeywords())))
        }
        parametersWhere({ true }) { param ->
            param.copy(name = Name.of(param.name.camelCase().sanitizeSymbol().sanitizeKeywords()))
        }
    }

    override fun emit(type: AstType, module: Module): File =
        type.convertWithValidation(module)
            .sanitizeNames()

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .transform {
            matchingElements { languageEnum: LanguageEnum ->
                languageEnum.withLabelField(
                    sanitizeEntry = { it.sanitizeEnum() },
                    extraElements = listOf(
                        function("label") {
                            returnType(Type.String)
                            returns(VariableReference(Name.of("label")))
                        },
                    ),
                )
            }
        }
        .sanitizeNames()

    override fun emit(union: Union): File = union
        .convert()
        .sanitizeNames()

    override fun emit(refined: Refined): File = refined.convert()
        .transform {
            matchingElements { s: Struct ->
                s.copy(
                    interfaces = listOf(Type.Custom("Wirespec.Refined")),
                    elements = listOf(
                        function("toString", isOverride = true) {
                            returnType(string)
                            returns(FunctionCall(receiver = VariableReference(Name.of("value")), name = Name.of("toString")))
                        },
                    ) + s.elements.map { element ->
                        if (element is LanguageFunction && element.name == Name.of("validate")) {
                            element.copy(isOverride = true)
                        } else element
                    } + listOf(
                        function("value", isOverride = true) {
                            returnType(refined.reference.convert())
                            returns(VariableReference(Name.of("value")))
                        },
                    ),
                )
            }
        }
        .sanitizeNames()


    override fun emit(channel: Channel): File {
        val fullyQualifiedPrefix = if (channel.identifier.value == channel.reference.value) {
            "${packageName.value}.model."
        } else {
            ""
        }
        return channel.convert()
            .sanitizeNames()
            .transform {
                matchingElements { it: Interface -> it.withFullyQualifiedPrefix(fullyQualifiedPrefix) }
                matchingElements { file: File ->
                    val interfaceElement = file.findElement<Interface>()!!
                    file.copy(elements = listOf(RawElement("@FunctionalInterface\n"), interfaceElement))
                }
            }
    }

    private fun Interface.withFullyQualifiedPrefix(prefix: String): Interface =
        if (prefix.isNotEmpty()) {
            transform {
                parametersWhere(
                    predicate = { it.name == Name.of("message") },
                    transform = { param ->
                        when (val t = param.type) {
                            is Type.Custom -> param.copy(type = t.copy(name = prefix + t.name))
                            else -> param
                        }
                    },
                )
            }
        } else {
            this
        }


    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.emitImportElements()
        return endpoint.convert()
            .sanitizeNames()
            .injectHandleFunction(endpoint)
            .let { file ->
                if (imports.isNotEmpty()) {
                    file.transform {
                        matchingElements { f: File ->
                            f.copy(elements = imports + f.elements)
                        }
                    }
                } else {
                    file
                }
            }
    }

    private fun <T : Element> T.injectHandleFunction(endpoint: Endpoint): T {
        val handlersStruct = buildHandlers(endpoint)
        return transform {
            matchingElements { iface: Interface ->
                if (iface.name == Name.of("Handler")) {
                    iface.transform { injectAfter { _: Interface -> listOf(handlersStruct) } }
                } else {
                    iface
                }
            }
        }
    }

    private fun buildHandlers(endpoint: Endpoint): Struct {
        val pathTemplate = "/" + endpoint.path.joinToString("/") {
            when (it) {
                is Endpoint.Segment.Literal -> it.value
                is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            }
        }

        return struct(name = "Handlers") {
            implements(
                type(
                    "Wirespec.Server",
                    type("Request"), type("Response", Type.Wildcard)
                )
            )
            implements(
                type(
                    "Wirespec.Client",
                    type("Request"), type("Response", Type.Wildcard)
                )
            )
            function("getPathTemplate", isOverride = true) {
                returnType(Type.String)
                returns(literal(pathTemplate))
            }
            function("getMethod", isOverride = true) {
                returnType(Type.String)
                returns(literal(endpoint.method.name))
            }
            function("getServer", isOverride = true) {
                returnType(
                    type(
                        "Wirespec.ServerEdge",
                        type("Request"), type("Response", Type.Wildcard)
                    )
                )
                arg("serialization", type("Wirespec.Serialization"))
                returns(
                    RawExpression(
                        "new Wirespec.ServerEdge<>() {\n" +
                                "@Override public Request from(Wirespec.RawRequest request) {\n" +
                                "    return fromRawRequest(serialization, request);\n" +
                                "}\n" +
                                "@Override public Wirespec.RawResponse to(Response<?> response) {\n" +
                                "    return toRawResponse(serialization, response);\n" +
                                "}\n" +
                                "}"
                    ),
                )
            }
            function("getClient", isOverride = true) {
                returnType(
                    type(
                        "Wirespec.ClientEdge",
                        type("Request"), type("Response", Type.Wildcard)
                    )
                )
                arg("serialization", type("Wirespec.Serialization"))
                returns(
                    RawExpression(
                        "new Wirespec.ClientEdge<>() {\n" +
                                "@Override public Wirespec.RawRequest to(Request request) {\n" +
                                "    return toRawRequest(serialization, request);\n" +
                                "}\n" +
                                "@Override public Response<?> from(Wirespec.RawResponse response) {\n" +
                                "    return fromRawResponse(serialization, response);\n" +
                                "}\n" +
                                "}"
                    ),
                )
            }
        }
    }

    private fun Definition.emitImportElements() = importReferences()
        .filter { identifier.value != it.value }
        .map { import("${packageName.value}.model", it.value) }

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
