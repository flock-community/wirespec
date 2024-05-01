package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union


interface Emitters :
    TypeDefinitionEmitter,
    EnumDefinitionEmitter,
    RefinedTypeDefinitionEmitter,
    EndpointDefinitionEmitter,
    UnionDefinitionEmitter,
    IdentifierEmitter

interface TypeDefinitionEmitter {
    fun Type.emit(ast: AST): String
}

interface EnumDefinitionEmitter {
    fun Enum.emit(): String
}

interface RefinedTypeDefinitionEmitter {
    fun Refined.emit(): String
}

interface EndpointDefinitionEmitter {
    fun Endpoint.emit(): String
}

interface UnionDefinitionEmitter {
    fun Union.emit(): String
}

interface IdentifierEmitter {
    fun Identifier.emit(): String = value
}
