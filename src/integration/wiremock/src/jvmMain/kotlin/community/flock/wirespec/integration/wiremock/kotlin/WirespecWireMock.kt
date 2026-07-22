package community.flock.wirespec.integration.wiremock.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.UrlPattern
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.kotlin.Wirespec
import java.net.URI
import java.net.URLDecoder

/**
 * Start building a WireMock stub for a Wirespec endpoint; the endpoint's method and path template
 * drive the request matcher (path parameters match any non-slash segment). The returned builder
 * accepts only [Wirespec.Response] values of the same endpoint — a different one is a compile error.
 *
 * ```
 * server.stubFor(wirespec(GetTodos.Handler).willReturn(GetTodos.Response200(todos)))
 * ```
 */
fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> wirespec(
    endpoint: Wirespec.Server<Req, Res>,
): WirespecMappingBuilder<Res> = WirespecMappingBuilder(endpoint, requestBuilder(endpoint.method, endpoint.pathTemplate))

class WirespecMappingBuilder<Res : Wirespec.Response<*>> internal constructor(
    private val endpoint: Wirespec.Server<*, Res>,
    private val mapping: MappingBuilder,
) {
    /**
     * Serialize [response] through [serialization] (Jackson-backed by default) and attach it as this
     * stub's response. Returns the underlying [MappingBuilder] so callers can keep chaining WireMock
     * methods (e.g. `.atPriority(...)`, `.inScenario(...)`).
     */
    fun willReturn(
        response: Res,
        serialization: Wirespec.Serialization = defaultSerialization,
    ): MappingBuilder = mapping.willReturn(responseBuilder(endpoint.server(serialization).to(response)))
}

private val defaultSerialization: Wirespec.Serialization by lazy { WirespecSerialization(ObjectMapper()) }

/**
 * A WireMock [MappingBuilder] matching an endpoint's HTTP [method] and [pathTemplate] (path parameters
 * match any non-slash segment). The building block behind [wirespec], reusable directly when a stub
 * needs extra matching (e.g. `.andMatching(...)`) on top of the method/path match.
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

/**
 * Map an incoming WireMock [Request] onto the neutral [Wirespec.RawRequest]. Use it when a stub
 * matches on the request itself (`.andMatching { request -> … request.toRawRequest() … }`) so the
 * matcher can deserialize through a generated endpoint rather than WireMock's own types.
 *
 * Path segments and query values are percent-decoded; a query key without `=` yields an empty value,
 * a repeated key yields all its values in order, and an empty body maps to `null`.
 */
fun Request.toRawRequest(): Wirespec.RawRequest {
    val uri = URI.create(absoluteUrl)
    val segments = uri.rawPath.split("/").filter(String::isNotEmpty).map(::decode)
    val queries = (uri.rawQuery ?: "").split("&").filter(String::isNotEmpty)
        .map { it.split("=", limit = 2) }
        .groupBy({ decode(it[0]) }, { decode(it.getOrElse(1) { "" }) })
    return Wirespec.RawRequest(
        method = method.value(),
        path = segments,
        queries = queries,
        headers = headers.all().associate { it.key() to it.values().toList() },
        body = body?.takeIf(ByteArray::isNotEmpty),
    )
}

private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8)

private val PATH_PARAM_REGEX = Regex("""\{[^/}]+\}""")

internal fun urlPatternFor(pathTemplate: String): UrlPattern = if (PATH_PARAM_REGEX.containsMatchIn(pathTemplate)) {
    val regex = pathTemplate.split(PATH_PARAM_REGEX).joinToString("[^/]+") { Regex.escape(it) }
    WireMock.urlPathMatching(regex)
} else {
    WireMock.urlPathEqualTo(pathTemplate)
}
