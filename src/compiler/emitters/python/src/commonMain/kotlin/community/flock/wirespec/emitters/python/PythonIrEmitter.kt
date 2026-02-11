package community.flock.wirespec.emitters.python

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.language.emit.IrEmitter
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
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
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.converter.convertConstraint
import community.flock.wirespec.language.converter.convertWithValidation
import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.ConstructorStatement
import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.FieldCall
import community.flock.wirespec.language.core.File
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.RawElement
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.Static
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.findElement
import community.flock.wirespec.language.core.function
import community.flock.wirespec.language.core.transformChildren
import community.flock.wirespec.language.core.transformMatchingElements
import community.flock.wirespec.language.core.transformer
import community.flock.wirespec.language.generator.generatePython
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.language.core.Enum as LanguageEnum
import community.flock.wirespec.language.core.Function as LanguageFunction
import community.flock.wirespec.language.core.File as LanguageFile
import community.flock.wirespec.language.core.Type as LanguageType
import community.flock.wirespec.language.core.Union as LanguageUnion

open class PythonIrEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared()
) : IrEmitter {

    override fun File.generate(): String = generatePython()

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

    override val extension = FileExtension.Python

    val sharedImport = """
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

    override val shared = object : Shared {
        override val packageString = "shared"
        override val source = sharedImport + AstShared(packageString).convert().generatePython()
            .replace("path: Path\n", "path: Wirespec.Path\n")
            .replace("method: Method\n", "method: Wirespec.Method\n")
            .replace("queries: Queries\n", "queries: Wirespec.Queries\n")
            .replace("request: RawRequest", "request: Wirespec.RawRequest")
            .replace("-> RawResponse", "-> Wirespec.RawResponse")
            .replace("type: Type)", "type: type[T])")
    }

    fun sort(definition: Definition) = when (definition) {
        is Enum -> 1
        is Refined -> 2
        is Type -> 3
        is Union -> 4
        is Endpoint -> 5
        is Channel -> 6
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val statements = module.statements.sortedBy(::sort).toNonEmptyListOrNull()!!
        return super.emit(module.copy(statements = statements), logger).let {
            fun emitInit(def: Definition) = "from .${def.identifier.sanitize()} import ${def.identifier.sanitize()}"
            val init = File(
                packageName.toDir() + "__init__",
                listOf(RawElement("from . import model\nfrom . import endpoint\nfrom . import wirespec"))
            )
            val initEndpoint = File(
                packageName.toDir() + "endpoint/" + "__init__",
                listOf(RawElement(module.statements.filter { it is Endpoint }.map { stmt -> emitInit(stmt) }.joinToString("\n")))
            )
            val initModel = File(
                packageName.toDir() + "model/" + "__init__",
                listOf(RawElement(module.statements.filter { it is Model }.map { stmt -> emitInit(stmt) }.joinToString("\n")))
            )
            val shared = File(packageName.toDir() + "wirespec", listOf(RawElement(shared.source)))
            val parentInits = packageName.value.split(".")
                .dropLast(1)
                .runningFold("") { acc, segment -> if (acc.isEmpty()) segment else "$acc/$segment" }
                .drop(1)
                .map { File("$it/__init__", emptyList()) }
            if (emitShared.value)
                it + init + initEndpoint + initModel + shared + parentInits
            else
                it + init + parentInits
        }
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val subPackageName = packageName + definition
        return super.emit(definition, module, logger).let { file ->
            File(
                name = subPackageName.toDir() + file.name,
                elements = listOf(RawElement(import)) + file.elements
            )
        }
    }

    fun Identifier.sanitize() = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
        .let { if (this is FieldIdentifier) it.sanitizeKeywords() else it }

    fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    override fun emit(type: Type, module: Module): File {
        val imports = type.importReferences().distinctBy { it.value }
            .joinToString("\n") { "from .${it.value} import ${it.value}" }
        val fieldNames = type.shape.value.map { it.identifier.value }.toSet()
        // Add self receiver to bare FieldCalls that reference type fields
        val addSelfReceiver = transformer(
            transformStatement = { s, t ->
                if (s is FieldCall && s.receiver == null && s.field in fieldNames) {
                    FieldCall(receiver = VariableReference("self"), field = s.field)
                } else {
                    s.transformChildren(t)
                }
            },
            transformExpression = { e, t ->
                if (e is FieldCall && e.receiver == null && e.field in fieldNames) {
                    FieldCall(receiver = VariableReference("self"), field = e.field)
                } else {
                    e.transformChildren(t)
                }
            },
        )
        val file = type.convertWithValidation(module)
            .transformMatchingElements { fn: LanguageFunction ->
                if (fn.name == "validate") {
                    val transformedBody = fn.body.map { addSelfReceiver.transformStatement(it) }
                    fn.copy(
                        parameters = listOf(community.flock.wirespec.language.core.Parameter("self", LanguageType.Custom(""))),
                        body = transformedBody,
                    )
                } else fn
            }
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements)
        else file
    }

    override fun emit(enum: Enum, module: Module): File = enum
        .convert()
        .transformMatchingElements { languageEnum: LanguageEnum ->
            languageEnum.copy(
                entries = languageEnum.entries.map {
                    LanguageEnum.Entry(it.name.sanitizeEnum().sanitizeKeywords(), listOf("\"${it.name}\""))
                },
            )
        }

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_")
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    // endregion

    // region UnionDefinitionEmitter

    override fun emit(union: Union): File =
        union.convert()

    // endregion

    // region RefinedTypeDefinitionEmitter

    override fun emit(refined: Refined): File {
        val file = refined.convert()
        val struct = file.findElement<Struct>()!!
        val validateFunction = struct.elements.filterIsInstance<LanguageFunction>().first { it.name == "validate" }
        val constraintExpr = refined.reference.convertConstraint(FieldCall(VariableReference("self"), "value"))
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
            .transformMatchingElements { s: Struct ->
                s.copy(elements = listOf(validate, toString))
            }
    }

    // endregion

    // region EndpointDefinitionEmitter

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.importReferences().distinctBy { it.value }
            .joinToString("\n") { "from ..model.${it.value} import ${it.value}" }
        val converted = endpoint.convert().findElement<Interface>()!!
        val (moduleElements, classElements) = flattenEndpointForPython(converted)
        val endpointClass = Static(
            name = converted.name,
            elements = classElements,
            extends = converted.extends.firstOrNull(),
        )
        val elements = mutableListOf<Element>()
        if (imports.isNotEmpty()) elements.add(RawElement(imports))
        elements.addAll(moduleElements)
        elements.add(endpointClass)
        return LanguageFile(converted.name, elements)
    }

    private fun flattenEndpointForPython(converted: Interface): Pair<List<Element>, List<Element>> {
        val moduleElements = mutableListOf<Element>()
        val classElements = mutableListOf<Element>()
        for (element in converted.elements) {
            when (element) {
                is Struct -> {
                    val nested = element.elements.filterIsInstance<Struct>()
                    if (nested.isNotEmpty()) {
                        val nestedNames = nested.map { it.name }.toSet()
                        for (nestedStruct in nested) {
                            moduleElements.add(nestedStruct.copy(name = "${element.name}${nestedStruct.name}"))
                        }
                        moduleElements.add(qualifyNestedRefs(element, nestedNames))
                    } else {
                        moduleElements.add(element)
                    }
                }

                is LanguageUnion -> moduleElements.add(element)
                else -> classElements.add(element)
            }
        }
        return Pair(moduleElements, classElements)
    }

    private fun qualifyNestedRefs(struct: Struct, nestedNames: Set<String>): Struct {
        val qualifiedFields = struct.fields.map { field ->
            val typeName = (field.type as? LanguageType.Custom)?.name
            if (typeName != null && typeName in nestedNames) {
                field.copy(type = LanguageType.Custom("${struct.name}$typeName"))
            } else field
        }
        val qualifiedConstructors = struct.constructors.map { c ->
            c.copy(body = c.body.map { stmt ->
                if (stmt is Assignment) {
                    val value = stmt.value
                    if (value is ConstructorStatement) {
                        val typeName = (value.type as? LanguageType.Custom)?.name
                        if (typeName != null && typeName in nestedNames) {
                            Assignment(stmt.name, value.copy(type = LanguageType.Custom("${struct.name}$typeName")))
                        } else stmt
                    } else stmt
                } else stmt
            })
        }
        return struct.copy(
            fields = qualifiedFields,
            constructors = qualifiedConstructors,
            elements = struct.elements.filter { it !is Struct },
        )
    }

    // endregion

    // region ChannelDefinitionEmitter

    override fun emit(channel: Channel): File =
        channel.convert()

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
