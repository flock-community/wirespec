package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.PetStatus;
public interface PetStatusGenerator {
  public static PetStatus generate(Wirespec.Generator generator, java.util.List<String> path) {
    return PetStatus.valueOf(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldEnum(
      java.util.List.of("available", "pending", "sold"),
      java.util.List.of(java.util.Map.ofEntries(java.util.Map.entry("name", "Description"), java.util.Map.entry("parameters", java.util.Map.ofEntries(java.util.Map.entry("default", "pet status in the store"))))),
      PetStatus.class
    )));
  }
}
