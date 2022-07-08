package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter(val logger: Logger) : TypeDefinitionEmitter {

    fun emit(ast: AST): String = ast
        .map { it.emit() }
        .takeIf { it.isNotEmpty() }
        ?.reduce { acc, cur -> acc + cur } ?: ""

    private fun Node.emit(): String = run {
        logger.log("Emitting Node $this")
        when (this) {
            is Type -> emit()
        }
    }

    companion object {
        const val SPACER = "  "
    }

}
