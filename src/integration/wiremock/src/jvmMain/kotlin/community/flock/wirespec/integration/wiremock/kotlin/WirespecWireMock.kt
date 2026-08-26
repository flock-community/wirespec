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
     */
    fun willReturn(
        response: Res,
        serialization: Wirespec.Serialization = defaultSerialization,
    ): MappingBuilder = mapping.willReturn(responseBuilder(endpoint.server(serialization).to(response)))
}

private val defaultSerialization: Wirespec.Serialization by lazy { WirespecSerialization(ObjectMapper()) }

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

fun responseBuilder(rawResponse: Wirespec.RawResponse): ResponseDefinitionBuilder {
    val builder = WireMock.aResponse().withStatus(rawResponse.statusCode)
    rawResponse.headers.forEach { (name, values) ->
        values.forEach { value -> builder.withHeader(name, value) }
    }
    rawResponse.body?.let(builder::withBody)
    return builder
}

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
