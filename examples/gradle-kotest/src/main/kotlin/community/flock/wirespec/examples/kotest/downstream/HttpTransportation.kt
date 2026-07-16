package community.flock.wirespec.examples.kotest.downstream

import community.flock.wirespec.kotlin.Wirespec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * A JDK-`HttpClient`-backed [Wirespec.Transportation] the app uses to call a downstream service:
 * it turns a generated client's [Wirespec.RawRequest] into a real HTTP call against [baseUrl] and
 * maps the reply back to a [Wirespec.RawResponse]. Dependency-free — only `java.net.http`.
 *
 * @param baseUrl scheme + host + optional port, e.g. `http://localhost:8080` (no trailing slash).
 */
class HttpTransportation(
    private val baseUrl: String,
) : Wirespec.Transportation {

    private val client: HttpClient = HttpClient.newHttpClient()

    override suspend fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse = withContext(Dispatchers.IO) {
        val path = request.path.joinToString("/", prefix = "/")
        val query = request.queries.entries
            .flatMap { (key, values) -> values.map { key to it } }
            .joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        val uri = URI.create(baseUrl + path + if (query.isEmpty()) "" else "?$query")

        val bodyPublisher = request.body
            ?.let { HttpRequest.BodyPublishers.ofByteArray(it) }
            ?: HttpRequest.BodyPublishers.noBody()

        val builder = HttpRequest.newBuilder(uri).method(request.method, bodyPublisher)
        request.headers.forEach { (key, values) -> values.forEach { builder.header(key, it) } }
        if (request.body != null) builder.header("Content-Type", "application/json")

        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
        Wirespec.RawResponse(
            statusCode = response.statusCode(),
            headers = response.headers().map().mapValues { (_, v) -> v.toList() },
            body = response.body()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
