package community.flock.wirespec.compiler.emit.common

import community.flock.wirespec.compiler.parse.Definition.TypeDefinition
import community.flock.wirespec.compiler.utils.log

interface TypeDefinitionEmitter : TypeDefinitionEmitterLogger {
    fun TypeDefinition.emit(): String

    fun TypeDefinition.Name.emit(): String

    fun TypeDefinition.Shape.emit(): String

    fun TypeDefinition.Shape.Key.emit(): String

    fun TypeDefinition.Shape.Value.emit(): String
}

interface TypeDefinitionEmitterLogger {
    fun TypeDefinition.withLogging(block: () -> String) = log("Emitting TypeDefinition", block)

    fun TypeDefinition.Name.withLogging(block: () -> String) = log("Emitting Type Definition Name", block)

    fun TypeDefinition.Shape.withLogging(block: () -> String) = log("Emitting Type Definition Shape", block)

    fun TypeDefinition.Shape.Key.withLogging(block: () -> String) = log("Emitting Type Definition Shape Key", block)

    fun TypeDefinition.Shape.Value.withLogging(block: () -> String) = log("Emitting Type Definition Shape Value", block)
}
