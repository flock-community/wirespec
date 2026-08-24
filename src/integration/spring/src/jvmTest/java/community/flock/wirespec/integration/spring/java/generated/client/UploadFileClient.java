package community.flock.wirespec.integration.spring.java.generated.client;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.UploadFileRequestBody;
import community.flock.wirespec.integration.spring.java.generated.model.ApiResponse;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UploadFile;
public record UploadFileClient (
  Wirespec.Serialization serialization,
  Wirespec.Transportation transportation
) implements UploadFile.Call {
  @Override
  public java.util.concurrent.CompletableFuture<UploadFile.Response<?>> uploadFile(Long petId, java.util.Optional<String> additionalMetadata, UploadFileRequestBody body) {
    final var request = new UploadFile.Request(
      petId,
      additionalMetadata,
      body
    );
    final var rawRequest = UploadFile.toRawRequest(serialization(), request);
    return transportation().transport(rawRequest).thenApply(rawResponse -> UploadFile.fromRawResponse(serialization(), rawResponse));
  }
};
