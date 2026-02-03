package community.flock.wirespec.integration.jvm.kotlin

import community.flock.wirespec.kotlin.Wirespec
import kotlinx.coroutines.future.await
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class HttpClientTransportation(
    private val baseUrl: String,
    private val client: HttpClient = HttpClient.newBuilder().build(),
) : Wirespec.Transportation {

    override suspend fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse {
        val pathString = request.path
            .filter { it.isNotBlank() }
            .joinToString("/")
        val base = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val uriString = if (pathString.isBlank()) base else "$base/$pathString"

        val uri = if (request.queries.isNotEmpty()) {
            val queryString = request.queries.entries.joinToString("&") { (key, values) ->
                values.joinToString("&") { value ->
                    "$key=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
                }
            }
            URI.create("$uriString?$queryString")
        } else {
            URI.create(uriString)
        }

        val headersList = request.headers.entries.flatMap { (key, values) ->
            values.filter { it.isNotBlank() }.flatMap { value ->
                listOf(key, value)
            }
        }

        val requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .method(
                request.method.uppercase(),
                request.body?.let { HttpRequest.BodyPublishers.ofByteArray(it) }
                    ?: HttpRequest.BodyPublishers.noBody(),
            )
            .apply {
                if (headersList.isNotEmpty()) {
                    headers(*headersList.toTypedArray())
                }
            }

        return client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
            .await()
            .let { response ->
                Wirespec.RawResponse(
                    statusCode = response.statusCode(),
                    headers = response.headers().map(),
                    body = response.body(),
                )
            }
    }
}
