package community.flock.wirespec.integration.wiremock.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.kotlin.Wirespec

/**
 * Start building a WireMock stub for a Wirespec endpoint. Mirrors WireMock's own
 * `get(urlEqualTo(...))` / `post(urlEqualTo(...))` factories — the returned builder
 * accepts only [Wirespec.Response] values that belong to the same endpoint:
 *
 * ```
 * server.stubFor(wirespec(GetTodos.Handler).willReturn(GetTodos.Response200(todos)))
 * ```
 *
 * Passing a response from a different endpoint is a compile error.
 *
 * The endpoint's HTTP method and path template drive the WireMock request matcher
 * (path parameters match any non-slash segment).
 */
fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> wirespec(
    endpoint: Wirespec.Server<Req, Res>,
): WirespecMappingBuilder<Res> = WirespecMappingBuilder(endpoint, requestBuilder(endpoint.method, endpoint.pathTemplate))

class WirespecMappingBuilder<Res : Wirespec.Response<*>> internal constructor(
    private val endpoint: Wirespec.Server<*, Res>,
    private val mapping: MappingBuilder,
) {
    /**
     * Serialize [response] through [serialization] and attach it as this stub's response.
     * Defaults to a Jackson-backed [Wirespec.Serialization]; pass your own to customize
     * the ObjectMapper or swap in a different serializer. Returns the underlying
     * [MappingBuilder] so callers can keep chaining WireMock methods (e.g. `.atPriority(...)`,
     * `.inScenario(...)`).
     */
    fun willReturn(
        response: Res,
        serialization: Wirespec.Serialization = defaultSerialization,
    ): MappingBuilder = mapping.willReturn(responseBuilder(endpoint.server(serialization).to(response)))
}

private val defaultSerialization: Wirespec.Serialization by lazy { WirespecSerialization(ObjectMapper()) }

/**
 * A WireMock [MappingBuilder] matching an endpoint's HTTP [method] and [pathTemplate] (path
 * parameters match any non-slash segment). The building block behind [wirespec]; reusable directly
 * when a stub needs extra matching (e.g. `.andMatching(...)` on the deserialized request) on top of
 * the method/path match.
 */
fun requestBuilder(method: String, pathTemplate: String): MappingBuilder {
    val urlPattern = urlPatternFor(pathTemplate)
    return when (method.uppercase()) {
        "GET" -> WireMock.get(urlPattern)
        "PUT" -> WireMock.put(urlPattern)
        "POST" -> WireMock.post(urlPattern)
        "DELETE" -> WireMock.delete(urlPattern)
        "PATCH" -> WireMock.patch(urlPattern)
        "HEAD" -> WireMock.head(urlPattern)
        "OPTIONS" -> WireMock.options(urlPattern)
        "TRACE" -> WireMock.trace(urlPattern)
        else -> WireMock.any(urlPattern)
    }
}

/** A WireMock [ResponseDefinitionBuilder] mirroring an already-serialized Wirespec [rawResponse]. */
fun responseBuilder(rawResponse: Wirespec.RawResponse): ResponseDefinitionBuilder {
    val builder = WireMock.aResponse().withStatus(rawResponse.statusCode)
    rawResponse.headers.forEach { (name, values) ->
        values.forEach { value -> builder.withHeader(name, value) }
    }
    rawResponse.body?.let(builder::withBody)
    return builder
}

private val PATH_PARAM_REGEX = Regex("""\{[^/}]+\}""")

internal fun urlPatternFor(pathTemplate: String): UrlPattern = if (PATH_PARAM_REGEX.containsMatchIn(pathTemplate)) {
    val regex = pathTemplate.split(PATH_PARAM_REGEX).joinToString("[^/]+") { Regex.escape(it) }
    WireMock.urlPathMatching(regex)
} else {
    WireMock.urlPathEqualTo(pathTemplate)
}
