package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.endpoint.LoginUser;
public record LoginUserClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements LoginUser.Call {
  @Override
  public java.util.concurrent.CompletableFuture<LoginUser.Response<?>> loginUser(java.util.Optional<String> username, java.util.Optional<String> password) {
    final var request = new LoginUser.Request(
      username,
      password
    );
    final var rawRequest = LoginUser.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> LoginUser.fromRawResponse(serialization(), rawResponse));
  }
};
