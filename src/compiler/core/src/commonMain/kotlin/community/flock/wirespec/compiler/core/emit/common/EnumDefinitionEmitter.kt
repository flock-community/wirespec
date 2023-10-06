package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.utils.Logger

interface EnumDefinitionEmitter : EnumDefinitionEmitterLogger {
    fun Enum.emit(): String
}

interface EnumDefinitionEmitterLogger {
    fun Enum.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Endpoint", block)
}
