package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

import java.util.Optional;

public record User (
  Optional<Long> id,
  Optional<String> username,
  Optional<String> firstName,
  Optional<String> lastName,
  Optional<String> email,
  Optional<String> password,
  Optional<String> phone,
  Optional<Integer> userStatus
) {
};
