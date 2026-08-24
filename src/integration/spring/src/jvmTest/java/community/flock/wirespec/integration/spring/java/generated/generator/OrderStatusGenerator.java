package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.OrderStatus;
public interface OrderStatusGenerator {
  public static OrderStatus generate(Wirespec.Generator generator, java.util.List<String> path) {
    return OrderStatus.valueOf(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldEnum(
      java.util.List.of("placed", "approved", "delivered"),
      java.util.List.of(java.util.Map.ofEntries(java.util.Map.entry("name", "Description"), java.util.Map.entry("parameters", java.util.Map.ofEntries(java.util.Map.entry("default", "Order Status"))))),
      OrderStatus.class
    )));
  }
}
