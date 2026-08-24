package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.UploadFileRequestBodyJson
object UploadFileRequestBodyJsonGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): UploadFileRequestBodyJson =
    UploadFileRequestBodyJson(foo = generator.generate(path + listOf("foo"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
      regex = null,
      annotations = emptyList<Map<String, Any>>()
    )) })))
}
