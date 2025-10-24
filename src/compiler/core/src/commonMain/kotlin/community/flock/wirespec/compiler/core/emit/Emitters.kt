package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union

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

    fun Reference.Primitive.Type.Constraint.emit(): String

    val Reference.Primitive.Type.Constraint.RegExp.expression get() =
        value.split("/").drop(1).dropLast(1).joinToString("/")
}

interface EnumDefinitionEmitter {
    fun emit(enum: Enum, module: Module): String
}

interface RefinedTypeDefinitionEmitter {
    fun emit(refined: Refined): String

    fun Refined.emitValidator(): String
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

interface ClientEmitter : HasExtension {
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
