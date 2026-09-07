package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union

internal interface Emitters :
    TypeDefinitionEmitter,
    EnumDefinitionEmitter,
    RefinedTypeDefinitionEmitter,
    EndpointDefinitionEmitter,
    UnionDefinitionEmitter,
    IdentifierEmitter,
    ChannelDefinitionEmitter,
    RpcDefinitionEmitter,
    NotYetImplemented

public interface TypeDefinitionEmitter {
    public fun emit(type: Type, module: Module): String

    public fun Type.Shape.emit(): String

    public fun Field.emit(): String

    public fun Reference.emit(): String

    public fun Reference.Primitive.Type.Constraint.emit(): String

    public val Reference.Primitive.Type.Constraint.RegExp.expression: String get() =
        value.split("/").drop(1).dropLast(1).joinToString("/")
}

public interface EnumDefinitionEmitter {
    public fun emit(enum: Enum, module: Module): String
}

public interface RefinedTypeDefinitionEmitter {
    public fun emit(refined: Refined): String

    public fun Refined.emitValidator(): String
}

public interface EndpointDefinitionEmitter {
    public fun emit(endpoint: Endpoint): String
}

public interface UnionDefinitionEmitter {
    public fun emit(union: Union): String
}

public interface ChannelDefinitionEmitter {
    public fun emit(channel: Channel): String
}

public fun interface RpcDefinitionEmitter {
    public fun emit(rpc: Rpc): String
}

public interface IdentifierEmitter {
    public fun emit(identifier: Identifier): String
}

internal interface NotYetImplemented {
    val singleLineComment: String
    fun notYetImplemented() = "$singleLineComment TODO(\"Not yet implemented\")\n"
}
