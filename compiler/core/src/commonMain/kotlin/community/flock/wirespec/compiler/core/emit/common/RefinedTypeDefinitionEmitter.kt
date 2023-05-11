package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.utils.Logger

interface RefinedTypeDefinitionEmitter : RefinedTypeDefinitionEmitterLogger {
    fun Refined.emit(): String

    fun Refined.RName.emit(): String

    fun Refined.Validator.emit(): String
}

interface RefinedTypeDefinitionEmitterLogger {
    fun Refined.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Refined", block)

    fun Refined.RName.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Refined Name", block)

    fun Refined.Validator.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Refined Validator", block)
}
