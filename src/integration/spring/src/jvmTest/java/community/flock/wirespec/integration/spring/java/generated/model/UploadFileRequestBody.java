package community.flock.wirespec.integration.spring.java.generated.model;
import community.flock.wirespec.java.Wirespec;
public record UploadFileRequestBody (
  java.util.Optional<String> additionalMetadata,
  java.util.Optional<byte[]> file,
  java.util.Optional<UploadFileRequestBodyJson> json
) implements Wirespec.Shape {
  @Override
  public java.util.List<String> validate() {
    return json().map(it -> it.validate().stream().map(e -> "json." + e).toList()).orElse(java.util.List.<String>of());
  }
};
