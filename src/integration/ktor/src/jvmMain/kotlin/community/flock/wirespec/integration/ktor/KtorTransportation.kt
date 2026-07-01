package community.flock.wirespec.integration.ktor

import community.flock.wirespec.kotlin.Wirespec
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent

/**
 * A [Wirespec.Transportation] backed by a Ktor [HttpClient], turning the raw request the
 * wirespec DSL produces into a Ktor call and the Ktor response back into a [Wirespec.RawResponse].
 *
 * The wirespec DSL has already serialized the body, so it is sent as raw
 * [ByteArrayContent] — this bypasses the client's `ContentNegotiation` plugin, which would
 * otherwise try to re-serialize an already-encoded body.
 *
 * [configure] runs last on every request, after method/url/headers/body are set, so a caller can
 * inject cross-cutting concerns such as authentication (a bearer token, basic auth) per call. Wire
 * it to a coroutine-local (e.g.
 * [WirespecRequestScope][community.flock.wirespec.integration.kotest.WirespecRequestScope]) to
 * switch identity per block:
 *
 * ```
 * KtorTransportation(client) { bearerAuth(scope.current()?.token ?: error("no identity")) }
 * ```
 *
 * @param client the Ktor client to send through — a real client or a test-application client.
 * @param configure applied to every [HttpRequestBuilder] after the request is built.
 */
class KtorTransportation(
    private val client: HttpClient,
    private val configure: HttpRequestBuilder.() -> Unit = {},
) : Wirespec.Transportation {
    override suspend fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse {
        val response =
            client.request("/" + request.path.joinToString("/")) {
                method = HttpMethod.parse(request.method)
                url {
                    request.queries.forEach { (key, values) ->
                        values.forEach { parameters.append(key, it) }
                    }
                }
                request.headers.forEach { (key, values) ->
                    values.forEach { header(key, it) }
                }
                request.body?.let {
                    setBody(ByteArrayContent(it, ContentType.Application.Json))
                }
                configure()
            }
        return Wirespec.RawResponse(
            statusCode = response.status.value,
            headers = response.headers.entries().associate { it.key to it.value },
            body = response.readRawBytes().takeIf { it.isNotEmpty() },
        )
    }
}
