package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.FindPetsByStatusParameterStatus
object FindPetsByStatusParameterStatusGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): FindPetsByStatusParameterStatus =
    FindPetsByStatusParameterStatus.valueOf(generator.generate(path + listOf("value"), Wirespec.GeneratorFieldEnum(
      values = listOf("available", "pending", "sold"),
      annotations = emptyList<Map<String, Any>>(),
      type = typeOf<FindPetsByStatusParameterStatus>()
    )))
}
