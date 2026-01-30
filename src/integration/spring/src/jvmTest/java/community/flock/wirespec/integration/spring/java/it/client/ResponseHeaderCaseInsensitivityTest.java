package community.flock.wirespec.integration.spring.java.it.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import community.flock.wirespec.integration.jackson.java.WirespecSerialization;
import community.flock.wirespec.integration.spring.java.application.Application;
import community.flock.wirespec.integration.spring.java.client.WirespecWebClient;
import community.flock.wirespec.integration.spring.java.generated.endpoint.RequestParrot;
import community.flock.wirespec.integration.spring.java.generated.model.RequestBodyParrot;
import community.flock.wirespec.java.Wirespec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test to verify that response headers are processed case-insensitively by the WebClient.
 * Per RFC 7230, HTTP header names are case-insensitive, so the client should be able to
 * read headers regardless of the case used by the server.
 * <p>
 * Additionally verifies that query parameters echoed in response headers maintain their
 * case-sensitivity (query param names are case-sensitive).
 */
@SpringBootTest(classes = {Application.class})
@Import(ResponseHeaderCaseInsensitivityTest.TestConfig.class)
public class ResponseHeaderCaseInsensitivityTest {

    @Autowired
    private WirespecWebClient wirespecWebClient;

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
    }

    @AfterAll
    public static void teardown() {
        wireMockServer.stop();
    }

    @ParameterizedTest
    @CsvSource({
            "x-request-id,randomheader,query-param-parrot,randomqueryparrot",
            "X-REQUEST-ID,RANDOMHEADER,query-param-parrot,randomqueryparrot",
            "X-Request-ID,RandomHeader,QUERY-PARAM-PARROT,randomqueryparrot",
            "x-REquEst-iD,RanDoMHeADer,query-param-parrot,RANDOMQUERYPARROT"
    })
    public void shouldParseResponseHeadersCaseInsensitively(
            String requestIdHeader,
            String randomHeader,
            String queryParamParrotHeader,
            String randomQueryParrotHeader
    ) throws ExecutionException, InterruptedException {
        String responseBody = "{\"number\": 1, \"string\": \"test\"}";

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
        );

        RequestParrot.Request request = new RequestParrot.Request(
                Optional.of("test-query"),
                Optional.of("test-random"),
                Optional.of("request-123"),
                Optional.of("header-456"),
                new RequestBodyParrot(1L, "test")
        );

        Wirespec.Response<?> response = wirespecWebClient.send(request).get();

        // Verify the response is successful
        assertNotNull(response);
        assertEquals(200, response.status());

        // Verify headers are accessible regardless of the case they were sent in
        RequestParrot.Response200 response200 = (RequestParrot.Response200) response;
        assertEquals(Optional.of("request-123"), response200.headers().XRequestID());
        assertEquals(Optional.of("header-456"), response200.headers().RanDoMHeADer());
        assertEquals(Optional.of("test-query"), response200.headers().QueryParamParrot());
        assertEquals(Optional.of("test-random"), response200.headers().RanDoMQueRYParrot());
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean
        @Primary
        public WirespecWebClient wirespecWebClientTest() {
            WebClient client = WebClient.builder()
                    .baseUrl("http://localhost:8089")
                    .build();
            return new WirespecWebClient(client, new WirespecSerialization(new ObjectMapper()));
        }
    }
}
