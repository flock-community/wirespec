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

public abstract class LanguageEmitter :
    Emitter,
    Emitters {

    public abstract val shared: Shared?

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast
        .modules.flatMap { m ->
            logger.info("Emitting Nodes from ${m.fileUri.value} ")
            emit(m, logger)
        }
        .map { e -> Emitted(e.file + "." + extension.value, e.result) }

    public open fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> = module
        .statements
        .map { emit(it, module, logger) }

    public open fun emit(definition: Definition, module: Module, logger: Logger): Emitted = run {
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

    public companion object {
        public fun String.firstToUpper(): String = replaceFirstChar(Char::uppercase)
        public fun String.firstToLower(): String = replaceFirstChar(Char::lowercase)
        public fun Module.needImports(): Boolean = statements.any { it is Endpoint || it is Enum || it is Refined }
        public fun Module.irNeedsWirespecImport(): Boolean = statements.any {
            it is Endpoint || it is Enum || it is Refined || it is Type || it is Channel
        }
        public fun Module.hasEndpoints(): Boolean = statements.any { it is Endpoint }
        public fun String.isStatusCode(): Boolean = toIntOrNull()?.let { it in 100..599 } ?: false
    }
}
