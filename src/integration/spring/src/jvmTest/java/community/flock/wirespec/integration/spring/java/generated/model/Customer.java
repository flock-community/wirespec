package community.flock.wirespec.integration.spring.java.generated.model;
import community.flock.wirespec.java.Wirespec;
public record Customer (
  java.util.Optional<Long> id,
  java.util.Optional<String> username,
  java.util.Optional<java.util.List<Address>> address
) implements Wirespec.Shape {
  @Override
  public java.util.List<String> validate() {
    return address().map(it -> java.util.stream.IntStream.range(0, it.size()).mapToObj(i -> it.get(i).validate().stream().map(e -> "address[" + i + "]." + e).toList()).flatMap(java.util.Collection::stream).toList()).orElse(java.util.List.<String>of());
  }
};
