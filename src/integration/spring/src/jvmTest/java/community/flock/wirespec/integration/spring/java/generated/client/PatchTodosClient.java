package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDtoPatch;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDto;
import community.flock.wirespec.integration.spring.java.generated.model.Error;
import community.flock.wirespec.integration.spring.java.generated.endpoint.PatchTodos;
public record PatchTodosClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements PatchTodos.Call {
  @Override
  public java.util.concurrent.CompletableFuture<PatchTodos.Response<?>> patchTodos(String id, TodoDtoPatch body) {
    final var request = new PatchTodos.Request(
      id,
      body
    );
    final var rawRequest = PatchTodos.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> PatchTodos.fromRawResponse(serialization(), rawResponse));
  }
};
