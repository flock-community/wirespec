package community.flock.wirespec.integration.spring.java.generated.model;
import community.flock.wirespec.java.Wirespec;
public record Pet (
  java.util.Optional<Long> id,
  String name,
  java.util.Optional<Category> category,
  java.util.List<String> photoUrls,
  java.util.Optional<java.util.List<Tag>> tags,
  java.util.Optional<PetStatus> status
) implements Wirespec.Shape {
  @Override
  public java.util.List<String> validate() {
    return java.util.stream.Stream.of(category().map(it -> it.validate().stream().map(e -> "category." + e).toList()).orElse(java.util.List.<String>of()), tags().map(it -> java.util.stream.IntStream.range(0, it.size()).mapToObj(i -> it.get(i).validate().stream().map(e -> "tags[" + i + "]." + e).toList()).flatMap(java.util.Collection::stream).toList()).orElse(java.util.List.<String>of())).flatMap(java.util.Collection::stream).toList();
  }
};
