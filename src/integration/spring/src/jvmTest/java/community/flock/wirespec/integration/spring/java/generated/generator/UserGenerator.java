package community.flock.wirespec.integration.spring.java.generated.generator;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.User;
public interface UserGenerator {
  public static User generate(Wirespec.Generator generator, java.util.List<String> path) {
    return new User(
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("id")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldInteger64(
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("username")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("firstName")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("lastName")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("email")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("password")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("phone")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldString(
        java.util.Optional.empty(),
        java.util.List.<java.util.Map<String, Object>>of()
      )))),
      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("userStatus")).flatMap(java.util.Collection::stream).toList(), new Wirespec.GeneratorFieldNullable<>((p0) -> generator.generate(p0, new Wirespec.GeneratorFieldInteger32(
        java.util.Optional.empty(),
        java.util.Optional.empty(),
        java.util.List.of(java.util.Map.ofEntries(java.util.Map.entry("name", "Description"), java.util.Map.entry("parameters", java.util.Map.ofEntries(java.util.Map.entry("default", "User Status")))))
      ))))
    );
  }
}
