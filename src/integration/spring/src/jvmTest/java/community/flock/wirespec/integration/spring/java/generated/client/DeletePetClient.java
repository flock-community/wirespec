package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.endpoint.DeletePet;
public record DeletePetClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements DeletePet.Call {
  @Override
  public java.util.concurrent.CompletableFuture<DeletePet.Response<?>> deletePet(Long petId, java.util.Optional<String> api_key) {
    final var request = new DeletePet.Request(
      petId,
      api_key
    );
    final var rawRequest = DeletePet.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> DeletePet.fromRawResponse(serialization(), rawResponse));
  }
};
