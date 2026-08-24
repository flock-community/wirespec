package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDtoPatch
object TodoDtoPatchGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): TodoDtoPatch =
    TodoDtoPatch(
      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      done = generator.generate(path + listOf("done"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldBoolean(annotations = emptyList<Map<String, Any>>())) }))
    )
}
