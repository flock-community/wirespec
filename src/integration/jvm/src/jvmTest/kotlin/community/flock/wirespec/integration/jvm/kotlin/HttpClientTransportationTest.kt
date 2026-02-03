package community.flock.wirespec.integration.jvm.kotlin

import community.flock.wirespec.kotlin.Wirespec
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture

class HttpClientTransportationTest {

    @Test
    fun testTransport() {
        runBlocking {
            val mockClient = mockk<HttpClient>()
            val mockResponse = mockk<HttpResponse<ByteArray>>()

            val body = "{\"hello\":\"world\"}"
            coEvery { mockResponse.statusCode() } returns 200
            coEvery { mockResponse.body() } returns body.toByteArray()
            coEvery { mockResponse.headers() } returns HttpHeaders.of(emptyMap()) { _, _ -> true }

            coEvery { mockClient.sendAsync(any<HttpRequest>(), any<HttpResponse.BodyHandler<ByteArray>>()) } returns CompletableFuture.completedFuture(mockResponse)

            val transportation = HttpClientTransportation("https://67.com", mockClient)
            val request = Wirespec.RawRequest(
                method = "POST",
                path = listOf("path", "to", "resource"),
                queries = mapOf("query" to listOf("v1")),
                headers = mapOf("Content-Type" to listOf("application/json")),
                body = body.toByteArray(),
            )

            val response = transportation.transport(request)

            val requestSlot = slot<HttpRequest>()
            verify { mockClient.sendAsync(capture(requestSlot), any<HttpResponse.BodyHandler<ByteArray>>()) }

            val capturedRequest = requestSlot.captured
            assertEquals("https://67.com/path/to/resource?query=v1", capturedRequest.uri().toString())
            assertEquals("POST", capturedRequest.method())
            assertEquals("application/json", capturedRequest.headers().firstValue("Content-Type").orElse(null))

            assertEquals(200, response.statusCode)
            assertArrayEquals(body.toByteArray(), response.body)
        }
    }

    @Test
    fun testTransportWithTrailingSlashAndEmptySegments() {
        runBlocking {
            val mockClient = mockk<HttpClient>()
            val mockResponse = mockk<HttpResponse<ByteArray>>()

            coEvery { mockResponse.statusCode() } returns 200
            coEvery { mockResponse.body() } returns ByteArray(0)
            coEvery { mockResponse.headers() } returns HttpHeaders.of(emptyMap()) { _, _ -> true }

            coEvery { mockClient.sendAsync(any<HttpRequest>(), any<HttpResponse.BodyHandler<ByteArray>>()) } returns CompletableFuture.completedFuture(mockResponse)

            val transportation = HttpClientTransportation("https://67.com/", mockClient)
            val request = Wirespec.RawRequest(
                method = "GET",
                path = listOf("", "path", "", "resource", ""),
                queries = emptyMap(),
                headers = emptyMap(),
                body = null,
            )

            transportation.transport(request)

            val requestSlot = slot<HttpRequest>()
            verify { mockClient.sendAsync(capture(requestSlot), any<HttpResponse.BodyHandler<ByteArray>>()) }

            val capturedRequest = requestSlot.captured
            assertEquals("https://67.com/path/resource", capturedRequest.uri().toString())
        }
    }
}
