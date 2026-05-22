package community.flock.wirespec.integration.wiremock.kotlin

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import community.flock.wirespec.kotlin.Wirespec

/**
 * Build a WireMock stub for a Wirespec endpoint, with a typed response.
 *
 * Usage:
 * ```
 * wireMockServer.stubFor(
 *     stubFor(GetTodos.Handler, GetTodos.Response200(listOf(TodoDto("hi"))), serialization)
 * )
 * ```
 *
 * The returned [MappingBuilder] matches the endpoint's HTTP method and path template
 * (path parameters are matched as any non-slash segment) and replies with the response
 * serialized through the supplied [Wirespec.Serialization].
 */
fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> stubFor(
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
