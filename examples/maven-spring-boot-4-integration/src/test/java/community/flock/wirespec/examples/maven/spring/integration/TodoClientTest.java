package community.flock.wirespec.examples.maven.spring.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import community.flock.wirespec.examples.maven.spring.integration.client.TodoClient;
import community.flock.wirespec.generated.examples.spring.endpoint.GetTodos;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Exercises the Wirespec {@link TodoClient} (backed by a Spring Boot 4 RestClient HTTP service
 * group) against the application's own controller. The server is started on a fixed port so it
 * matches the {@code spring.http.client.service.group.todo.base-url} used by the client.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TodoClientTest {

    @Autowired
    private TodoClient todoClient;

    @Test
    void getTodosThroughHttpServiceClient() throws Exception {
        GetTodos.Response<?> response =
                todoClient.getTodos(new GetTodos.Request(Optional.empty())).get();

        GetTodos.Response200 ok = assertInstanceOf(GetTodos.Response200.class, response);
        assertEquals(3, ok.body().size());
    }
}
