package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint

interface EndpointEmitter {
    fun Endpoint.Segment.emit() =
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "{${identifier.value}}"
        }

    val Endpoint.indexedPathParams
        get() = path.withIndex().mapNotNull { (idx, segment) ->
            when (segment) {
                is Endpoint.Segment.Literal -> null
                is Endpoint.Segment.Param -> IndexedValue(idx, segment)
            }
        }

    fun String.fixStatus(): String = when (this) {
        "default" -> "200"
        else -> this
    }

    fun List<Endpoint.Response>.distinctByStatus(): List<Endpoint.Response> =
        distinctBy { it.status }

    fun AST.hasEndpoints() = modules.flatMap { it.statements }.any { it is Endpoint }
}