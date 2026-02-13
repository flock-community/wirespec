package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

import java.util.Optional;

public record UploadFileRequestBody (
  Optional<String> additionalMetadata,
  Optional<byte[]> file,
  Optional<UploadFileRequestBodyJson> json
) {
};
