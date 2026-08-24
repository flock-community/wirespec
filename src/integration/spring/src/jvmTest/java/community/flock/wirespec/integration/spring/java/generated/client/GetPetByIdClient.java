package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetPetById;
public record GetPetByIdClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements GetPetById.Call {
  @Override
  public java.util.concurrent.CompletableFuture<GetPetById.Response<?>> getPetById(Long petId) {
    final var request = new GetPetById.Request(petId);
    final var rawRequest = GetPetById.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> GetPetById.fromRawResponse(serialization(), rawResponse));
  }
};
