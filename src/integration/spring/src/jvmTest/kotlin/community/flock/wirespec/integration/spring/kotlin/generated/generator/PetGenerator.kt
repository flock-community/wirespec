package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import community.flock.wirespec.integration.spring.kotlin.generated.model.Category
import community.flock.wirespec.integration.spring.kotlin.generated.model.Tag
import community.flock.wirespec.integration.spring.kotlin.generated.model.PetStatus
object PetGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): Pet =
    Pet(
      id = generator.generate(path + listOf("id"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldInteger64(
        min = null,
        max = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )),
      category = generator.generate(path + listOf("category"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldShape(
        annotations = mapOf("id" to emptyList<Map<String, Any>>(), "name" to emptyList<Map<String, Any>>()),
        generate = { p1 -> CategoryGenerator.generate(generator, p1) },
        type = typeOf<Category>()
      )) })),
      photoUrls = generator.generate(path + listOf("photoUrls"), Wirespec.GeneratorFieldArray(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      tags = generator.generate(path + listOf("tags"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldArray(generate = { p1 -> generator.generate(p1, Wirespec.GeneratorFieldShape(
        annotations = mapOf("id" to emptyList<Map<String, Any>>(), "name" to emptyList<Map<String, Any>>()),
        generate = { p2 -> TagGenerator.generate(generator, p2) },
        type = typeOf<Tag>()
      )) })) })),
      status = generator.generate(path + listOf("status"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldShape(
        annotations = mapOf("value" to listOf(mapOf("name" to "Description", "parameters" to mapOf("default" to "pet status in the store")))),
        generate = { p1 -> PetStatusGenerator.generate(generator, p1) },
        type = typeOf<PetStatus>()
      )) }))
    )
}
