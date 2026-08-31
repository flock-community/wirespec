package community.flock.wirespec.integration.jvm.transport

import community.flock.wirespec.kotlin.Wirespec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

public class HttpTransportation(
    private val baseUrl: String,
) : Wirespec.Transportation {

    private val client: HttpClient = HttpClient.newHttpClient()

    override suspend fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse = withContext(Dispatchers.IO) {
        val path = request.path.joinToString("/", prefix = "/") { encodePathSegment(it) }
        val query = request.queries.entries
            .flatMap { (key, values) -> values.map { key to it } }
            .joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        val uri = URI.create(baseUrl + path + if (query.isEmpty()) "" else "?$query")

        val bodyPublisher = request.body
            ?.let { HttpRequest.BodyPublishers.ofByteArray(it) }
            ?: HttpRequest.BodyPublishers.noBody()

        val builder = HttpRequest.newBuilder(uri).method(request.method, bodyPublisher)
        request.headers.forEach { (key, values) -> values.forEach { builder.header(key, it) } }
        val hasContentType = request.headers.keys.any { it.equals("Content-Type", ignoreCase = true) }
        if (request.body != null && !hasContentType) builder.header("Content-Type", "application/json")

        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
        Wirespec.RawResponse(
            statusCode = response.statusCode(),
            headers = response.headers().map().mapValues { (_, v) -> v.toList() },
            body = response.body()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun encodePathSegment(segment: String): String = encode(segment).replace("+", "%20")
}
