package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Category;
public interface CategoryGenerator {
  public static Category generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new Category(
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("id")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldInteger64(
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("name")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      ))))
    );
  }
}
