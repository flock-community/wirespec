package community.flock.wirespec.example.jsonrpc

import community.flock.wirespec.kotlin.Wirespec
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.util.toMap

/**
 * Sends the JSON-RPC requests of the generated client to the [url] of a server with ktor. The client posts every request
 * to the empty path, so the transportation decides where it goes.
 */
class KtorTransportation(private val client: HttpClient, private val url: String) : Wirespec.Transportation {

    override suspend fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse = client
        .request(url) {
            method = HttpMethod.parse(request.method)
            // ktor sets the content type from the body, and refuses it as a header.
            request.headers.filterKeys { !it.equals(HttpHeaders.ContentType, ignoreCase = true) }
                .forEach { (name, values) -> headers.appendAll(name, values) }
            request.body?.let {
                contentType(ContentType.Application.Json)
                setBody(it)
            }
        }
        .let { response ->
            Wirespec.RawResponse(
                statusCode = response.status.value,
                headers = response.headers.toMap(),
                body = response.readBytes().takeIf { it.isNotEmpty() },
            )
        }
}
