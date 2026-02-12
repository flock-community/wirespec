package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

import java.util.List;
import java.util.Optional;

public record Customer (
  Optional<Long> id,
  Optional<String> username,
  Optional<List<Address>> address
) {
};
