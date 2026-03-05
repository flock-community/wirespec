package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

import java.util.Optional;

public record TodoDtoPatch (
  Optional<String> name,
  Optional<Boolean> done
) {
};
