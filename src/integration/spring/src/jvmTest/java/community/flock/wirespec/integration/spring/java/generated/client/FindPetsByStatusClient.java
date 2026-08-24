package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.FindPetsByStatusParameterStatus;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.FindPetsByStatus;
public record FindPetsByStatusClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements FindPetsByStatus.Call {
  @Override
  public java.util.concurrent.CompletableFuture<FindPetsByStatus.Response<?>> findPetsByStatus(java.util.Optional<FindPetsByStatusParameterStatus> status) {
    final var request = new FindPetsByStatus.Request(status);
    final var rawRequest = FindPetsByStatus.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> FindPetsByStatus.fromRawResponse(serialization(), rawResponse));
  }
};
