@file:OptIn(ExperimentalJsExport::class)

import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.lib.WsEndpoint
import community.flock.wirespec.compiler.lib.consume
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.router.MatchResult
import community.flock.wirespec.router.Route
import community.flock.wirespec.router.match

@JsExport
class WsRouter(
    private val routes: Array<WsRoute>
){
    fun match(method: String, path: String) = routes
        .map { it.consume() }
        .match(Endpoint.Method.valueOf(method), path)
        ?.produce()
}

@JsExport
data class WsMatchResult(
    val endpoint: WsEndpoint,
    val params: Map<String, String>,
    val query: Map<String, String>
)

@JsExport
data class WsRoute(
    val endpoint: WsEndpoint,
    val regex: String,
    val params: Array<String>
)

fun Route.produce() =
    WsRoute(
        endpoint = endpoint.produce(),
        regex = regex.pattern,
        params = params.toTypedArray()
    )

fun WsRoute.consume() =
    Route(
        endpoint = endpoint.consume(),
        regex = Regex(regex),
        params = params.toList()
    )

fun MatchResult.produce() =
    WsMatchResult(
        endpoint = endpoint.produce(),
        params = params,
        query = query
    )

fun List<Route>.produce() = WsRouter(
    routes = map { it.produce() }.toTypedArray()
)