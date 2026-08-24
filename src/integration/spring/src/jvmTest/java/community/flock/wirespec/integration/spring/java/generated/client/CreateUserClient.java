package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.User;
import community.flock.wirespec.integration.spring.java.generated.endpoint.CreateUser;
public record CreateUserClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements CreateUser.Call {
  @Override
  public java.util.concurrent.CompletableFuture<CreateUser.Response<?>> createUser(User body) {
    final var request = new CreateUser.Request(body);
    final var rawRequest = CreateUser.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> CreateUser.fromRawResponse(serialization(), rawResponse));
  }
};
