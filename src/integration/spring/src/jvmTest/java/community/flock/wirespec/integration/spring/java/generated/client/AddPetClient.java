package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.AddPet;
public record AddPetClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements AddPet.Call {
  @Override
  public java.util.concurrent.CompletableFuture<AddPet.Response<?>> addPet(Pet body) {
    final var request = new AddPet.Request(body);
    final var rawRequest = AddPet.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> AddPet.fromRawResponse(serialization(), rawResponse));
  }
};
