package community.flock.wirespec.integration.spring.kotlin.it.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.integration.spring.kotlin.client.WirespecHttpExchange
import community.flock.wirespec.integration.spring.kotlin.client.WirespecTransportation
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetPetById
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetTodos
import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import community.flock.wirespec.kotlin.Wirespec
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the [WirespecTransportation] that backs the generated `<Endpoint>.Call` clients.
 *
 * Each test drives the transport exactly the way a generated `Client`/`<Endpoint>Client` does — build the
 * `RawRequest` with `toRequest`, hand it to [Wirespec.Transportation.transport], then decode the
 * `RawResponse` with `fromResponse` — but here the transport is the [HttpServiceProxyFactory]-configured
 * implementation under test.
 */
class WirespecTransportationTest {

    private val serialization = WirespecSerialization(jacksonObjectMapper())

    private val transportation: Wirespec.Transportation by lazy {
        val exchange = HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(WebClient.create()))
            .build()
            .createClient(WirespecHttpExchange::class.java)
        WirespecTransportation(exchange, "http://localhost:${wireMockServer.port()}")
    }

    @Test
    fun `path parameters and a body response`() = runBlocking {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/pet/1")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"id":1,"name":"Dog","category":null,"photoUrls":[],"tags":null,"status":null}"""),
            ),
        )

        val response = callGetPetById(petId = 1)

        assertEquals(
            GetPetById.Response200(Pet(id = 1, name = "Dog", category = null, photoUrls = emptyList(), tags = null, status = null)),
            response,
        )
    }

    @Test
    fun `a non-2xx status is surfaced as a response rather than thrown`() = runBlocking {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/pet/2")).willReturn(aResponse().withStatus(400)),
        )

        val response = callGetPetById(petId = 2)

        assertEquals(GetPetById.Response400(Unit), response)
    }

    @Test
    fun `query parameters and response headers`() = runBlocking {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/todos"))
                .withQueryParam("done", equalTo("true"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("total", "0")
                        .withBody("[]"),
                ),
        )

        val response = callGetTodos(done = true)

        assertEquals(GetTodos.Response200(body = emptyList(), total = 0), response)
    }

    // Mirrors the body the IR emitter generates for a `GetPetById.Call` implementation.
    private suspend fun callGetPetById(petId: Long): GetPetById.Response<*> {
        val rawRequest = GetPetById.toRequest(serialization, GetPetById.Request(petId))
        val rawResponse = transportation.transport(rawRequest)
        return GetPetById.fromResponse(serialization, rawResponse)
    }

    // Mirrors the body the IR emitter generates for a `GetTodos.Call` implementation.
    private suspend fun callGetTodos(done: Boolean): GetTodos.Response<*> {
        val rawRequest = GetTodos.toRequest(serialization, GetTodos.Request(done))
        val rawResponse = transportation.transport(rawRequest)
        return GetTodos.fromResponse(serialization, rawResponse)
    }

    companion object {
        private lateinit var wireMockServer: WireMockServer

        @JvmStatic
        @BeforeAll
        fun setup() {
            wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
            wireMockServer.start()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            wireMockServer.stop()
        }
    }
}
