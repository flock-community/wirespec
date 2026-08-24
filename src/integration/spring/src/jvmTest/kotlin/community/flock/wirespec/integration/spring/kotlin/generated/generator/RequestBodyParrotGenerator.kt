package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.RequestBodyParrot
object RequestBodyParrotGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): RequestBodyParrot =
    RequestBodyParrot(
      number = generator.generate(path + listOf("number"), Wirespec.GeneratorFieldInteger64(
        min = null,
        max = null,
        annotations = emptyList<Map<String, Any>>()
      )),
      string = generator.generate(path + listOf("string"), Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      ))
    )
}
