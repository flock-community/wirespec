package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.UploadFileRequestBody;
import community.flock.wirespec.integration.spring.java.generated.model.UploadFileRequestBodyJson;
public interface UploadFileRequestBodyGenerator {
  public static UploadFileRequestBody generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new UploadFileRequestBody(
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("additionalMetadata")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("file")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldBytes(java.util.List.<java.util.Map<String, Object>>of())))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("json")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldShape<>(
        java.util.Map.ofEntries(java.util.Map.entry("foo", java.util.List.<java.util.Map<String, Object>>of())),
        (p1) -> UploadFileRequestBodyJsonGenerator.generate(generator, p1),
        UploadFileRequestBodyJson.class
      ))))
    );
  }
}
