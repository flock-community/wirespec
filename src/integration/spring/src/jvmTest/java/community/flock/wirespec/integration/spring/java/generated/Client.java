package community.flock.wirespec.integration.spring.java.generated;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.RequestBodyParrot;
import community.flock.wirespec.integration.spring.java.generated.model.Error;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDto;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDtoPatch;
import community.flock.wirespec.integration.spring.java.generated.endpoint.RequestParrot;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetTodos;
import community.flock.wirespec.integration.spring.java.generated.endpoint.PatchTodos;
import community.flock.wirespec.integration.spring.java.generated.client.RequestParrotClient;
import community.flock.wirespec.integration.spring.java.generated.client.GetTodosClient;
import community.flock.wirespec.integration.spring.java.generated.client.PatchTodosClient;
public record Client (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements RequestParrot.Call, GetTodos.Call, PatchTodos.Call {
  @Override
  public java.util.concurrent.CompletableFuture<RequestParrot.Response<?>> requestParrot(java.util.Optional<String> queryParam, java.util.Optional<String> ranDoMQueRY, java.util.Optional<String> xRequestID, java.util.Optional<String> ranDoMHeADer, RequestBodyParrot body) {
    return new RequestParrotClient(
      serialization(),
      transportation()
    ).requestParrot(queryParam, ranDoMQueRY, xRequestID, ranDoMHeADer, body);
  }
  @Override
  public java.util.concurrent.CompletableFuture<GetTodos.Response<?>> getTodos(java.util.Optional<Boolean> done) {
    return new GetTodosClient(
      serialization(),
      transportation()
    ).getTodos(done);
  }
  @Override
  public java.util.concurrent.CompletableFuture<PatchTodos.Response<?>> patchTodos(String id, TodoDtoPatch body) {
    return new PatchTodosClient(
      serialization(),
      transportation()
    ).patchTodos(id, body);
  }
};
