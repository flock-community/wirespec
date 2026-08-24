package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Address;
public interface AddressGenerator {
  public static Address generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new Address(
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("street")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("city")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("state")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("zip")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      ))))
    );
  }
}
