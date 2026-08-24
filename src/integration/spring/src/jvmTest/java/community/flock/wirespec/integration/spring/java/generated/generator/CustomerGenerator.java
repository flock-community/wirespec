package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Customer;
import community.flock.wirespec.integration.spring.java.generated.model.Address;
public interface CustomerGenerator {
  public static Customer generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new Customer(
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("id")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldInteger64(
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("username")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("address")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldArray<>((p1) -> generator.generate(p1, new Wirespec.GeneratorFieldShape<>(
        java.util.Map.ofEntries(java.util.Map.entry("street", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("city", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("state", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("zip", java.util.List.<java.util.Map<String, Object>>of())),
        (p2) -> AddressGenerator.generate(generator, p2),
        Address.class
      ))))))
    );
  }
}
