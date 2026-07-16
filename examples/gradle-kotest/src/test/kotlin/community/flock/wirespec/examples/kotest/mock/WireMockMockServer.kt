package community.flock.wirespec.examples.kotest.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.matching.ValueMatcher
import community.flock.wirespec.integration.kotest.MockServer
import community.flock.wirespec.integration.kotest.MockStub
import community.flock.wirespec.kotlin.Wirespec
import java.net.URI
import java.net.URLDecoder

/**
 * The WireMock-backed [MockServer] the response side of the scenario DSL drives — the mock
 * counterpart to [KafkaChannelTransport][community.flock.wirespec.examples.kotest.KafkaChannelTransport]:
 * it implements the framework-neutral [MockServer] the DSL consumes and carries no wirespec
 * types of its own, translating each [MockStub] into a WireMock stub.
 *
 * A stub matches on the endpoint's HTTP method and path template (path parameters match any
 * non-slash segment), then defers to [MockStub.matches] — the lowered `.mock { req -> … }`
 * predicate — via a WireMock [ValueMatcher]. On a match it replies with the drawn, already
 * serialized [Wirespec.RawResponse].
 *
 * Start it eagerly so its [baseUrl]/[port] is known before the Spring context boots (point the
 * app's outbound client at it, e.g. via `@DynamicPropertySource`), then hand it to
 * `WirespecMockExtension(server, serialization)`. [reset] drops all stubs between tests; [close]
 * stops the server.
 */
class WireMockMockServer private constructor(
    private val server: WireMockServer,
) : MockServer,
    AutoCloseable {

    val baseUrl: String get() = server.baseUrl()

    val port: Int get() = server.port()

    override fun stub(stub: MockStub) {
        val mapping = requestBuilder(stub.method, urlPatternFor(stub.pathTemplate))
            .andMatching(
                ValueMatcher<Request> { request ->
                    if (matchesSafely(stub, request)) MatchResult.exactMatch() else MatchResult.noMatch()
                },
            )
            .willReturn(responseBuilder(stub.response))
        server.stubFor(mapping)
    }

    override fun reset() = server.resetAll()

    override fun close() = server.stop()

    // A request that slips through the method/path matcher but does not belong to this endpoint
    // can fail to deserialize; treat that as "no match" rather than failing the whole request.
    private fun matchesSafely(stub: MockStub, request: Request): Boolean = try {
        stub.matches(request.toRawRequest())
    } catch (_: Throwable) {
        false
    }

    companion object {
        /** Start a WireMock server on [port] (0 selects a free dynamic port). Stop it via [close]. */
        fun start(port: Int = 0): WireMockMockServer {
            val options = WireMockConfiguration.options()
            if (port == 0) options.dynamicPort() else options.port(port)
            return WireMockMockServer(WireMockServer(options).apply { start() })
        }
    }
}

private fun requestBuilder(method: String, url: UrlPattern) = when (method.uppercase()) {
    "GET" -> WireMock.get(url)
    "PUT" -> WireMock.put(url)
    "POST" -> WireMock.post(url)
    "DELETE" -> WireMock.delete(url)
    "PATCH" -> WireMock.patch(url)
    "HEAD" -> WireMock.head(url)
    "OPTIONS" -> WireMock.options(url)
    "TRACE" -> WireMock.trace(url)
    else -> WireMock.any(url)
}

private fun responseBuilder(response: Wirespec.RawResponse): ResponseDefinitionBuilder {
    val builder = WireMock.aResponse().withStatus(response.statusCode)
    response.headers.forEach { (name, values) -> builder.withHeader(name, *values.toTypedArray()) }
    response.body?.let { builder.withBody(it) }
    return builder
}

private val PATH_PARAM_REGEX = Regex("""\{[^/}]+}""")

/** Turn a wirespec path template (`/api/todos/{id}`) into a WireMock URL matcher. */
private fun urlPatternFor(pathTemplate: String): UrlPattern = if (PATH_PARAM_REGEX.containsMatchIn(pathTemplate)) {
    val regex = pathTemplate.split(PATH_PARAM_REGEX).joinToString("[^/]+") { Regex.escape(it) }
    WireMock.urlPathMatching(regex)
} else {
    WireMock.urlPathEqualTo(pathTemplate)
}

/** Map a WireMock [Request] onto the neutral [Wirespec.RawRequest] the typed predicate reads. */
private fun Request.toRawRequest(): Wirespec.RawRequest {
    val uri = URI.create(absoluteUrl)
    val segments = uri.rawPath.split("/").filter(String::isNotEmpty).map(::decode)
    val queries = (uri.rawQuery ?: "").split("&").filter(String::isNotEmpty)
        .map { it.split("=", limit = 2) }
        .groupBy({ decode(it[0]) }, { decode(it.getOrElse(1) { "" }) })
    val headerMap = headers.all().associate { it.key() to it.values().toList() }
    return Wirespec.RawRequest(
        method = method.value(),
        path = segments,
        queries = queries,
        headers = headerMap,
        body = body?.takeIf(ByteArray::isNotEmpty),
    )
}

private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8)
