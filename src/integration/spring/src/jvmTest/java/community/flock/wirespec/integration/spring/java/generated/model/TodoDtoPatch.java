package community.flock.wirespec.integration.spring.java.generated.model;
import community.flock.wirespec.java.Wirespec;
public record TodoDtoPatch (
  java.util.Optional<String> name,
  java.util.Optional<Boolean> done
) implements Wirespec.Shape {
  @Override
  public java.util.List<String> validate() {
    return java.util.List.<String>of();
  }
};
