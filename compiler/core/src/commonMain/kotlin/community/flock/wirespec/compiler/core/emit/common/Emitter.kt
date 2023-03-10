package community.flock.wirespec.compiler.core.emit.common

import EndpointDefinitionEmitter
import arrow.core.continuations.eagerEffect
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.EndpointDefinition
import community.flock.wirespec.compiler.core.parse.TypeDefinition
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter(val logger: Logger, val split: Boolean = false) : TypeDefinitionEmitter, EndpointDefinitionEmitter {

    open fun emit(ast: AST) = eagerEffect<CompilerException, List<Pair<String, String>>> {
        ast
            .map {
                logger.log("Emitting Node $this")
                when (it) {
                    is TypeDefinition -> it.name.value to it.emit()
                    is EndpointDefinition -> it.name.value to it.emit()
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
    }.toValidated()

    companion object {
        const val SPACER = "  "
    }

}
