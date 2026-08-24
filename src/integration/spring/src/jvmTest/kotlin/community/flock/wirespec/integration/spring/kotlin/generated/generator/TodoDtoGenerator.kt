package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDto
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoId
object TodoDtoGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): TodoDto =
    TodoDto(
      id = generator.generate(path + listOf("id"), Wirespec.GeneratorFieldShape(
        annotations = emptyMap<String, List<Map<String, Any>>>(),
        generate = { p0 -> TodoIdGenerator.generate(generator, p0) },
        type = typeOf<TodoId>()
      )),
      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )),
      done = generator.generate(path + listOf("done"), Wirespec.GeneratorFieldBoolean(annotations = emptyList<Map<String, Any>>()))
    )
}
