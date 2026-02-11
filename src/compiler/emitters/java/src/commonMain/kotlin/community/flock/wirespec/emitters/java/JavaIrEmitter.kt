package community.flock.wirespec.emitters.java

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.IrEmitter
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
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.Constructor
import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.Field
import community.flock.wirespec.language.core.File
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Package
import community.flock.wirespec.language.core.RawElement
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.Static
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.file
import community.flock.wirespec.language.core.findAll
import community.flock.wirespec.language.core.findElement
import community.flock.wirespec.language.core.function
import community.flock.wirespec.language.core.import
import community.flock.wirespec.language.core.injectAfter
import community.flock.wirespec.language.core.`interface`
import community.flock.wirespec.language.core.raw
import community.flock.wirespec.language.core.struct
import community.flock.wirespec.language.core.transformFieldsWhere
import community.flock.wirespec.language.core.transformMatchingElements
import community.flock.wirespec.language.core.transformParametersWhere
import community.flock.wirespec.language.generator.generateJava
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType
import community.flock.wirespec.language.core.Enum as LanguageEnum
import community.flock.wirespec.language.core.Function as LanguageFunction

open class JavaIrEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : IrEmitter, HasPackageName {

    val import =
        """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.java.Wirespec;
        |
        """.trimMargin()

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
            .transformMatchingElements { file: File ->
                val (packageElements, rest) = file.elements.partition { it is Package }
                file.copy(elements = packageElements + imports + rest)
            }
            .injectAfter { static: Static ->
                if (static.name == "Wirespec") clientServer else emptyList()
            }

