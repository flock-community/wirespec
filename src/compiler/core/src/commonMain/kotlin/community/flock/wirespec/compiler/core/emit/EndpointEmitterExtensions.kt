package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Reference

internal fun Endpoint.Segment.emit(): String = when (this) {
    is Endpoint.Segment.Literal -> value
    is Endpoint.Segment.Param -> "{${identifier.value}}"
}

public fun String.fixStatus(): String = when (this) {
    "default" -> "200"
    else -> this
}

internal data class Param(
    val type: ParamType,
    val identifier: Identifier,
    val reference: Reference,
) {
    enum class ParamType {
        PATH,
        QUERY,
        HEADER,
        BODY,
    }
}
