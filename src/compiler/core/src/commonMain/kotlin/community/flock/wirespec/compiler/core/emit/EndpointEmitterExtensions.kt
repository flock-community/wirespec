package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference

fun Endpoint.Segment.emit() = when (this) {
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

fun List<Endpoint.Response>.distinctByStatus(): List<Endpoint.Response> = distinctBy { it.status }

fun AST.hasEndpoints() = modules.flatMap { it.statements }.any { it is Endpoint }

val Endpoint.pathParams get() = path.filterIsInstance<Endpoint.Segment.Param>()

fun Endpoint.Request.paramList(endpoint: Endpoint): List<Param> = listOf(
    endpoint.pathParams.map { it.toParam() },
    endpoint.queries.map { it.toParam(Param.ParamType.QUERY) },
    endpoint.headers.map { it.toParam(Param.ParamType.HEADER) },
    listOfNotNull(content?.toParam()),
).flatten()

fun Endpoint.Response.paramList(): List<Param> = listOf(
    headers.map { it.toParam(Param.ParamType.HEADER) },
    listOfNotNull(content?.toParam()),
).flatten()

data class Param(
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

private fun Endpoint.Segment.Param.toParam() = Param(
    type = Param.ParamType.PATH,
    identifier = identifier,
    reference = reference,
)

private fun Field.toParam(type: Param.ParamType) = Param(
    type = type,
    identifier = identifier,
    reference = reference,
)

private fun Endpoint.Content.toParam() = Param(
    type = Param.ParamType.BODY,
    identifier = FieldIdentifier("body"),
    reference = reference,
)
