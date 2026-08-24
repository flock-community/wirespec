package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.FindPetsByTags;
public record FindPetsByTagsClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements FindPetsByTags.Call {
  @Override
  public java.util.concurrent.CompletableFuture<FindPetsByTags.Response<?>> findPetsByTags(java.util.Optional<java.util.List<String>> tags) {
    final var request = new FindPetsByTags.Request(tags);
    final var rawRequest = FindPetsByTags.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> FindPetsByTags.fromRawResponse(serialization(), rawResponse));
  }
};
