package community.flock.wirespec.generated.model;
import community.flock.wirespec.java.Wirespec;
public record TodoDto (
  String description
) implements Wirespec.Model {
  @Override
  public java.util.List<String> validate() {
    return java.util.List.<String>of();
  }
};
