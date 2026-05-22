package community.flock.wirespec.integration.wiremock.kotlin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import community.flock.wirespec.kotlin.Wirespec

/**
 * Register a WireMock stub for a Wirespec endpoint, with a typed response.
 *
 * Usage:
 * ```
 * wireMockServer.stubFor(GetTodos.Handler, GetTodos.Response200(listOf(TodoDto("hi"))), serialization)
 * ```
 *
 * The endpoint's method and path template drive the WireMock request matcher (path
 * parameters match any non-slash segment), and the response is serialized through
 * the supplied [Wirespec.Serialization] into the stub's body, status, and headers.
 */
fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> WireMockServer.stubFor(
    endpoint: Wirespec.Server<Req, Res>,
    response: Res,
    serialization: Wirespec.Serialization,
): StubMapping = stubFor(mappingBuilder(endpoint, response, serialization))

internal fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> mappingBuilder(
    endpoint: Wirespec.Server<Req, Res>,
    response: Res,
    serialization: Wirespec.Serialization,
): MappingBuilder {
    val rawResponse = endpoint.server(serialization).to(response)
    val urlPattern = urlPatternFor(endpoint.pathTemplate)
    return requestBuilder(endpoint.method, urlPattern)
        .willReturn(responseBuilder(rawResponse))
}

private fun requestBuilder(method: String, urlPattern: UrlPattern): MappingBuilder = when (method.uppercase()) {
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

private fun responseBuilder(rawResponse: Wirespec.RawResponse): ResponseDefinitionBuilder {
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
