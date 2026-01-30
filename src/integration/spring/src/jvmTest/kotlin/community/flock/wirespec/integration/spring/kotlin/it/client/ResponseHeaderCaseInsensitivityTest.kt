package community.flock.wirespec.integration.spring.kotlin.it.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import community.flock.wirespec.integration.jackson.kotlin.WirespecSerialization
import community.flock.wirespec.integration.spring.java.application.Application
import community.flock.wirespec.integration.spring.kotlin.client.WirespecWebClient
import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecSerializationConfiguration.Companion.objectMapper
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.RequestParrot
import community.flock.wirespec.integration.spring.kotlin.generated.model.RequestBodyParrot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.builder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test to verify that response headers are processed case-insensitively by the WebClient.
 * Per RFC 7230, HTTP header names are case-insensitive, so the client should be able to
 * read headers regardless of the case used by the server.
 *
 * Additionally verifies that query parameters echoed in response headers maintain their
 * case-sensitivity (query param names are case-sensitive).
 */
@SpringBootTest(classes = [Application::class])
@Import(TestConfig::class)
class ResponseHeaderCaseInsensitivityTest {

    @Autowired
    lateinit var wirespecWebClient: WirespecWebClient

    @ParameterizedTest
    @CsvSource(
        "x-request-id,randomheader,query-param-parrot,randomqueryparrot",
        "X-REQUEST-ID,RANDOMHEADER,query-param-parrot,randomqueryparrot",
        "X-Request-ID,RandomHeader,QUERY-PARAM-PARROT,randomqueryparrot",
        "x-REquEst-iD,RanDoMHeADer,query-param-parrot,RANDOMQUERYPARROT",
    )
    fun `Response headers should be parsed case-insensitively`(
        requestIdHeader: String,
        randomHeader: String,
        queryParamParrotHeader: String,
        randomQueryParrotHeader: String,
    ) = runBlocking {
        val responseBody = """{"number": 1, "string": "test"}"""

        // Set up WireMock to return headers in various cases
        wireMockServer.stubFor(
            post(urlPathEqualTo("/api/parrot"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(requestIdHeader, "{{request.headers.X-Request-ID}}")
                        .withHeader(randomHeader, "{{request.headers.RanDoMHeADer}}")
                        .withHeader(queryParamParrotHeader, "{{request.query.Query-Param}}")
                        .withHeader(randomQueryParrotHeader, "{{request.query.RanDoMQueRY}}")
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                        .withTransformers("response-template")
                )
        )

        val request = RequestParrot.Request(
            QueryParam = "test-query",
            RanDoMQueRY = "test-random",
            XRequestID = "request-123",
            RanDoMHeADer = "header-456",
            body = RequestBodyParrot(number = 1, string = "test")
        )

        val response = wirespecWebClient.send<RequestParrot.Request, RequestParrot.Response<*>>(request)
        // Verify the response is successful
        assertNotNull(response)
        assertEquals(200, response.status)

        // Verify headers are accessible regardless of the case they were sent in
        val response200 = response as RequestParrot.Response200
        assertAll({
            assertEquals("request-123", response200.headers.XRequestID)
            assertEquals("header-456", response200.headers.RanDoMHeADer)
            assertEquals("test-query", response200.headers.QueryParamParrot)
            assertEquals("test-random", response200.headers.RanDoMQueRYParrot)
        })
    }

    companion object {
        private lateinit var wireMockServer: WireMockServer

        @JvmStatic
        @BeforeAll
        fun setup() {
            wireMockServer = WireMockServer(WireMockConfiguration.options().port(8089))
            wireMockServer.start()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            wireMockServer.stop()
        }
    }
}

@TestConfiguration
open class TestConfig(
) {

    @Bean
    @Primary
    open fun wirespecWebClientTest(
        webClient: WebClient,
    ): WirespecWebClient {
        val client = builder()
            .baseUrl("http://localhost:8089")
            .build()
        return WirespecWebClient(client, WirespecSerialization(objectMapper))
    }
}
