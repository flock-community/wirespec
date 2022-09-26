package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter(val logger: Logger) : TypeDefinitionEmitter {

    fun emit(ast: AST): Either<CompilerException, String> = either {
        ast
            .map { it.emit() }
            .reduceOrNull { acc, cur -> acc + cur }
            .let { it ?: "" }
    }

    private fun Node.emit(): String = run {
        logger.log("Emitting Node $this")
        when (this) {
            is Type -> emit()
            is Endpoint -> emit()
            is Shape -> emit()
            is Endpoint.PathSegment -> emit()
        }
    }

    companion object {
        const val SPACER = "  "
    }

}
