package community.flock.wirespec.compiler.emit.common

import community.flock.wirespec.WireSpecException
import community.flock.wirespec.compiler.parse.AST
import community.flock.wirespec.compiler.parse.Definition.TypeDefinition
import community.flock.wirespec.compiler.parse.Node
import community.flock.wirespec.compiler.utils.log

abstract class Emitter : TypeDefinitionEmitter {

    fun emit(ast: AST): String = ast.map { it.emit() }.reduce { acc, cur -> acc + cur }

    private fun Node.emit(): String = run {
        log("Emitting Node $this")
        when (this) {
            is TypeDefinition -> emit()
            else -> throw WireSpecException.CompilerException.EmitterException("Unknown Node: $this")
        }
    }

    companion object {
        internal const val SPACER = "  "
    }
}
