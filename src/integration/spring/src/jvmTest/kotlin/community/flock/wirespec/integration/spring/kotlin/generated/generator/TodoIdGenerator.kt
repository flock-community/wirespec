package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoId
object TodoIdGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): TodoId =
    TodoId(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldString(
      regex = "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\$",
      annotations = emptyList<Map<String, Any>>()
    )))
}
