package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.endpoint.LogoutUser;
public record LogoutUserClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements LogoutUser.Call {
  @Override
  public java.util.concurrent.CompletableFuture<LogoutUser.Response<?>> logoutUser() {
    final var request = new LogoutUser.Request();
    final var rawRequest = LogoutUser.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> LogoutUser.fromRawResponse(serialization(), rawResponse));
  }
};
