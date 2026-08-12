package community.flock.wirespec.integration.ktor

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class KtorTransportationTest {
    private fun clientCapturing(
        status: HttpStatusCode = HttpStatusCode.OK,
        responseBody: ByteArray = ByteArray(0),
        responseHeaders: io.ktor.http.Headers = headersOf(),
        capture: (HttpRequestData) -> Unit = {},
    ): HttpClient = HttpClient(
        MockEngine { request ->
            capture(request)
            respond(content = responseBody, status = status, headers = responseHeaders)
        },
    )

    @Test
    fun buildsMethodPathQueryHeadersAndBodyFromRawRequest() {
        lateinit var seen: HttpRequestData
        var seenBody: ByteArray? = null
        val client =
            clientCapturing {
                seen = it
                seenBody = runBlocking { (it.body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes() }
            }

        runBlocking {
            KtorTransportation(client).transport(
                Wirespec.RawRequest(
                    method = "POST",
                    path = listOf("agents", "42", "runs"),
                    queries = mapOf("limit" to listOf("10")),
                    headers = mapOf("X-Trace" to listOf("abc")),
                    body = """{"name":"x"}""".toByteArray(),
                ),
            )
        }

        seen.method.value shouldBe "POST"
        seen.url.encodedPath shouldBe "/agents/42/runs"
        seen.url.parameters["limit"] shouldBe "10"
        seen.headers["X-Trace"] shouldBe "abc"
        String(seenBody!!) shouldBe """{"name":"x"}"""
    }

    @Test
    fun mapsStatusHeadersAndBodyBackToRawResponse() {
        val client =
            clientCapturing(
                status = HttpStatusCode.Created,
                responseBody = """{"id":"7"}""".toByteArray(),
                responseHeaders = headersOf(HttpHeaders.ContentType, "application/json"),
            )

        val response =
            runBlocking {
                KtorTransportation(client).transport(
                    Wirespec.RawRequest("GET", listOf("agents"), emptyMap(), emptyMap(), null),
                )
            }

        response.statusCode shouldBe 201
        String(response.body!!) shouldBe """{"id":"7"}"""
        response.headers[HttpHeaders.ContentType]?.first() shouldBe "application/json"
    }

    @Test
    fun emptyResponseBodyBecomesNull() {
        val client = clientCapturing(status = HttpStatusCode.NoContent)

        val response =
            runBlocking {
                KtorTransportation(client).transport(
                    Wirespec.RawRequest("DELETE", listOf("agents", "1"), emptyMap(), emptyMap(), null),
                )
            }

        response.statusCode shouldBe 204
        response.body shouldBe null
    }

    @Test
    fun configureHookRunsOnEveryRequest() {
        lateinit var seen: HttpRequestData
        val client = clientCapturing { seen = it }

        runBlocking {
            KtorTransportation(client) {
                header("Authorization", "Bearer token-123")
            }.transport(Wirespec.RawRequest("GET", listOf("me"), emptyMap(), emptyMap(), null))
        }

        seen.headers["Authorization"] shouldBe "Bearer token-123"
    }
}
