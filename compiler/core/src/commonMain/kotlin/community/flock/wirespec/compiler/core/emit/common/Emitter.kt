package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter(val logger: Logger, val split: Boolean = false) : TypeDefinitionEmitter {

    open fun emit(ast: AST) = ast
        .map {
            logger.log("Emitting Node $this")
            when (it) {
                is Type -> it.name.value to it.emit()
            }
        }
        .let {
            if (split) it
            else listOf(
                it.first().first to it.fold("") { acc, cur -> acc + cur.second }
                    // drop last newline
                    .dropLast(1)
            )
        }

    companion object {
        const val SPACER = "  "
    }

}
