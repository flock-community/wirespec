package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.endpoint.DeleteOrder;
public record DeleteOrderClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements DeleteOrder.Call {
  @Override
  public java.util.concurrent.CompletableFuture<DeleteOrder.Response<?>> deleteOrder(Long orderId) {
    final var request = new DeleteOrder.Request(orderId);
    final var rawRequest = DeleteOrder.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> DeleteOrder.fromRawResponse(serialization(), rawResponse));
  }
};
