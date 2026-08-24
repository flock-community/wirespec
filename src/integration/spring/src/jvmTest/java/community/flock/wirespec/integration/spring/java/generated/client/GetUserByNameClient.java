package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.User;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetUserByName;
public record GetUserByNameClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements GetUserByName.Call {
  @Override
  public java.util.concurrent.CompletableFuture<GetUserByName.Response<?>> getUserByName(String username) {
    final var request = new GetUserByName.Request(username);
    final var rawRequest = GetUserByName.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> GetUserByName.fromRawResponse(serialization(), rawResponse));
  }
};
