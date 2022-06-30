package community.flock.wirespec.compiler.emit.common

import community.flock.wirespec.compiler.parse.Type
import community.flock.wirespec.utils.log

interface TypeDefinitionEmitter : TypeDefinitionEmitterLogger {
    fun Type.emit(): String

    fun Type.Name.emit(): String

    fun Type.Shape.emit(): String

    fun Type.Shape.Key.emit(): String

    fun Type.Shape.Value.emit(): String
}

interface TypeDefinitionEmitterLogger {
    fun Type.withLogging(block: () -> String) = log("Emitting Definition: Type", block)

    fun Type.Name.withLogging(block: () -> String) = log("Emitting Definition: Type Name", block)

    fun Type.Shape.withLogging(block: () -> String) = log("Emitting Definition: Type Shape", block)

    fun Type.Shape.Key.withLogging(block: () -> String) = log("Emitting Definition: Type Shape Key", block)

    fun Type.Shape.Value.withLogging(block: () -> String) = log("Emitting Definition: Type Shape Value", block)
}
