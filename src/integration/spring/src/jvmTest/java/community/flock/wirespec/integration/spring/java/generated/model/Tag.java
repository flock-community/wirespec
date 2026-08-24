package community.flock.wirespec.integration.spring.java.generated.model;
import community.flock.wirespec.java.Wirespec;
public record Tag (
  java.util.Optional<Long> id,
  java.util.Optional<String> name
) implements Wirespec.Shape {
  @Override
  public java.util.List<String> validate() {
    return java.util.List.<String>of();
  }
};
