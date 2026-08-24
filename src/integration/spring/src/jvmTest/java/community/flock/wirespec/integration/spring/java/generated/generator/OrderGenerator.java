package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Order;
import community.flock.wirespec.integration.spring.java.generated.model.OrderStatus;
public interface OrderGenerator {
  public static Order generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new Order(
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("id")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldInteger64(
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("petId")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldInteger64(
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("quantity")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldInteger32(
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("shipDate")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("status")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldShape<>(
        java.util.Map.ofEntries(java.util.Map.entry("value", java.util.List.of(java.util.Map.ofEntries(java.util.Map.entry("name", "Description"), java.util.Map.entry("parameters", java.util.Map.ofEntries(java.util.Map.entry("default", "Order Status"))))))),
        (p1) -> OrderStatusGenerator.generate(generator, p1),
        OrderStatus.class
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("complete")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldBoolean(java.util.List.<java.util.Map<String, Object>>of()))))
    );
  }
}
