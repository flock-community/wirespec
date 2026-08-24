package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.FindPetsByStatusParameterStatus;
public interface FindPetsByStatusParameterStatusGenerator {
  public static FindPetsByStatusParameterStatus generate(Wirespec.Generator generator, java.util.List<String> path) {
    return FindPetsByStatusParameterStatus.valueOf(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldEnum(
      java.util.List.of("available", "pending", "sold"),
      java.util.List.<java.util.Map<String, Object>>of(),
      FindPetsByStatusParameterStatus.class
    )));
  }
}
