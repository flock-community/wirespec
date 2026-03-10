package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

import java.util.Optional;

public record ApiResponse (
  Optional<Integer> code,
  Optional<String> type,
  Optional<String> message
) {
};
