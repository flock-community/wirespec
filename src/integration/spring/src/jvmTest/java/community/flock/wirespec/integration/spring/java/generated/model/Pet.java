package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

public record Pet (
  java.util.Optional<Long> id,
  String name,
  java.util.Optional<Category> category,
  java.util.List<String> photoUrls,
  java.util.Optional<java.util.List<Tag>> tags,
  java.util.Optional<PetStatus> status
) {
};
