package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDto;
import community.flock.wirespec.integration.spring.java.generated.model.TodoId;
public interface TodoDtoGenerator {
  public static TodoDto generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new TodoDto(
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("id")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldShape<>(
        java.util.Collections.emptyMap(),
        (p0) -> TodoIdGenerator.generate(generator, p0),
        TodoId.class
      )),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("name")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("done")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldBoolean(java.util.List.<java.util.Map<String, Object>>of()))
    );
  }
}
