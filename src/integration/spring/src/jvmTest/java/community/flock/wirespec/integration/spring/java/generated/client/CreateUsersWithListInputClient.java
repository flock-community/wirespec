package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.User;
import community.flock.wirespec.integration.spring.java.generated.endpoint.CreateUsersWithListInput;
public record CreateUsersWithListInputClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements CreateUsersWithListInput.Call {
  @Override
  public java.util.concurrent.CompletableFuture<CreateUsersWithListInput.Response<?>> createUsersWithListInput(java.util.List<User> body) {
    final var request = new CreateUsersWithListInput.Request(body);
    final var rawRequest = CreateUsersWithListInput.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> CreateUsersWithListInput.fromRawResponse(serialization(), rawResponse));
  }
};
