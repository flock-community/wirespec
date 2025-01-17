package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
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
    IdentifierEmitter,
    ChannelDefinitionEmitter,
    NotYetImplemented

interface TypeDefinitionEmitter {
    fun emit(type: Type, ast: AST): String
}

interface EnumDefinitionEmitter {
    fun emit(enum: Enum, ast: AST): String
}

interface RefinedTypeDefinitionEmitter {
    fun emit(refined: Refined): String
}

interface EndpointDefinitionEmitter {
    fun emit(endpoint: Endpoint): String
}

interface UnionDefinitionEmitter {
    fun emit(union: Union): String
}

interface ChannelDefinitionEmitter {
    fun emit(channel: Channel): String
}

interface IdentifierEmitter {
    fun emit(identifier: Identifier): String
}

interface NotYetImplemented {
    val singleLineComment: String
    fun notYetImplemented() = "$singleLineComment TODO(\"Not yet implemented\")\n"
}
