package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
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
    fun emit(type: Type, module: Module): String

    fun Type.Shape.emit(): String

    fun Field.emit(): String

    fun Reference.emit(): String

    fun Refined.emitValidator(): String

    fun Reference.Primitive.Type.Constraint.emit(): String

    val Reference.Primitive.Type.Constraint.RegExp.expression get() =
        value.split("/").drop(1).dropLast(1).joinToString("/")
}

interface EnumDefinitionEmitter {
    fun emit(enum: Enum, module: Module): String
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

interface ClientEmitter {
    fun emitClient(ast: AST): Emitted

    fun AST.emitClientEndpointRequest() = modules
        .flatMap { it.statements }
        .filterIsInstance<Endpoint>()
        .map { endpoint -> Pair(endpoint, endpoint.requests.first()) }
}

interface NotYetImplemented {
    val singleLineComment: String
    fun notYetImplemented() = "$singleLineComment TODO(\"Not yet implemented\")\n"
}
