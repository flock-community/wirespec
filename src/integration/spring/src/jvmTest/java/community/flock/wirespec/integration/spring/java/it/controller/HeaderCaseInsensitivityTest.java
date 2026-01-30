package community.flock.wirespec.integration.spring.java.it.controller;

import community.flock.wirespec.integration.spring.java.application.Application;
import community.flock.wirespec.integration.spring.java.application.WebTestClientConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Test for RFC 7230 compliance - HTTP header names should be case-insensitive
 */
@SpringBootTest(classes = {Application.class})
@Import(WebTestClientConfig.class)
public class HeaderCaseInsensitivityTest {

    @Autowired
    private WebTestClient client;

    @ParameterizedTest
    @CsvSource({
            "X-Request-ID,RandomHeader",
            "x-REquEst-iD,RanDoMHeADer",
            "X-REQUEST-ID,RANDOMHEADER",
            "x-request-id,randomheader"
    })
    public void shouldParseIncomingHeaderCaseInsensitive(String requestIdHeader, String randomHeader) {
        String body = "{\"number\": 1, \"string\": \"test\"}";

        client
                .post()
                .uri("/api/parrot?Query-Param=query-param-value&RanDoMQueRY=random-query-value")
                .header(requestIdHeader, "request-id")
                .header(randomHeader, "random-value")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-REQUEST-ID", "request-id")
                .expectHeader().valueEquals("RANDOMHEADER", "random-value");
    }
}
