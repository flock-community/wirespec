package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UpdatePet;
public record UpdatePetClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements UpdatePet.Call {
  @Override
  public java.util.concurrent.CompletableFuture<UpdatePet.Response<?>> updatePet(Pet body) {
    final var request = new UpdatePet.Request(body);
    final var rawRequest = UpdatePet.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> UpdatePet.fromRawResponse(serialization(), rawResponse));
  }
};
