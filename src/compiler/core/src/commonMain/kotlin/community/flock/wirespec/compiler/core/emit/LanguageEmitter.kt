package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger

abstract class LanguageEmitter :
    Emitter,
    Emitters {

    abstract val shared: Shared?

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast
        .modules.flatMap { m ->
            logger.info("Emitting Nodes from ${m.fileUri.value} ")
            emit(m, logger)
        }
        .map { e -> Emitted(e.file + "." + extension.value, e.result) }

    open fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> = module
        .statements
        .map { emit(it, module, logger) }

    open fun emit(definition: Definition, module: Module, logger: Logger): Emitted = run {
        logger.info("Emitting ${definition::class.simpleName} ${definition.identifier.value}")
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
        fun String.isStatusCode() = toIntOrNull()?.let { it in 100..599 } ?: false
    }
}
