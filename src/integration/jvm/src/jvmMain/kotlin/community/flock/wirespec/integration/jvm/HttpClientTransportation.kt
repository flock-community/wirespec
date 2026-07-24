package community.flock.wirespec.integration.jvm

import community.flock.wirespec.kotlin.Wirespec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * A [Wirespec.Transportation] that executes the raw request over real HTTP using the
 * JDK [HttpClient]. A typed client edge turns a generated `Request` into a
 * [Wirespec.RawRequest]; this sends it and maps the HTTP response back into a
 * [Wirespec.RawResponse].
 */
class HttpClientTransportation(
    private val baseUrl: String,
    private val client: HttpClient = HttpClient.newHttpClient(),
) : Wirespec.Transportation {

    override suspend fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse = withContext(Dispatchers.IO) {
        val response = client.send(request.toHttpRequest(), HttpResponse.BodyHandlers.ofByteArray())
        Wirespec.RawResponse(
            statusCode = response.statusCode(),
            headers = response.headers().map().mapValues { (_, v) -> v.toList() },
            body = response.body()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun Wirespec.RawRequest.toHttpRequest(): HttpRequest {
        val path = path.joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8) }
        val query = queries.entries
            .flatMap { (key, values) -> values.map { key to it } }
            .joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, Charsets.UTF_8)}=${URLEncoder.encode(v, Charsets.UTF_8)}"
            }
        val uri = URI.create(
            buildString {
                append(baseUrl).append("/").append(path)
                if (query.isNotEmpty()) append("?").append(query)
            },
        )

        val bodyPublisher = body
            ?.let { HttpRequest.BodyPublishers.ofByteArray(it) }
            ?: HttpRequest.BodyPublishers.noBody()

        val builder = HttpRequest.newBuilder(uri)
            .method(method, bodyPublisher)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        headers.forEach { (name, values) -> values.forEach { builder.header(name, it) } }
        return builder.build()
    }
}
