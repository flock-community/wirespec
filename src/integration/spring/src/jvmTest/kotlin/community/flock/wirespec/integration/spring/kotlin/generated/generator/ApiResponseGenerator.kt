package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.ApiResponse
object ApiResponseGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): ApiResponse =
    ApiResponse(
      code = generator.generate(path + listOf("code"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldInteger32(
        min = null,
        max = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      type = generator.generate(path + listOf("type"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      message = generator.generate(path + listOf("message"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )) }))
    )
}
