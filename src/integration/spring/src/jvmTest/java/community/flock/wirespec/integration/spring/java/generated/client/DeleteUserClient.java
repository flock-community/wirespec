package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.endpoint.DeleteUser;
public record DeleteUserClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements DeleteUser.Call {
  @Override
  public java.util.concurrent.CompletableFuture<DeleteUser.Response<?>> deleteUser(String username) {
    final var request = new DeleteUser.Request(username);
    final var rawRequest = DeleteUser.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> DeleteUser.fromRawResponse(serialization(), rawResponse));
  }
};
