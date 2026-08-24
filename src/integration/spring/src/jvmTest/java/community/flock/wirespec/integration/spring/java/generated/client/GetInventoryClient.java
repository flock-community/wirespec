package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetInventory;
public record GetInventoryClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements GetInventory.Call {
  @Override
  public java.util.concurrent.CompletableFuture<GetInventory.Response<?>> getInventory() {
    final var request = new GetInventory.Request();
    final var rawRequest = GetInventory.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> GetInventory.fromRawResponse(serialization(), rawResponse));
  }
};
