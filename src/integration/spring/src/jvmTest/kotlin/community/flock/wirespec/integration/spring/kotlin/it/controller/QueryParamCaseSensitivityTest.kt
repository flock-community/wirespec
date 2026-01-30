package community.flock.wirespec.integration.spring.kotlin.it.controller

import community.flock.wirespec.integration.spring.kotlin.application.Application
import community.flock.wirespec.integration.spring.kotlin.application.WebTestClientConfig
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Test to verify that query parameters are case-sensitive in Spring.
 * Unlike HTTP headers (which are case-insensitive per RFC 7230), query parameters
 * must match the exact case defined in the endpoint specification.
 */
@SpringBootTest(classes = [Application::class])
@Import(WebTestClientConfig::class)
class QueryParamCaseSensitivityTest {

    @Autowired
    lateinit var client: WebTestClient

    @ParameterizedTest
    @CsvSource(
        "Query-Param,RanDoMQueRY,true",
        "query-param,randomquery,false",
        "QUERY-PARAM,RANDOMQUERY,false",
        "Query-PARAM,RaNdOmQuErY,false",
    )
    fun `Query parameters should be handled case-sensitively`(
        queryParam: String,
        randomQuery: String,
        shouldSucceed: Boolean,
    ) {
        val body = """{"number": 1, "string": "test"}"""

        val result =
            client
                .post()
                .uri("/api/parrot?$queryParam=query-value&$randomQuery=random-value")
                .header("X-Request-ID", "request-id")
                .header("RandomHeader", "header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()

        result.expectStatus().isOk

        if (shouldSucceed) {
            result
                .expectHeader().valueEquals("query-param-parrot", "query-value")
                .expectHeader().valueEquals("randomqueryparrot", "random-value")
        } else {
            result
                .expectHeader().doesNotExist("query-param-parrot")
                .expectHeader().doesNotExist("randomqueryparrot")
        }
    }
}
