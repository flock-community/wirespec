package community.flock.wirespec.generated.model;

import community.flock.wirespec.java.Wirespec;

public record Address (
  java.util.Optional<String> street,
  java.util.Optional<String> city,
  java.util.Optional<String> state,
  java.util.Optional<String> zip
) {
};
