package community.flock.wirespec.emitters.python

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.LanguageEmitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger

interface PythonEmitters:
    PythonIdentifierEmitter,
    PythonTypeDefinitionEmitter,
    PythonEndpointDefinitionEmitter,
    PythonChannelDefinitionEmitter,
    PythonEnumDefinitionEmitter,
    PythonUnionDefinitionEmitter,
    PythonRefinedTypeDefinitionEmitter

open class PythonEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared()
) : LanguageEmitter(), PythonEmitters {

    val import = """
        |import re
        |
        |from abc import abstractmethod
        |from dataclasses import dataclass
        |from typing import List, Optional
        |from enum import Enum
        |
        |from ..wirespec import T, Wirespec
        |
    """.trimMargin()

    override val extension = FileExtension.Python

    override val shared = PythonShared

    override val singleLineComment = "#"

    fun sort(definition: Definition) = when (definition) {
        is Enum -> 1
        is Refined -> 2
        is Type -> 3
        is Union -> 4
        is Endpoint -> 5
        is Channel -> 6
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> {
        val statements = module.statements.sortedBy(::sort).toNonEmptyListOrNull()!!
        return super<LanguageEmitter>.emit(module.copy(statements = statements), logger).let {
            fun emitInit(def: Definition) = "from .${emit(def.identifier)} import ${emit(def.identifier)}"
            val init = Emitted(
                packageName.toDir() + "__init__",
                "from . import model\nfrom . import endpoint\nfrom . import wirespec\n"
            )
            val initEndpoint = Emitted(
                packageName.toDir() + "endpoint/" + "__init__",
                module.statements.filter { it is Endpoint }.map { stmt -> emitInit(stmt) }.joinToString("\n")
            )
            val initModel = Emitted(
                packageName.toDir() + "model/" + "__init__",
                module.statements.filter { it is Model }.map { stmt -> emitInit(stmt) }.joinToString("\n")
            )
            val shared = Emitted(packageName.toDir() + "wirespec", shared.source)
            if (emitShared.value)
                it + init + initEndpoint + initModel + shared
            else
                it + init
        }
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted {
        val subPackageName = packageName + definition
        return super<LanguageEmitter>.emit(definition, module, logger).let {
            Emitted(
                file = subPackageName.toDir() + it.file,
                result = """
                    |${import}
                    |${it.result}
                """.trimMargin().trimStart()
            )
        }
    }
}
