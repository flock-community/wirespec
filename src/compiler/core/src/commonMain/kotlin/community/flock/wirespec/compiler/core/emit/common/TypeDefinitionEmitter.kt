package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Type

interface TypeDefinitionEmitter {
    fun Type.emit(ast: AST): String
}
