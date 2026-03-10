package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

import java.util.Optional;

public record Order (
  Optional<Long> id,
  Optional<Long> petId,
  Optional<Integer> quantity,
  Optional<String> shipDate,
  Optional<OrderStatus> status,
  Optional<Boolean> complete
) {
};
