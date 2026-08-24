package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.UploadFileRequestBody
import community.flock.wirespec.integration.spring.kotlin.generated.model.UploadFileRequestBodyJson
object UploadFileRequestBodyGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): UploadFileRequestBody =
    UploadFileRequestBody(
      additionalMetadata = generator.generate(path + listOf("additionalMetadata"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      file = generator.generate(path + listOf("file"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldBytes(annotations = emptyList<Map<String, Any>>())) })),
      json = generator.generate(path + listOf("json"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldShape(
        annotations = mapOf("foo" to emptyList<Map<String, Any>>()),
        generate = { p1 -> UploadFileRequestBodyJsonGenerator.generate(generator, p1) },
        type = typeOf<UploadFileRequestBodyJson>()
      )) }))
    )
}
