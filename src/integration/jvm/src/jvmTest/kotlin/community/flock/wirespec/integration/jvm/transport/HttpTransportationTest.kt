package community.flock.wirespec.integration.jvm.transport

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import community.flock.wirespec.kotlin.Wirespec
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Drives [HttpTransportation] against a throwaway in-process JDK [HttpServer] and asserts on what
 * that server actually received, so the request-building (path/query encoding, Content-Type
 * handling) is exercised end-to-end over a real socket rather than mocked.
 */
class HttpTransportationTest {

    private lateinit var server: HttpServer
    private lateinit var transportation: HttpTransportation

    // The exchange the stub handler saw on the most recent call, for post-hoc assertions.
    private var lastExchange: HttpExchange? = null

    @BeforeTest
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            lastExchange = exchange
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        server.start()
        transportation = HttpTransportation("http://127.0.0.1:${server.address.port}")
    }

    @AfterTest
    fun stop() {
        server.stop(0)
    }

    private fun rawRequest(
        method: String = "GET",
        path: List<String>,
        queries: Map<String, List<String>> = emptyMap(),
        headers: Map<String, List<String>> = emptyMap(),
        body: ByteArray? = null,
    ) = Wirespec.RawRequest(method, path, queries, headers, body)

    @Test
    fun `path segments with URI-illegal characters are percent-encoded`() = runBlocking {
        // A raw space would make URI.create throw before the request is ever sent.
        transportation.transport(rawRequest(path = listOf("todos", "a b#d")))
        assertEquals("/todos/a%20b%23d", lastExchange!!.requestURI.rawPath)
    }

    @Test
    fun `query values are percent-encoded`() = runBlocking {
        transportation.transport(rawRequest(path = listOf("todos"), queries = mapOf("q" to listOf("a b&c"))))
        assertEquals("q=a+b%26c", lastExchange!!.requestURI.rawQuery)
    }

    @Test
    fun `a request-supplied Content-Type is not duplicated by the json default`() = runBlocking {
        transportation.transport(
            rawRequest(
                method = "POST",
                path = listOf("todos"),
                headers = mapOf("Content-Type" to listOf("application/xml")),
                body = "<todo/>".toByteArray(),
            ),
        )
        assertEquals(listOf("application/xml"), lastExchange!!.requestHeaders["Content-Type"])
    }

    @Test
    fun `a body without a Content-Type header defaults to application-json`() = runBlocking {
        transportation.transport(rawRequest(method = "POST", path = listOf("todos"), body = "{}".toByteArray()))
        assertEquals(listOf("application/json"), lastExchange!!.requestHeaders["Content-Type"])
    }
}
