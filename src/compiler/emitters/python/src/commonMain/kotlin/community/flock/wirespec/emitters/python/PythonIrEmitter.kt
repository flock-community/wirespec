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
import community.flock.wirespec.compiler.core.parse.ast.Rpc
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
        val statements = module.statements.sortedBy { it.sortKey() }.toNonEmptyListOrNull()!!
        val emitted = super.emit(module.copy(statements = statements), logger)

        fun emitInitImport(def: Definition) = import(".${def.identifier.sanitize()}", def.identifier.sanitize())
        val hasEndpoints = module.statements.any { it is Endpoint }

        val init = File(
            Name.of(packageName.toDir() + "__init__"),
            buildList {
                add(import(".", "model"))
                add(import(".", "endpoint"))
                if (hasEndpoints) add(import(".", "client"))
                add(import(".", "wirespec"))
            }
        )
        val initEndpoint = File(
            Name.of(packageName.toDir() + "endpoint/" + "__init__"),
            module.statements.filterIsInstance<Endpoint>().map(::emitInitImport)
        )
        val initModel = File(
            Name.of(packageName.toDir() + "model/" + "__init__"),
            module.statements.filterIsInstance<Model>().map(::emitInitImport)
        )
        val initClient = if (hasEndpoints) listOf(
            File(Name.of(packageName.toDir() + "client/" + "__init__"), emptyList())
        ) else emptyList()
        val parentInits = packageName.value.split(".")
            .dropLast(1)
            .runningFold("") { acc, segment -> if (acc.isEmpty()) segment else "$acc/$segment" }
            .drop(1)
            .map { File(Name.of("$it/__init__"), emptyList()) }

        return if (emitShared.value)
            emitted + init + initEndpoint + initModel + initClient + parentInits
        else
            emitted + init + parentInits
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

    override fun emit(rpc: Rpc): File =
        rpc.convert()
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
