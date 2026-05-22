package community.flock.wirespec.example.maven.wiremock;

import static community.flock.wirespec.integration.wiremock.java.WirespecWireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import community.flock.wirespec.example.maven.wiremock.generated.endpoint.GetTodoById;
import community.flock.wirespec.example.maven.wiremock.generated.endpoint.GetTodos;
import community.flock.wirespec.example.maven.wiremock.generated.model.Error;
import community.flock.wirespec.example.maven.wiremock.generated.model.Todo;
import community.flock.wirespec.integration.jackson.java.WirespecSerialization;
import community.flock.wirespec.java.Wirespec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates how the wiremock integration turns a Wirespec endpoint plus a typed Response
 * into a WireMock stub. The endpoint's pathTemplate and method drive the request matcher;
 * the Response is serialized into the stub body via Wirespec.Serialization.
 */
class TodoStubTest {

    private final HttpClient http = HttpClient.newHttpClient();
    private final Wirespec.Serialization serialization = new WirespecSerialization(new ObjectMapper());

    private WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    @Test
    void stubsTheListEndpoint() throws Exception {
        var todos = List.of(new Todo("1", "Buy milk", false));

        stubFor(server, new GetTodos.Handler.Handlers(), new GetTodos.Response200(todos), serialization);

        var response = get("/api/todos");

        assertEquals(200, response.statusCode());
        assertEquals("[{\"id\":\"1\",\"name\":\"Buy milk\",\"done\":false}]", response.body());
    }

    @Test
    void stubsAPathParamEndpoint() throws Exception {
        var todo = new Todo("abc", "Walk dog", true);

        stubFor(server, new GetTodoById.Handler.Handlers(), new GetTodoById.Response200(todo), serialization);

        var response = get("/api/todos/abc");

        assertEquals(200, response.statusCode());
        assertEquals("{\"id\":\"abc\",\"name\":\"Walk dog\",\"done\":true}", response.body());
    }

    @Test
    void stubsAnErrorResponse() throws Exception {
        var error = new Error(404L, "not found");

        stubFor(server, new GetTodoById.Handler.Handlers(), new GetTodoById.Response404(error), serialization);

        var response = get("/api/todos/missing");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("not found"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(server.baseUrl() + path))
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
