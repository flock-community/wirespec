package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger

interface Emitters : TypeDefinitionEmitter, RefinedTypeDefinitionEmitter, EndpointDefinitionEmitter

abstract class Emitter(val logger: Logger, val split: Boolean = false) : Emitters {

    open fun emit(ast: AST) = ast
        .map {
            logger.log("Emitting Node $this")
            when (it) {
                is Type -> it.name to it.emit()
                is Endpoint -> it.name to it.emit()
                is Refined -> it.name to it.emit()
            }
        }
        .run {
            if (split) this
            else listOf(first().first to joinToString("") { it.second }.dropLast(1)) // drop last newline
        }

    companion object {
        const val SPACER = "  "
    }

}
