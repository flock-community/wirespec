package community.flock.wirespec.generator

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Endpoint.Segment

typealias Path = List<Segment>

data class MatchResult(
    val endpoint: Endpoint,
    val params: Map<String, String>,
    val query: Map<String, String>
)

data class Route(
    val endpoint: Endpoint,
    val regex: Regex,
    val params: List<String>
)

fun AST.router() = this
    .filterIsInstance<Endpoint>()
    .map { endpoint ->
        Route(
            endpoint = endpoint,
            regex = endpoint.path.toRegex(),
            params = endpoint.path.filterIsInstance<Segment.Param>().map { it.identifier.value }
        )
    }

fun Route.match(method: Endpoint.Method, path: String): MatchResult? {
    val parts = path.split("?")
    val p = parts[0]
    val q = parts.getOrNull(1)
    val match = regex.find(p)
    println(regex)
    return if (endpoint.method == method && match != null) {
        MatchResult(
            endpoint = endpoint,
            params = params.associateWith { match.groups[it]?.value ?: error("parameter not found in matcher")},
            query = q?.parseQuery().orEmpty()
        )
    } else {
        null
    }
}

fun List<Route>.match(method: Endpoint.Method, path: String): MatchResult? {
    return firstNotNullOfOrNull { it.match(method, path)  }
}

private fun String.parseQuery() = this
    .split("&")
    .map { it.split("=", limit = 2) }
    .associate { (key, value) -> key to value }

fun Path.toRegex() = Regex(this.joinToString("/", "^/", "/*$") {
    when (it) {
        is Segment.Param -> """(?<${it.identifier.value}>[^/]+)"""
        is Segment.Literal -> """(${it.value})"""
    }
})
