package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record Order (
  java.util.Optional<Long> id,
  java.util.Optional<Long> petId,
  java.util.Optional<Integer> quantity,
  java.util.Optional<String> shipDate,
  java.util.Optional<OrderStatus> status,
  java.util.Optional<Boolean> complete
) {
};
