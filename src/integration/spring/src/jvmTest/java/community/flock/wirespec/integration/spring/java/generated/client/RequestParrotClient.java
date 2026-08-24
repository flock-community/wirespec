package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.RequestBodyParrot;
import community.flock.wirespec.integration.spring.java.generated.model.Error;
import community.flock.wirespec.integration.spring.java.generated.endpoint.RequestParrot;
public record RequestParrotClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements RequestParrot.Call {
  @Override
  public java.util.concurrent.CompletableFuture<RequestParrot.Response<?>> requestParrot(java.util.Optional<String> queryParam, java.util.Optional<String> ranDoMQueRY, java.util.Optional<String> xRequestID, java.util.Optional<String> ranDoMHeADer, RequestBodyParrot body) {
    final var request = new RequestParrot.Request(
      queryParam,
      ranDoMQueRY,
      xRequestID,
      ranDoMHeADer,
      body
    );
    final var rawRequest = RequestParrot.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> RequestParrot.fromRawResponse(serialization(), rawResponse));
  }
};
