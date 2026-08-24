package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UpdatePetWithForm;
public record UpdatePetWithFormClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements UpdatePetWithForm.Call {
  @Override
  public java.util.concurrent.CompletableFuture<UpdatePetWithForm.Response<?>> updatePetWithForm(Long petId, java.util.Optional<String> name, java.util.Optional<String> status) {
    final var request = new UpdatePetWithForm.Request(
      petId,
      name,
      status
    );
    final var rawRequest = UpdatePetWithForm.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> UpdatePetWithForm.fromRawResponse(serialization(), rawResponse));
  }
};
