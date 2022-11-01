package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter(val logger: Logger) : TypeDefinitionEmitter {

    open fun emit(ast: AST): Either<CompilerException, String> = either {
        ast
            .map { it.emit() }
            .fold("") { acc, cur -> acc + cur }
            .dropLast(1) // drop last newline
    }

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
