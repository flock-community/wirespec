package community.flock.wirespec.integration.spring.java.generated.model;
import community.flock.wirespec.java.Wirespec;
public record TodoDto (
  TodoId id,
  String name,
  Boolean done
) implements Wirespec.Shape {
  @Override
  public java.util.List<String> validate() {
    return (!id().validate() ? java.util.List.of("id") : java.util.List.<String>of());
  }
};
