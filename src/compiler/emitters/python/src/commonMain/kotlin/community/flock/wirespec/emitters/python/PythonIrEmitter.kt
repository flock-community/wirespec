package community.flock.wirespec.emitters.python

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.transformer.SanitizationConfig
import community.flock.wirespec.ir.transformer.sanitizeFieldName
import community.flock.wirespec.ir.transformer.sanitizeNames
import community.flock.wirespec.ir.emit.placeInModule
import community.flock.wirespec.ir.emit.prependImports
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
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
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertConstraint
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.Field
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.flattenNestedStructs
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.generator.PythonGenerator
import community.flock.wirespec.ir.generator.generatePython
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.ir.core.Enum as LanguageEnum
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.File as LanguageFile
import community.flock.wirespec.ir.core.Type as LanguageType
import community.flock.wirespec.ir.core.Union as LanguageUnion

open class PythonIrEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared()
) : IrEmitter {

    override val generator = PythonGenerator

    override val extension = FileExtension.Python

    private val sanitizationConfig: SanitizationConfig by lazy {
        SanitizationConfig(
            reservedKeywords = reservedKeywords,
            escapeKeyword = { "_$it" },
            fieldNameCase = { name ->
                val sanitized = if (name.parts.size > 1) name.camelCase() else name.value()
                Name(listOf(sanitized))
            },
            parameterNameCase = { name -> Name(listOf(name.camelCase())) },
            sanitizeSymbol = { it },
            extraStatementTransforms = { stmt, tr ->
                when (stmt) {
                    is ConstructorStatement -> ConstructorStatement(
                        type = tr.transformType(stmt.type),
                        namedArguments = stmt.namedArguments
                            .map { (k, v) -> sanitizationConfig.sanitizeFieldName(k) to tr.transformExpression(v) }
                            .toMap(),
                    )
                    else -> stmt.transformChildren(tr)
                }
            },
        )
    }

    private val sharedSource = """
        |from __future__ import annotations
        |
        |import enum
        |from abc import ABC, abstractmethod
        |from dataclasses import dataclass
        |from typing import Any, Generic, Optional, Type, TypeVar
        |
        |T = TypeVar('T')
        |
        |
        |def _raise(msg: str) -> Any:
        |    raise Exception(msg)
        |
        |
    """.trimMargin()

    private val pathSegmentStructs: List<Element> = listOf(
        Struct(
            name = Name.of("Literal"),
            fields = listOf(Field(Name.of("value"), LanguageType.String)),
        ),
        Struct(
            name = Name.of("Param"),
            fields = listOf(
                Field(Name.of("name"), LanguageType.String),
                Field(Name.of("type"), LanguageType.Custom("Type", listOf(LanguageType.Any))),
            ),
        ),
    )

    override val shared = object : Shared {
        override val packageString = "shared"
        override val source = sharedSource + AstShared(packageString).convert()
            .transform {
                injectAfter { namespace: Namespace ->
                    if (namespace.name == Name.of("Wirespec")) pathSegmentStructs
                    else emptyList()
                }
            }
            .generatePython()
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val statements = module.statements.sortedBy(::sort).toNonEmptyListOrNull()!!
        return super.emit(module.copy(statements = statements), logger).let {
            fun emitInitImport(def: Definition) = import(".${def.identifier.sanitize()}", def.identifier.sanitize())
            val hasEndpoints = module.statements.any { it is Endpoint }
            val initElements: List<Element> = listOf(
                import(".", "model"),
                import(".", "endpoint"),
            ) + (if (hasEndpoints) listOf(import(".", "client")) else emptyList()) +
                listOf(import(".", "wirespec"))
            val init = File(
                Name.of(packageName.toDir() + "__init__"),
                initElements
            )
            val initEndpoint = File(
                Name.of(packageName.toDir() + "endpoint/" + "__init__"),
                module.statements.filter { it is Endpoint }.map { stmt -> emitInitImport(stmt) }
            )
            val initModel = File(
                Name.of(packageName.toDir() + "model/" + "__init__"),
                module.statements.filter { it is Model }.map { stmt -> emitInitImport(stmt) }
            )
            val initClient = if (hasEndpoints) listOf(File(
                Name.of(packageName.toDir() + "client/" + "__init__"),
                emptyList()
            )) else emptyList()
            val shared = File(Name.of(packageName.toDir() + "wirespec"), listOf(RawElement(shared.source)))
            val parentInits = packageName.value.split(".")
                .dropLast(1)
                .runningFold("") { acc, segment -> if (acc.isEmpty()) segment else "$acc/$segment" }
                .drop(1)
                .map { File(Name.of("$it/__init__"), emptyList()) }
            if (emitShared.value)
                it + init + initEndpoint + initModel + initClient + shared + parentInits
            else
                it + init + parentInits
        }
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val file = super.emit(definition, module, logger)
        return file
            .prependImports(buildImports("..wirespec"))
            .placeInModule(packageName = packageName, definition = definition)
    }

    override fun emit(type: Type, module: Module): File {
        val typeImports = type.importReferences().distinctBy { it.value }
            .map { import(".${it.value}", it.value) }
        val fieldNames = type.shape.value.map { it.identifier.value }.toSet()
        val file = type.convertWithValidation(module)
            .transform {
                matchingElements { fn: LanguageFunction ->
                    if (fn.name == Name.of("validate")) {
                        fn.copy(
                            parameters = listOf(Parameter(Name.of("self"), LanguageType.Custom(""))),
                        ).transform {
                            statementAndExpression { s, t ->
                                if (s is FieldCall && s.receiver == null && s.field.camelCase() in fieldNames) {
                                    FieldCall(receiver = VariableReference(Name.of("self")), field = s.field)
                                } else {
                                    s.transformChildren(t)
                                }
                            }
                        }
                    } else fn
                }
            }
            .sanitizeNames(sanitizationConfig)
        return if (typeImports.isNotEmpty()) file.copy(elements = typeImports + file.elements)
        else file
    }

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .transform {
            matchingElements { languageEnum: LanguageEnum ->
                languageEnum.copy(
                    entries = languageEnum.entries.map {
                        it.copy(name = Name.of(it.name.value().sanitizeEnum().sanitizeKeywords()))
                    },
                )
            }
        }
        .sanitizeNames(sanitizationConfig)

    override fun emit(union: Union): File =
        union.convert()
            .sanitizeNames(sanitizationConfig)

    override fun emit(refined: Refined): File {
        val file = refined.convert()
        val struct = file.findElement<Struct>()!!
        val updatedStruct = struct.copy(
            elements = struct.elements.mapNotNull { element ->
                when {
                    element is LanguageFunction && element.name == Name.of("validate") -> {
                        val constraintExpr = refined.reference.convertConstraint(
                            FieldCall(VariableReference(Name.of("self")), Name.of("value"))
                        )
                        function("validate") {
                            arg("self", LanguageType.Custom(""))
                            returnType(LanguageType.Boolean)
                            returns(constraintExpr)
                        }
                    }
                    element is LanguageFunction && element.name == Name.of("toString") -> {
                        val toStringExpr = when (refined.reference.type) {
                            is Reference.Primitive.Type.String -> "self.value"
                            else -> "str(self.value)"
                        }
                        function("__str__") {
                            arg("self", LanguageType.Custom(""))
                            returnType(LanguageType.String)
                            returns(RawExpression(toStringExpr))
                        }
                    }
                    else -> element
                }
            },
        )
        return file
            .transform {
                matchingElements { _: Struct -> updatedStruct }
            }
            .sanitizeNames(sanitizationConfig)
    }

    override fun emit(endpoint: Endpoint): File {
        val endpointImports = endpoint.importReferences().distinctBy { it.value }
            .map { import("..model.${it.value}", it.value) }
        val converted = endpoint.convert().findElement<Namespace>()!!
        val flattened = converted.flattenNestedStructs()
        val (moduleElements, classElements) = flattened.elements.partition { it is Struct || it is LanguageUnion }
        val pathSegmentsCode = endpoint.path.joinToString(", ") { segment ->
            when (segment) {
                is Endpoint.Segment.Literal -> """Wirespec.Literal("${segment.value}")"""
                is Endpoint.Segment.Param -> """Wirespec.Param("${segment.identifier.value}", ${segment.reference.toPythonTypeName()})"""
            }
        }
        val endpointClass = Namespace(
            name = converted.name,
            elements = listOf(RawElement("path_segments = [$pathSegmentsCode]")) + classElements,
            extends = converted.extends,
        )
        return LanguageFile(converted.name, buildList {
            addAll(endpointImports)
            addAll(moduleElements)
            add(endpointClass)
        })
            .snakeCaseHandlerAndCallMethods()
            .sanitizeNames(sanitizationConfig)
    }

    private fun Reference.toPythonTypeName(): String = when (this) {
        is Reference.Primitive -> when (type) {
            is Reference.Primitive.Type.String -> "str"
            is Reference.Primitive.Type.Integer -> "int"
            is Reference.Primitive.Type.Number -> "float"
            is Reference.Primitive.Type.Boolean -> "bool"
            is Reference.Primitive.Type.Bytes -> "bytes"
        }
        is Reference.Custom -> value
        is Reference.Iterable -> "list[${reference.toPythonTypeName()}]"
        is Reference.Dict -> "dict[str, ${reference.toPythonTypeName()}]"
        is Reference.Any -> "Any"
        is Reference.Unit -> "None"
    }

    override fun emit(channel: Channel): File =
        channel.convert()
            .sanitizeNames(sanitizationConfig)

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val modelImports = endpoint.importReferences().distinctBy { it.value }
            .map { import("..model.${it.value}", it.value) }
        val endpointImport = import("..endpoint.${endpoint.identifier.value}", "*")
        val endpointName = endpoint.identifier.value

        val file = super.emitEndpointClient(endpoint)
            .sanitizeNames(sanitizationConfig)
            .addSelfReceiverToClientFields()
            .snakeCaseClientFunctions()
            .flattenEndpointTypeRefs(endpointName)

        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase()),
            elements = buildImports("..wirespec") +
                modelImports +
                listOf(endpointImport) +
                file.elements
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        val modelImports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .map { import(".model.${it.value}", it.value) }
        val endpointImports = endpoints.map { import(".endpoint.${it.identifier.value}", "*") }
        val clientImports = endpoints.map { import(".client.${it.identifier.value}Client", "${it.identifier.value}Client") }
        val allImports = modelImports + endpointImports + clientImports
        val endpointNames = endpoints.map { it.identifier.value }

        val file = super.emitClient(endpoints, logger)
            .sanitizeNames(sanitizationConfig)
            .addSelfReceiverToClientFields()
            .snakeCaseClientFunctions()
            .let { f -> endpointNames.fold(f) { acc, name -> acc.flattenEndpointTypeRefs(name) } }

        return File(
            name = Name.of(packageName.toDir() + file.name.pascalCase()),
            elements = buildImports(".wirespec") +
                allImports +
                file.elements
        )
    }

    private fun Identifier.sanitize(): String = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
        .let { if (this is FieldIdentifier) it.sanitizeKeywords() else it }

    private fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    private fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_")
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    private fun sort(definition: Definition) = when (definition) {
        is Enum -> 1
        is Refined -> 2
        is Type -> 3
        is Union -> 4
        is Endpoint -> 5
        is Channel -> 6
    }

    private fun buildImports(wirespecPath: String): List<Element> = listOf(
        import("__future__", "annotations"),
        import("", "re"),
        import("abc", "ABC"),
        import("abc", "abstractmethod"),
        import("dataclasses", "dataclass"),
        import("typing", "Any"),
        import("typing", "Generic"),
        import("typing", "List"),
        import("typing", "Optional"),
        import("", "enum"),
        import(wirespecPath, "T"),
        import(wirespecPath, "Wirespec"),
        import(wirespecPath, "_raise"),
    )

    private fun <T : Element> T.snakeCaseHandlerAndCallMethods(): T = transform {
        matchingElements { iface: Interface ->
            if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) {
                iface.copy(
                    elements = iface.elements.map { element ->
                        if (element is LanguageFunction) {
                            element.copy(name = Name.of(element.name.snakeCase()))
                        } else element
                    },
                )
            } else iface
        }
    }

    private fun <T : Element> T.flattenEndpointTypeRefs(endpointName: String): T = transform {
        type { type, _ ->
            if (type is LanguageType.Custom && type.name.startsWith("$endpointName.")) {
                val suffix = type.name.removePrefix("$endpointName.")
                if (suffix == "Call" || suffix == "Handler") type
                else type.copy(name = suffix)
            } else type
        }
    }

    private fun <T : Element> T.addSelfReceiverToClientFields(): T {
        val struct = (this as? File)?.findElement<Struct>()
        val fieldNames = struct?.fields?.map { it.name.value() }?.toSet() ?: emptySet()
        if (fieldNames.isEmpty()) return this

        return transform {
            statementAndExpression { stmt, tr ->
                when (stmt) {
                    is FieldCall -> {
                        if (stmt.receiver == null && stmt.field.value() in fieldNames) {
                            FieldCall(receiver = VariableReference(Name.of("self")), field = stmt.field)
                        } else {
                            FieldCall(
                                receiver = stmt.receiver?.let { tr.transformExpression(it) },
                                field = stmt.field,
                            )
                        }
                    }
                    else -> stmt.transformChildren(tr)
                }
            }
        }
    }

    private fun <T : Element> T.snakeCaseClientFunctions(): T = transform {
        matchingElements { func: LanguageFunction ->
            func.copy(
                name = Name.of(func.name.snakeCase()),
                parameters = listOf(Parameter(Name.of("self"), LanguageType.Custom(""))) + func.parameters,
            )
        }
        statementAndExpression { stmt, tr ->
            when (stmt) {
                is FunctionCall -> {
                    val nameStr = stmt.name.value()
                    val newName = if ("." in nameStr) stmt.name else Name.of(Name.of(nameStr).snakeCase())
                    FunctionCall(
                        name = newName,
                        receiver = stmt.receiver?.let { tr.transformExpression(it) },
                        arguments = stmt.arguments.mapValues { (_, v) -> tr.transformExpression(v) },
                        isAwait = stmt.receiver != null,
                    )
                }
                else -> stmt.transformChildren(tr)
            }
        }
    }

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "False", "None", "True", "and", "as", "assert",
            "break", "class", "continue", "def", "del",
            "elif", "else", "except", "finally", "for",
            "from", "global", "if", "import", "in",
            "is", "lambda", "nonlocal", "not", "or",
            "pass", "raise", "return", "try", "while",
            "with", "yield"
        )
    }

}
