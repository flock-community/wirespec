package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.User;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UpdateUser;
public record UpdateUserClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements UpdateUser.Call {
  @Override
  public java.util.concurrent.CompletableFuture<UpdateUser.Response<?>> updateUser(String username, User body) {
    final var request = new UpdateUser.Request(
      username,
      body
    );
    final var rawRequest = UpdateUser.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> UpdateUser.fromRawResponse(serialization(), rawResponse));
  }
};
