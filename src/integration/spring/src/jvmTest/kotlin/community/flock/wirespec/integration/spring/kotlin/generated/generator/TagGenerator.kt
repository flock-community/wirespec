package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Tag
object TagGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): Tag =
    Tag(
      id = generator.generate(path + listOf("id"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldInteger64(
        min = null,
        max = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )) }))
    )
}
