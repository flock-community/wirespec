package community.flock.wirespec.integration.wiremock.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import community.flock.wirespec.integration.jackson.java.WirespecSerialization;
import community.flock.wirespec.integration.wiremock.java.generated.endpoint.GetTodoById;
import community.flock.wirespec.integration.wiremock.java.generated.endpoint.GetTodos;
import community.flock.wirespec.integration.wiremock.java.generated.model.Error;
import community.flock.wirespec.integration.wiremock.java.generated.model.TodoDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static community.flock.wirespec.integration.wiremock.java.WirespecWireMock.wirespec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StubForTest {

    private WireMockServer server;
    private final HttpClient client = HttpClient.newHttpClient();
    private final WirespecSerialization serialization = new WirespecSerialization(new ObjectMapper());

    @BeforeEach
    public void startServer() {
        server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        server.start();
    }

    @AfterEach
    public void stopServer() {
        server.stop();
    }

    @Test
    public void stubsStaticPathEndpoint() throws Exception {
        List<TodoDto> todos = List.of(new TodoDto("1", "Buy milk", false));

        server.stubFor(wirespec(new GetTodos.Handler.Handlers()).willReturn(new GetTodos.Response200(todos), serialization));

        HttpResponse<String> response = get("/api/todos");

        assertEquals(200, response.statusCode());
        assertEquals("[{\"id\":\"1\",\"name\":\"Buy milk\",\"done\":false}]", response.body());
    }

    @Test
    public void stubsTemplatedPathEndpoint() throws Exception {
        TodoDto todo = new TodoDto("abc", "Walk dog", true);

        server.stubFor(wirespec(new GetTodoById.Handler.Handlers()).willReturn(new GetTodoById.Response200(todo), serialization));

        HttpResponse<String> response = get("/api/todos/abc");

        assertEquals(200, response.statusCode());
        assertEquals("{\"id\":\"abc\",\"name\":\"Walk dog\",\"done\":true}", response.body());
    }

    @Test
    public void stubsNon2xxResponse() throws Exception {
        Error err = new Error(404L, "not found");

        server.stubFor(wirespec(new GetTodoById.Handler.Handlers()).willReturn(new GetTodoById.Response404(err), serialization));

        HttpResponse<String> response = get("/api/todos/anything");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("not found"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(server.baseUrl() + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
