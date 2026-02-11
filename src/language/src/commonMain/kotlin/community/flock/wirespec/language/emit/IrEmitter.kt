package community.flock.wirespec.language.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger

interface IrEmitter : Emitter {

    val shared: Shared?

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast
        .modules.flatMap { m ->
            logger.info("Emitting Nodes from ${m.fileUri.value} ")
            emit(m, logger)
        }
        .map { e -> Emitted(e.file + "." + extension.value, e.result) }

    fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> = module
        .statements
        .map { emit(it, module, logger) }

    fun emit(definition: Definition, module: Module, logger: Logger): Emitted = run {
        logger.info("Emitting ${definition::class.simpleName} ${definition.identifier.value}")
        return when (definition) {
            is Type -> emit(definition, module)
            is Endpoint -> emit(definition)
            is Enum -> emit(definition, module)
            is Refined -> emit(definition)
            is Union -> emit(definition)
            is Channel -> emit(definition)
        }
    }

    val Reference.Primitive.Type.Constraint.RegExp.expression get() =
        value.split("/").drop(1).dropLast(1).joinToString("/")

    fun emit(type: Type, module: Module): Emitted
    fun emit(enum: Enum, module: Module): Emitted
    fun emit(refined: Refined): Emitted
    fun emit(endpoint: Endpoint): Emitted
    fun emit(union: Union): Emitted
    fun emit(channel: Channel): Emitted

    fun emit(identifier: Identifier): String
}
