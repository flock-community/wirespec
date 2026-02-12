package community.flock.wirespec.integration.spring.java.generated.model;

import community.flock.wirespec.java.Wirespec;

import java.util.List;
import java.util.Optional;

public record Pet (
  Optional<Long> id,
  String name,
  Optional<Category> category,
  List<String> photoUrls,
  Optional<List<Tag>> tags,
  Optional<PetStatus> status
) {
};
