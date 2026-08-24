package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error
object ErrorGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): Error =
    Error(
      code = generator.generate(path + listOf("code"), Wirespec.GeneratorFieldInteger64(
        min = null,
        max = null,
        annotations = emptyList<Map<String, Any>>()
      )),
      description = generator.generate(path + listOf("description"), Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      ))
    )
}
