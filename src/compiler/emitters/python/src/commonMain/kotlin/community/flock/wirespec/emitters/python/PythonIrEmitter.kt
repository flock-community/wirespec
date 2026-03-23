package community.flock.wirespec.emitters.python

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.PackageName
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
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.plus
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.emit.placeInModule
import community.flock.wirespec.ir.emit.prependImports
import community.flock.wirespec.ir.generator.PythonGenerator
import community.flock.wirespec.ir.transformer.SanitizationConfig
import community.flock.wirespec.ir.transformer.injectSelfReceiverToValidate
import community.flock.wirespec.ir.transformer.sanitizeEnumEntries
import community.flock.wirespec.ir.transformer.sanitizeFieldName
import community.flock.wirespec.ir.transformer.sanitizeNames
import community.flock.wirespec.ir.transformer.sortKey

open class PythonIrEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared()
) : IrEmitter {

    override val generator = PythonGenerator

    val import = """
        |from __future__ import annotations
        |
        |import re
        |
        |from abc import ABC, abstractmethod
        |from dataclasses import dataclass
        |from typing import Any, Generic, List, Optional
        |import enum
        |
        |from ..wirespec import T, Wirespec, _raise
        |
    """.trimMargin()

    val rootImport = """
        |from __future__ import annotations
        |
        |import re
        |
        |from abc import ABC, abstractmethod
        |from dataclasses import dataclass
        |from typing import Any, Generic, List, Optional
        |import enum
        |
        |from .wirespec import T, Wirespec, _raise
        |
    """.trimMargin()

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

    override fun emitShared(): File? {

        val source = PackageName("shared").convert()

        return if (emitShared.value) {
            File(
                Name.of(packageName.toDir() + "wirespec"),
                listOf(raw(sharedSource + PythonGenerator.generate(source)))
            )
        } else {
            null
        }
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val statements = module.statements.sortedBy(::sort).toNonEmptyListOrNull()!!
        return super.emit(module.copy(statements = statements), logger).let {
            fun emitInit(def: Definition) = "from .${def.identifier.sanitize()} import ${def.identifier.sanitize()}"
            val hasEndpoints = module.statements.any { it is Endpoint }
            val init = File(
                Name.of(packageName.toDir() + "__init__"),
                listOf(RawElement("from . import model\nfrom . import endpoint" + (if (hasEndpoints) "\nfrom . import client" else "") + "\nfrom . import wirespec"))
            )
            val initEndpoint = File(
                Name.of(packageName.toDir() + "endpoint/" + "__init__"),
                listOf(RawElement(module.statements.filter { it is Endpoint }.map { stmt -> emitInit(stmt) }.joinToString("\n")))
            )
            val initModel = File(
                Name.of(packageName.toDir() + "model/" + "__init__"),
                listOf(RawElement(module.statements.filter { it is Model }.map { stmt -> emitInit(stmt) }.joinToString("\n")))
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
        return type.convertWithValidation(module)
            .injectSelfReceiverToValidate(fieldNames)
            .sanitizeNames(sanitizationConfig)
            .prependImports(typeImports.takeIf { it.isNotEmpty() })
    }

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .sanitizeEnumEntries(sanitizeEntry = { it.sanitizeEnum().sanitizeKeywords() })
        .sanitizeNames(sanitizationConfig)

    override fun emit(union: Union): File =
        union.convert()
            .sanitizeNames(sanitizationConfig)

    override fun emit(refined: Refined): File = refined
        .convert()
        .replaceRefinedFunctions(refined)
        .sanitizeNames(sanitizationConfig)

    override fun emit(endpoint: Endpoint): File {
        val endpointImports = endpoint.importReferences().distinctBy { it.value }
            .map { import("..model.${it.value}", it.value) }
        return endpoint.convert()
            .splitEndpointStructsToModuleLevel()
            .let { it.copy(elements = endpointImports + it.elements) }
            .snakeCaseHandlerAndCallMethods()
            .sanitizeNames(sanitizationConfig)
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
        val clientImports =
            endpoints.map { import(".client.${it.identifier.value}Client", "${it.identifier.value}Client") }
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

    // endregion

    // region UnionDefinitionEmitter

    override fun emit(union: Union): File =
        union.convert()
            .sanitizeNames()

    // endregion

    // region RefinedTypeDefinitionEmitter

    override fun emit(refined: Refined): File {
        val file = refined.convert()
        val struct = file.findElement<Struct>()!!
        val validateFunction = struct.elements.filterIsInstance<LanguageFunction>().first { it.name == Name.of("validate") }
        val constraintExpr = refined.reference.convertConstraint(FieldCall(VariableReference(Name.of("self")), Name.of("value")))
        val validate = function("validate") {
            arg("self", LanguageType.Custom(""))
            returnType(LanguageType.Boolean)
            returns(constraintExpr)
        }
        val toStringExpr = when (refined.reference.type) {
            is Reference.Primitive.Type.String -> "self.value"
            else -> "str(self.value)"
        }
        val toString = function("__str__") {
            arg("self", LanguageType.Custom(""))
            returnType(LanguageType.String)
            returns(RawExpression(toStringExpr))
        }
        return file
            .transform {
                matchingElements { s: Struct ->
                    s.copy(elements = listOf(validate, toString))
                }
            }
            .sanitizeNames()
    }

    // endregion

    // region EndpointDefinitionEmitter

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.importReferences().distinctBy { it.value }
            .joinToString("\n") { "from ..model.${it.value} import ${it.value}" }
        val converted = endpoint.convert().findElement<Namespace>()!!
        val flattened = converted.flattenNestedStructs()
        val (moduleElements, classElements) = flattened.elements.partition { it is Struct || it is LanguageUnion }
        val endpointClass = Namespace(
            name = converted.name,
            elements = classElements,
            extends = converted.extends,
        )
        val elements = buildList {
            if (imports.isNotEmpty()) add(RawElement(imports))
            addAll(moduleElements)
            add(endpointClass)
        }
        return LanguageFile(converted.name, elements)
            .sanitizeNames()
            .snakeCaseHandlerAndCallMethods()
    }

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

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.importReferences().distinctBy { it.value }
            .joinToString("\n") { "from ..model.${it.value} import ${it.value}" }
        val endpointImport = "from ..endpoint.${endpoint.identifier.value} import *"
        val endpointName = endpoint.identifier.value

        val file = super.emitEndpointClient(endpoint)
            .sanitizeNames()
            .addSelfReceiverToClientFields()
            .snakeCaseClientFunctions()
            .flattenEndpointTypeRefs(endpointName)

        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase()),
            elements = listOf(RawElement(import)) +
                listOfNotNull(
                    if (imports.isNotEmpty()) RawElement(imports) else null,
                    RawElement(endpointImport),
                ) +
                file.elements
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        val imports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .joinToString("\n") { "from .model.${it.value} import ${it.value}" }
        val endpointImports = endpoints.joinToString("\n") { "from .endpoint.${it.identifier.value} import *" }
        val clientImports = endpoints.joinToString("\n") { "from .client.${it.identifier.value}Client import ${it.identifier.value}Client" }
        val allImports = listOf(imports, endpointImports, clientImports).filter { it.isNotEmpty() }.joinToString("\n")
        val endpointNames = endpoints.map { it.identifier.value }

        val file = super.emitClient(endpoints, logger)
            .sanitizeNames()
            .addSelfReceiverToClientFields()
            .snakeCaseClientFunctions()
            .let { f -> endpointNames.fold(f) { acc, name -> acc.flattenEndpointTypeRefs(name) } }

        return File(
            name = Name.of(packageName.toDir() + file.name.pascalCase()),
            elements = listOf(RawElement(rootImport)) +
                (if (allImports.isNotEmpty()) listOf(RawElement(allImports)) else emptyList()) +
                file.elements
        )
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
                    val newName = if ("." in nameStr) {
                        stmt.name
                    } else {
                        Name.of(Name.of(nameStr).snakeCase())
                    }
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

    // endregion

    // region ChannelDefinitionEmitter

    override fun emit(channel: Channel): File =
        channel.convert()
            .sanitizeNames()

    // endregion

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
