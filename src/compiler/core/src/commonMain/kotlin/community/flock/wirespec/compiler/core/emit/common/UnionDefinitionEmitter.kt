package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

interface UnionDefinitionEmitter : UnionDefinitionEmitterLogger {
    fun Union.emit(): String
}

interface UnionDefinitionEmitterLogger {
    fun Union.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Endpoint", block)
}
