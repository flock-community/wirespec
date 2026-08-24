package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Error;
public interface ErrorGenerator {
  public static Error generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new Error(
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("code")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldInteger64(
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("description")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      ))
    );
  }
}
