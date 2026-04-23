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
import community.flock.wirespec.ir.converter.convertClientServer
import community.flock.wirespec.ir.converter.convertToGenerator
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.Assignment
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.Precision
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.TypeDescriptor
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.plus
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.struct
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.withLabelField
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.emit.placeInPackage
import community.flock.wirespec.ir.emit.prependImports
import community.flock.wirespec.ir.emit.withSharedSource
import community.flock.wirespec.ir.generator.JavaGenerator
import community.flock.wirespec.ir.generator.generateJava
import community.flock.wirespec.ir.transformer.SanitizationConfig
import community.flock.wirespec.ir.transformer.sanitizeNames
import community.flock.wirespec.ir.transformer.toGetterAccessors
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType
import community.flock.wirespec.ir.core.Enum as LanguageEnum
import community.flock.wirespec.ir.core.Function as LanguageFunction

open class JavaIrEmitter(
    override val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : IrEmitter, HasPackageName {

    override val generator = JavaGenerator

    override val extension = FileExtension.Java

    override fun transformTestFile(file: File): File = file.transformTypeDescriptors()

    private val wirespecImports = listOf(import("$DEFAULT_SHARED_PACKAGE_STRING.java", "Wirespec"))

    private val sanitizationConfig = SanitizationConfig(
        reservedKeywords = reservedKeywords,
        escapeKeyword = { "_$it" },
        fieldNameCase = { name ->
            if (name.parts.size > 1) Name(listOf(name.camelCase())) else name
        },
        parameterNameCase = { name -> Name(listOf(name.camelCase())) },
        sanitizeSymbol = { it.sanitizeSymbol() },
        extraStatementTransforms = { stmt, tr ->
            when {
                stmt is FunctionCall && stmt.name.value() == "validate" ->
                    stmt.copy(typeArguments = emptyList()).transformChildren(tr)
                else -> stmt.transformChildren(tr)
            }
        },
    )

    override val shared = object : Shared {
        override val packageString: String = "$DEFAULT_SHARED_PACKAGE_STRING.java"

        private val wirespecShared = AstShared(packageString).convert()

        private val imports = listOf(
            import("java.lang.reflect", "Type"),
            import("java.lang.reflect", "ParameterizedType"),
            import("java.util", "List"),
            import("java.util", "Map"),
        )

        private val clientServer = AstShared(packageString).convertClientServer().map {
            it.toGetterAccessors { name ->
                when (name.value()) {
                    "client" -> Name.of("getClient")
                    "server" -> Name.of("getServer")
                    else -> null
                }
            }
        } + listOf(
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
        super.emit(module, logger).withSharedSource(emitShared) {
            File(
                Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.java").toDir() + "Wirespec"),
                listOf(RawElement(shared.source))
            )
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val file = super.emit(definition, module, logger)
        return file.copy(name = Name.of(file.name.pascalCase().sanitizeSymbol()))
            .prependImports(wirespecImports.takeIf { module.needImports() })
            .placeInPackage(packageName = packageName, definition = definition)
    }

    override fun emitGenerator(definition: Definition, module: Module): File? {
        val generatorFile = when (definition) {
            is AstType -> definition.convertToGenerator()
            is Enum -> definition.convertToGenerator()
            is Refined -> definition.convertToGenerator()
            is Union -> definition.convertToGenerator()
            else -> return null
        }
        return generatorFile
            .sanitizeNames(sanitizationConfig)
            .prependImports(wirespecImports)
            .placeInPackage(packageName = packageName, subPackage = "generator")
    }

    override fun emit(type: AstType, module: Module): File =
        type.convertWithValidation(module)
            .sanitizeNames(sanitizationConfig)

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .transform {
            matchingElements { languageEnum: LanguageEnum ->
                languageEnum
                    .withLabelField(sanitizeEntry = { it.sanitizeEnum() })
                    .plus(
                        function("label") {
                            returnType(Type.String)
                            returns(VariableReference(Name.of("label")))
                        }
                    )
            }
        }
        .sanitizeNames(sanitizationConfig)

    override fun emit(union: Union): File = union
        .convert()
        .sanitizeNames(sanitizationConfig)

    override fun emit(refined: Refined): File = refined.convert()
        .transform {
            matchingElements { struct: Struct ->
                struct
                    .copy(
                        interfaces = listOf(Type.Custom("Wirespec.Refined")),
                        elements = struct.elements.map { element ->
                            if (element is LanguageFunction) {
                                element.copy(isOverride = true)
                            } else element
                        }
                    )
                    .plus(
                        function("value", isOverride = true) {
                            returnType(refined.reference.convert())
                            returns(VariableReference(Name.of("value")))
                        }
                    )
            }
        }
        .sanitizeNames(sanitizationConfig)

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        return endpoint.convert()
            .injectHandleFunction(endpoint)
            .transformTypeDescriptors()
            .sanitizeNames(sanitizationConfig)
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

    override fun emit(channel: Channel): File {
        val fullyQualifiedPrefix = if (channel.identifier.value == channel.reference.value) {
            "${packageName.value}.model."
        } else {
            ""
        }
        return channel.convert()
            .sanitizeNames(sanitizationConfig)
            .transform {
                matchingElements { it: Interface -> it.withFullyQualifiedPrefix(fullyQualifiedPrefix) }
                matchingElements { file: File ->
                    val interfaceElement = file.findElement<Interface>()!!
                    file.copy(elements = listOf(RawElement("@FunctionalInterface\n"), interfaceElement))
                }
            }
    }

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        val endpointImport = import("${packageName.value}.endpoint", endpoint.identifier.value)
        val file = super.emitEndpointClient(endpoint).sanitizeNames(sanitizationConfig).transformTypeDescriptors()
        val endpointName = endpoint.identifier.value

        val transformedFile = file.transform {
            matchingElements { func: LanguageFunction ->
                if (func.isAsync && func.body.size >= 2) {
                    val transportAssign = func.body[func.body.size - 2]
                    val returnStmt = func.body.last()
                    if (transportAssign is Assignment && returnStmt is ReturnStatement) {
                        val bodyPrefix = func.body.dropLast(2)
                        func.copy(
                            body = bodyPrefix + ReturnStatement(
                                FunctionCall(
                                    name = Name.of("thenApply"),
                                    receiver = transportAssign.value,
                                    arguments = mapOf(
                                        Name.of("mapper") to RawExpression(
                                            "rawResponse -> $endpointName.fromRawResponse(serialization(), rawResponse)"
                                        )
                                    )
                                )
                            )
                        )
                    } else func
                } else func
            }
        }

        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + transformedFile.name.pascalCase().sanitizeSymbol()),
            elements = listOf(Package(subPackageName.value)) +
                wirespecImports +
                imports +
                listOf(endpointImport) +
                transformedFile.elements
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        val imports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .filter { imp -> endpoints.none { it.identifier.value == imp.value } }
            .map { import("${packageName.value}.model", it.value) }
        val endpointImports = endpoints.map { import("${packageName.value}.endpoint", it.identifier.value) }
        val clientImports = endpoints.map { import("${packageName.value}.client", "${it.identifier.value}Client") }
        val allImports = imports + endpointImports + clientImports
        val file = super.emitClient(endpoints, logger).sanitizeNames(sanitizationConfig)
        return File(
            name = Name.of(packageName.toDir() + file.name.pascalCase().sanitizeSymbol()),
            elements = listOf(Package(packageName.value)) +
                wirespecImports +
                allImports +
                file.elements
        )
    }

    private fun String.sanitizeSymbol(): String = this
        .split(".", " ", "-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .sanitizeFirstIsDigit()

    private fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    private fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    private fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

    private fun Definition.buildImports() = importReferences()
        .filter { identifier.value != it.value }
        .map { import("${packageName.value}.model", it.value) }

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
                type("Wirespec.Server", type("Request"), type("Response", Type.Wildcard))
            )
            implements(
                type("Wirespec.Client", type("Request"), type("Response", Type.Wildcard))
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
                    type("Wirespec.ServerEdge", type("Request"), type("Response", Type.Wildcard))
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
                    type("Wirespec.ClientEdge", type("Request"), type("Response", Type.Wildcard))
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

    private fun <T : Element> T.transformTypeDescriptors(): T = transform {
        statementAndExpression { stmt, tr ->
            when (stmt) {
                is TypeDescriptor -> {
                    val rootType = stmt.type.findRoot()
                    val containerStr = stmt.type.rawContainerClass()
                    val rootStr = "${rootType.toJavaName()}.class"
                    val containerArg = containerStr?.let { "$it.class" } ?: "null"
                    RawExpression("Wirespec.getType($rootStr, $containerArg)")
                }
                else -> stmt.transformChildren(tr)
            }
        }
    }

    private fun Type.findRoot(): Type = when (this) {
        is Type.Nullable -> type.findRoot()
        is Type.Array -> elementType.findRoot()
        is Type.Dict -> valueType.findRoot()
        else -> this
    }

    private fun Type.rawContainerClass(): String? = when (this) {
        is Type.Nullable -> "java.util.Optional"
        is Type.Array -> "java.util.List"
        is Type.Dict -> "java.util.Map"
        else -> null
    }

    private fun Type.toJavaName(): String = when (this) {
        is Type.Integer -> when (precision) {
            Precision.P32 -> "Integer"; Precision.P64 -> "Long"
        }

        is Type.Number -> when (precision) {
            Precision.P32 -> "Float"; Precision.P64 -> "Double"
        }

        Type.String -> "String"
        Type.Boolean -> "Boolean"
        Type.Bytes -> "byte[]"
        Type.Any -> "Object"
        Type.Unit -> "Void"
        is Type.Custom -> name
        else -> "Object"
    }

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
