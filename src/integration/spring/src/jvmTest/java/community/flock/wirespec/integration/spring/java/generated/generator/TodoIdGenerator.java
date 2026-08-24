package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.TodoId;
public interface TodoIdGenerator {
  public static TodoId generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new TodoId(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldString(
      java.util.Optional.of("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$"),
      java.util.List.<java.util.Map<String, Object>>of()
    )));
  }
}