        override val source: String = wirespecFile.generateJava()
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> =
        super.emit(module, logger).let {
            if (emitShared.value) it + Emitted(
                PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.java").toDir() + "Wirespec",
                shared.source
            )
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted =
        super.emit(definition, module, logger).let {
            val subPackageName = packageName + definition
            Emitted(
                file = subPackageName.toDir() + it.file.sanitizeSymbol(),
                result =
                    """
                    |package $subPackageName;
                    |${if (module.needImports()) import else ""}
                    |${it.result}
                    """.trimMargin().trimStart()
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

    private fun <T : Element> T.sanitizeNames(): T = this
        .transformFieldsWhere({ true }) { field ->
            field.copy(name = field.name.sanitizeSymbol().sanitizeKeywords())
        }
        .transformParametersWhere({ true }) { param ->
            param.copy(name = param.name.sanitizeSymbol().sanitizeKeywords())
        }

    override fun emit(type: AstType, module: Module): Emitted = type.convert().sanitizeNames().toEmitted()

    override fun emit(enum: Enum, module: Module): Emitted = enum
        .convert()
        .transformMatchingElements { languageEnum: LanguageEnum ->
            languageEnum.copy(
                entries = languageEnum.entries.map {
                    LanguageEnum.Entry(it.name.sanitizeEnum(), listOf("\"${it.name}\""))
                },
                fields = listOf(
                    Field("label", Type.String),
                ),
                constructors = listOf(
                    Constructor(
                        parameters = listOf(community.flock.wirespec.language.core.Parameter("label", Type.String)),
                        body = listOf(Assignment("this.label", RawExpression("label"), true)),
                    ),
                ),
                elements = listOf(
                    function("toString", isOverride = true) {
                        returnType(Type.String)
                        returns(RawExpression("label"))
                    },
                    function("label") {
                        returnType(Type.String)
                        returns(RawExpression("label"))
                    },
                ),
            )
        }
        .sanitizeNames()
        .toEmitted()

    override fun emit(union: Union): Emitted = union
        .convert()
        .sanitizeNames()
        .toEmitted()

    override fun emit(refined: Refined): Emitted = refined
        .convert()
        .transformMatchingElements { struct: Struct ->
            struct.copy(
                interfaces = listOf(Type.Custom("Wirespec.Refined")),
                elements = listOf(
                    function("toString", isOverride = true) {
                        returnType(string)
                        returns(RawExpression("value.toString()"))
                    },
                    function("validate", isStatic = true) {
                        returnType(Type.Boolean)
                        arg("record", type(struct.name))
                        returns(RawExpression(refined.reference.emitConstraint() ?: "true"))
                    },
                    function("value", isOverride = true) {
                        returnType(refined.reference.convert())
                        returns(RawExpression("value"))
                    }
                )
            )
        }
        .sanitizeNames()
        .toEmitted()

    fun Reference.emitConstraint() = when (this) {
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.String -> t.constraint?.emit()
            is Reference.Primitive.Type.Integer -> t.constraint?.emit()
            is Reference.Primitive.Type.Number -> t.constraint?.emit()
            else -> null
        }

        else -> TODO("Not yet implemented")
    }


    override fun emit(channel: Channel): Emitted {
        val fullyQualifiedPrefix = if (channel.identifier.value == channel.reference.value) {
            "${packageName.value}.model."
        } else {
            ""
        }
        return channel.convert()
            .sanitizeNames()
            .transformMatchingElements { it: Interface -> it.withFullyQualifiedPrefix(fullyQualifiedPrefix) }
            .transformMatchingElements { file: File ->
                val interfaceElement = file.findElement<Interface>()!!
                file.copy(elements = listOf(RawElement("@FunctionalInterface\n"), interfaceElement))
            }
            .toEmitted()
    }

    private fun Interface.withFullyQualifiedPrefix(prefix: String): Interface =
        if (prefix.isNotEmpty()) {
            transformParametersWhere(
                predicate = { it.name == "message" },
                transform = { param ->
                    when (val t = param.type) {
                        is Type.Custom -> param.copy(type = t.copy(name = prefix + t.name))
                        else -> param
                    }
                },
            )
        } else {
            this
        }


    override fun emit(endpoint: Endpoint): Emitted {
        val imports = endpoint.emitImports()
        return endpoint.convert()
            .sanitizeNames()
            .injectHandleFunction(endpoint)
            .let { file ->
                if (imports.isNotEmpty()) {
                    file.transformMatchingElements { f: File ->
                        f.copy(elements = listOf(RawElement("$imports\n\n")) + f.elements)
                    }
                } else {
                    file
                }
            }
            .toEmitted()
    }

    open fun emitHandleFunction(endpoint: Endpoint): String {
        return endpoint.convert()
            .sanitizeNames()
            .findAll<Interface>().firstOrNull { it.name == "Handler" }
            ?.findElement<LanguageFunction>()
            ?.generateJava()
            ?.replace("static ", "")
            ?.let { it + "\n" }
            ?: ""
    }

    private fun <T : Element> T.injectHandleFunction(endpoint: Endpoint): T {
        val handleFunction = emitHandleFunction(endpoint)
        val targetFunctionName = endpoint.identifier.value.replaceFirstChar { it.lowercase() }
        val handlersStruct = buildHandlers(endpoint)

        return transformMatchingElements { iface: Interface ->
            if (iface.name == "Handler") {
                val transformed = iface.transformMatchingElements { fn: LanguageFunction ->
                    if (handleFunction.isNotBlank() && fn.name == targetFunctionName) RawElement(handleFunction) else fn
                }
                transformed.injectAfter { iface2: Interface -> listOf(handlersStruct) }
            } else {
                iface
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

    private fun Definition.emitImports() = importReferences()
        .filter { identifier.value != it.value }
        .joinToString("\n") { "import ${packageName.value}.model.${it.value};" }

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

    fun Reference.Primitive.Type.Constraint.emit() = when (this) {
        is Reference.Primitive.Type.Constraint.RegExp -> """java.util.regex.Pattern.compile("${
            expression.replace(
                "\\",
                "\\\\"
            )
        }").matcher(record.value).find()"""

        is Reference.Primitive.Type.Constraint.Bound -> {
            val minCheck = min?.let { "$it < record.value" }
            val maxCheck = max?.let { "record.value < $it" }
            val checks = listOfNotNull(minCheck, maxCheck).joinToString(" && ")
            """${if (checks.isEmpty()) "true" else checks}"""
        }
    }

    override fun emit(identifier: Identifier) = identifier.value.firstToUpper()

    fun File.toEmitted(): Emitted = Emitted(name, generateJava())

}
