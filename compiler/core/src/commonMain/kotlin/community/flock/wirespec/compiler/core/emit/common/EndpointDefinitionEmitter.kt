package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.utils.Logger

interface EndpointDefinitionEmitter : EndpointDefinitionEmitterLogger {
    fun Endpoint.emit(): String

    fun Endpoint.Method.emit(): String

    fun Endpoint.Segment.emit(): String

    fun Endpoint.Segment.Param.emit(): String

    fun Endpoint.Segment.Literal.emit(): String

    fun Endpoint.Response.emit(): String
}

interface EndpointDefinitionEmitterLogger {
    fun Endpoint.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Endpoint", block)

    fun Endpoint.Method.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Endpoint Method", block)

    fun Endpoint.Segment.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Endpoint Segment", block)

    fun Endpoint.Segment.Param.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Endpoint Segment Param", block)

    fun Endpoint.Segment.Literal.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Endpoint Response Literal", block)

    fun Endpoint.Response.withLogging(logger: Logger, block: () -> String) = logger
        .log("Emitting Definition: Endpoint Response", block)
}
