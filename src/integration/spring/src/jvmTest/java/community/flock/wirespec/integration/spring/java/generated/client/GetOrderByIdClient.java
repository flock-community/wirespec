package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Order;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetOrderById;
public record GetOrderByIdClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements GetOrderById.Call {
  @Override
  public java.util.concurrent.CompletableFuture<GetOrderById.Response<?>> getOrderById(Long orderId) {
    final var request = new GetOrderById.Request(orderId);
    final var rawRequest = GetOrderById.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> GetOrderById.fromRawResponse(serialization(), rawResponse));
  }
};
