package community.flock.wirespec.example.maven.wiremock;

import static community.flock.wirespec.integration.wiremock.java.WirespecWireMock.wirespec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import community.flock.wirespec.example.maven.wiremock.generated.endpoint.GetTodoById;
import community.flock.wirespec.example.maven.wiremock.generated.endpoint.GetTodos;
import community.flock.wirespec.example.maven.wiremock.generated.model.Error;
import community.flock.wirespec.example.maven.wiremock.generated.model.Todo;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates the wiremock integration. The {@code wirespec(...)} factory mirrors WireMock's own
 * {@code get(urlEqualTo(...))} pattern: it takes the generated endpoint's {@code Handler.Handlers} and returns a stub
 * builder driven by the endpoint's path template and HTTP method. {@code willReturn(response)} then turns a typed
 * Wirespec response into the stub's body, status, and headers — using a Jackson-backed serializer by default. Only
 * responses that belong to that endpoint type-check, so a mismatch is caught at compile time.
 */
class TodoStubTest {

    private final HttpClient http = HttpClient.newHttpClient();

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

        server.stubFor(wirespec(new GetTodos.Handler.Handlers()).willReturn(new GetTodos.Response200(todos)));

        var response = get("/api/todos");

        assertEquals(200, response.statusCode());
        assertEquals("[{\"id\":\"1\",\"name\":\"Buy milk\",\"done\":false}]", response.body());
    }

    @Test
    void stubsAPathParamEndpoint() throws Exception {
        var todo = new Todo("abc", "Walk dog", true);

        server.stubFor(wirespec(new GetTodoById.Handler.Handlers()).willReturn(new GetTodoById.Response200(todo)));

        var response = get("/api/todos/abc");

        assertEquals(200, response.statusCode());
        assertEquals("{\"id\":\"abc\",\"name\":\"Walk dog\",\"done\":true}", response.body());
    }

    @Test
    void stubsAnErrorResponse() throws Exception {
        var error = new Error(404L, "not found");

        server.stubFor(wirespec(new GetTodoById.Handler.Handlers()).willReturn(new GetTodoById.Response404(error)));

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
