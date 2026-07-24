package community.flock.wirespec.examples.maven.spring.integration.client;

import community.flock.wirespec.generated.examples.spring.endpoint.GetTodos;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * A hand-written client that implements the Wirespec-generated {@code Handler} interfaces and
 * delegates every call to the shared {@link WirespecRestClient}.
 *
 * <p>The generated {@code Handler} methods return a {@link CompletableFuture}, so the blocking
 * {@code RestClient} result is wrapped with {@link CompletableFuture#completedFuture}.
 */
@Component
public class TodoClient implements GetTodos.Handler {

    private final WirespecRestClient wirespecRestClient;

    public TodoClient(WirespecRestClient wirespecRestClient) {
        this.wirespecRestClient = wirespecRestClient;
    }

    @Override
    public CompletableFuture<GetTodos.Response<?>> getTodos(GetTodos.Request request) {
        return CompletableFuture.completedFuture(wirespecRestClient.send(request));
    }
}
