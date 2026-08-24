package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.UploadFileRequestBodyJson;
public interface UploadFileRequestBodyJsonGenerator {
  public static UploadFileRequestBodyJson generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new UploadFileRequestBodyJson(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("foo")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
      java.util.Optional.empty(),
      java.util.List.<java.util.Map<String, Object>>of()
    )))));
  }
}
