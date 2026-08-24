package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import community.flock.wirespec.integration.spring.java.generated.model.Category;
import community.flock.wirespec.integration.spring.java.generated.model.Tag;
import community.flock.wirespec.integration.spring.java.generated.model.PetStatus;
public interface PetGenerator {
  public static Pet generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new Pet(
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("id")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldInteger64(
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("name")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("category")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldShape<>(
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("name", java.util.List.<java.util.Map<String, Object>>of())),
        (p1) -> CategoryGenerator.generate(generator, p1),
        Category.class
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("photoUrls")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldArray<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("tags")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldArray<>((p1) -> generator.generate(p1, new Wirespec.GeneratorFieldShape<>(
        java.util.Map.ofEntries(java.util.Map.entry("id", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("name", java.util.List.<java.util.Map<String, Object>>of())),
        (p2) -> TagGenerator.generate(generator, p2),
        Tag.class
      )))))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("status")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldShape<>(
        java.util.Map.ofEntries(java.util.Map.entry("value", java.util.List.of(java.util.Map.ofEntries(java.util.Map.entry("name", "Description"), java.util.Map.entry("parameters", java.util.Map.ofEntries(java.util.Map.entry("default", "pet status in the store"))))))),
        (p1) -> PetStatusGenerator.generate(generator, p1),
        PetStatus.class
      ))))
    );
  }
}
