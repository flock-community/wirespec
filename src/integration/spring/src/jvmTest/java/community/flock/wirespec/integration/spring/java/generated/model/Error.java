package community.flock.wirespec.integration.spring.java.generated.model;
import community.flock.wirespec.java.Wirespec;
public record Error (
  Long code,
  String description
) implements Wirespec.Shape {
  @Override
  public java.util.List<String> validate() {
    return java.util.List.<String>of();
  }
};
