package community.flock.wirespec.integration.wiremock.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import community.flock.wirespec.integration.jackson.kotlin.WirespecSerialization
import community.flock.wirespec.integration.wiremock.kotlin.generated.endpoint.GetTodoById
import community.flock.wirespec.integration.wiremock.kotlin.generated.endpoint.GetTodos
import community.flock.wirespec.integration.wiremock.kotlin.generated.model.Error
import community.flock.wirespec.integration.wiremock.kotlin.generated.model.TodoDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StubForTest {

    private lateinit var server: WireMockServer
    private val client = HttpClient.newHttpClient()
    private val serialization = WirespecSerialization(ObjectMapper())

    @BeforeEach
    fun startServer() {
        server = WireMockServer(wireMockConfig().dynamicPort())
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.stop()
    }

    @Test
    fun `stubs a static-path endpoint with the typed response`() {
        val todos = listOf(TodoDto(id = "1", name = "Buy milk", done = false))

        server.stubFor(stubFor(GetTodos.Handler, GetTodos.Response200(todos), serialization))

        val response = get("/api/todos")

        assertEquals(200, response.statusCode())
        assertEquals("""[{"id":"1","name":"Buy milk","done":false}]""", response.body())
    }

    @Test
    fun `stubs a templated-path endpoint with the typed response`() {
        val todo = TodoDto(id = "abc", name = "Walk dog", done = true)

        server.stubFor(stubFor(GetTodoById.Handler, GetTodoById.Response200(todo), serialization))

        val response = get("/api/todos/abc")

        assertEquals(200, response.statusCode())
        assertEquals("""{"id":"abc","name":"Walk dog","done":true}""", response.body())
    }

    @Test
    fun `stubs a non-2xx typed response with the correct status code`() {
        val err = Error(code = 404, description = "not found")

        server.stubFor(stubFor(GetTodoById.Handler, GetTodoById.Response404(err), serialization))

        val response = get("/api/todos/anything")

        assertEquals(404, response.statusCode())
        assertTrue(response.body().contains("not found"))
    }

    @Test
    fun `urlPatternFor turns wirespec path params into non-slash matchers`() {
        val pattern = urlPatternFor("/api/todos/{id}")
        assertTrue(pattern.match("/api/todos/abc").isExactMatch)
        assertTrue(pattern.match("/api/todos/123-xyz").isExactMatch)
        assertTrue(!pattern.match("/api/todos/abc/extra").isExactMatch)
        assertTrue(!pattern.match("/api/users/abc").isExactMatch)
    }

    private fun get(path: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder().uri(URI.create("${server.baseUrl()}$path")).GET().build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
