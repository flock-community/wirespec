package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger

interface TypeDefinitionEmitter : TypeDefinitionEmitterLogger {
    fun Type.emit(): String

    fun Type.Name.emit(): String

    fun Type.Shape.emit(): String

    fun Type.Shape.Field.emit(): String

    fun Type.Shape.Field.Key.emit(): String

    fun Type.Shape.Field.Value.emit(): String
}

interface TypeDefinitionEmitterLogger {
    fun Type.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type", block)

    fun Type.Name.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Name", block)

    fun Type.Shape.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape", block)

    fun Type.Shape.Field.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape Field", block)

    fun Type.Shape.Field.Key.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape Field Key", block)

    fun Type.Shape.Field.Value.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape Field Value", block)
}
