package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

import java.util.Optional;

public record Address (
  Optional<String> street,
  Optional<String> city,
  Optional<String> state,
  Optional<String> zip
) {
};
