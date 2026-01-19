package community.flock.wirespec.integration.spring.java.generated.model;

public record UploadFileRequestBody (
  java.util.Optional<String> additionalMetadata,
  java.util.Optional<byte[]> file,
  java.util.Optional<UploadFileRequestBodyJson> json
) {
};
