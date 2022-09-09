package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger

interface TypeDefinitionEmitter : TypeDefinitionEmitterLogger {
    fun Type.emit(): String

    fun Type.Name.emit(): String

    fun Type.Shape.emit(): String

    fun Type.Shape.Key.emit(nullable: Boolean = false): String

    fun Type.Shape.Value.emit(): String
}

interface TypeDefinitionEmitterLogger {
    fun Type.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type", block)

    fun Type.Name.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Name", block)

    fun Type.Shape.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape", block)

    fun Type.Shape.Key.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape Key", block)

    fun Type.Shape.Value.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Type Shape Value", block)
}
