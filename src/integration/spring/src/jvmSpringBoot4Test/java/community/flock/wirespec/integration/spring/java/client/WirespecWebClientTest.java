package community.flock.wirespec.integration.spring.java.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import community.flock.wirespec.integration.jackson.v3.java.WirespecSerialization;
import community.flock.wirespec.integration.spring.java.generated.endpoint.DeletePet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.RequestParrot;
import community.flock.wirespec.integration.spring.java.generated.model.RequestBodyParrot;
import community.flock.wirespec.java.Wirespec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The client is compiled against Spring Framework 6, where {@code HttpHeaders} is a {@code Map}, and
 * must keep working on Spring Framework 7, where it no longer is.
 */
class WirespecWebClientTest {

    private final WireMockServer server = new WireMockServer(options().dynamicPort());

    private WirespecWebClient client;

    @BeforeEach
    void start() {
        server.start();
        client = new WirespecWebClient(
                WebClient.create(server.baseUrl()),
                new WirespecSerialization(JsonMapper.builder().build())
        );
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    @Test
    void readsTheStatusHeadersAndBodyOfAResponse() throws Exception {
        server.stubFor(post(urlPathEqualTo("/api/parrot")).willReturn(
                okJson("{\"number\": 1, \"string\": \"test\"}")
                        .withHeader("X-Request-ID", "request-123")
                        .withHeader("RanDoMHeADer", "header-456")
        ));
        RequestBodyParrot body = new RequestBodyParrot(1L, "test");

        Wirespec.Response<?> response = client
                .send(new RequestParrot.Request(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), body))
                .get(10, TimeUnit.SECONDS);

        assertEquals(
                new RequestParrot.Response200(Optional.of("request-123"), Optional.of("header-456"), Optional.empty(), Optional.empty(), body),
                response
        );
    }

    @Test
    void readsAResponseWithoutABody() throws Exception {
        server.stubFor(delete(urlPathEqualTo("/pet/1")).willReturn(aResponse().withStatus(400)));

        Wirespec.Response<?> response = client
                .send(new DeletePet.Request(1L, Optional.empty()))
                .get(10, TimeUnit.SECONDS);

        assertEquals(new DeletePet.Response400(), response);
    }
}
