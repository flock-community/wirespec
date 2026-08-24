package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDto;
import community.flock.wirespec.integration.spring.java.generated.model.Error;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetTodos;
public record GetTodosClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements GetTodos.Call {
  @Override
  public java.util.concurrent.CompletableFuture<GetTodos.Response<?>> getTodos(java.util.Optional<Boolean> done) {
    final var request = new GetTodos.Request(done);
    final var rawRequest = GetTodos.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> GetTodos.fromRawResponse(serialization(), rawResponse));
  }
};
