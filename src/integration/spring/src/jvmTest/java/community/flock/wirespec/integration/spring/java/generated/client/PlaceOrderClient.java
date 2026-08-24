package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Order;
import community.flock.wirespec.integration.spring.java.generated.endpoint.PlaceOrder;
public record PlaceOrderClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements PlaceOrder.Call {
  @Override
  public java.util.concurrent.CompletableFuture<PlaceOrder.Response<?>> placeOrder(Order body) {
    final var request = new PlaceOrder.Request(body);
    final var rawRequest = PlaceOrder.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> PlaceOrder.fromRawResponse(serialization(), rawResponse));
  }
};
