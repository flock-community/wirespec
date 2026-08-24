package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.PetStatus
object PetStatusGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): PetStatus =
    PetStatus.valueOf(generator.generate(path + listOf("value"), Wirespec.GeneratorFieldEnum(
      values = listOf("available", "pending", "sold"),
      annotations = listOf(mapOf("name" to "Description", "parameters" to mapOf("default" to "pet status in the store"))),
      type = typeOf<PetStatus>()
    )))
}
