package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.EndpointDefinition
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.TypeDefinition
import community.flock.wirespec.compiler.core.parse.Shape
import community.flock.wirespec.compiler.utils.Logger

interface TypeDefinitionEmitter : TypeDefinitionEmitterLogger {
    fun TypeDefinition.emit(): String

    fun TypeDefinition.Name.emit(): String

    fun Type.emit(): String

    fun Shape.emit(): String

    fun Shape.Field.emit(): String

    fun Shape.Field.Key.emit(): String

    fun Shape.Field.Value.emit(): String
}



interface TypeDefinitionEmitterLogger {
    fun TypeDefinition.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type", block)

    fun TypeDefinition.Name.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Name", block)

    fun Type.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape", block)

    fun Shape.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape", block)

    fun Shape.Field.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape Field", block)

    fun Shape.Field.Key.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape Field Key", block)

    fun Shape.Field.Value.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape Field Value", block)
}
