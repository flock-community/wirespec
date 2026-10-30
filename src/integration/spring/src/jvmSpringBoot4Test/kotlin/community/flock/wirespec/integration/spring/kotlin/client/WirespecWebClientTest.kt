package community.flock.wirespec.integration.spring.kotlin.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import community.flock.wirespec.integration.jackson.v3.kotlin.WirespecSerialization
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeletePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.RequestParrot
import community.flock.wirespec.integration.spring.kotlin.generated.model.RequestBodyParrot
import kotlinx.coroutines.runBlocking
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The client is compiled against Spring Framework 6, where `HttpHeaders` is a `Map`, and must keep
 * working on Spring Framework 7, where it no longer is.
 */
class WirespecWebClientTest {

    private val server = WireMockServer(options().dynamicPort())

    private val client by lazy {
        WirespecWebClient(
            client = WebClient.create(server.baseUrl()),
            wirespecSerde = WirespecSerialization(jsonMapper { addModule(kotlinModule()) }),
        )
    }

    @BeforeTest
    fun start() = server.start()

    @AfterTest
    fun stop() = server.stop()

    @Test
    fun `reads the status, headers and body of a response`(): Unit = runBlocking {
        server.stubFor(
            post(urlPathEqualTo("/api/parrot")).willReturn(
                okJson("""{"number": 1, "string": "test"}""")
                    .withHeader("X-Request-ID", "request-123")
                    .withHeader("RanDoMHeADer", "header-456"),
            ),
        )

        val response = client.send<RequestParrot.Request, RequestParrot.Response<*>>(
            RequestParrot.Request(
                queryParam = null,
                RanDoMQueRY = null,
                xRequestID = null,
                RanDoMHeADer = null,
                body = RequestBodyParrot(number = 1, string = "test"),
            ),
        )

        assertEquals(
            RequestParrot.Response200(
                xRequestID = "request-123",
                RanDoMHeADer = "header-456",
                queryParamParrot = null,
                RanDoMQueRYParrot = null,
                body = RequestBodyParrot(number = 1, string = "test"),
            ),
            response,
        )
    }

    @Test
    fun `reads a response without a body`(): Unit = runBlocking {
        server.stubFor(delete(urlPathEqualTo("/pet/1")).willReturn(aResponse().withStatus(400)))

        val response = client.send<DeletePet.Request, DeletePet.Response<*>>(DeletePet.Request(petId = 1, api_key = null))

        assertEquals(DeletePet.Response400, response)
    }
}
