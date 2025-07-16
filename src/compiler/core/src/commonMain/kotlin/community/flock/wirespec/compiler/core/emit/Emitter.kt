package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter : Emitters, BaseEmitter, ParamEmitter, SpaceEmitter, ImportEmitter, EndpointEmitter {

    open fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast
        .modules.flatMap { emit(it, logger) }
        .map { e -> Emitted(e.file + "." + extension.value, e.result) }

    open fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> = module
        .statements
        .map { emit(it, module, logger) }

    open fun emit(definition: Definition, module: Module, logger: Logger): Emitted = run {
        logger.info("Emitting Node ${definition.identifier.value}")
        when (definition) {
            is Type -> Emitted(emit(definition.identifier), emit(definition, module))
            is Endpoint -> Emitted(emit(definition.identifier), emit(definition))
            is Enum -> Emitted(emit(definition.identifier), emit(definition, module))
            is Refined -> Emitted(emit(definition.identifier), emit(definition))
            is Union -> Emitted(emit(definition.identifier), emit(definition))
            is Channel -> Emitted(emit(definition.identifier), emit(definition))
        }
    }

    companion object {
        fun String.firstToUpper() = replaceFirstChar(Char::uppercase)
        fun String.firstToLower() = replaceFirstChar(Char::lowercase)
        fun Module.needImports() = statements.any { it is Endpoint || it is Enum || it is Refined }
        fun Module.hasEndpoints() = statements.any { it is Endpoint }
        fun String.isStatusCode() = toIntOrNull()?.let { it in 0..599 } ?: false
    }
}

interface HasEmitters {
    val emitters: NonEmptySet<Emitter>
}
