package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Customer
import community.flock.wirespec.integration.spring.kotlin.generated.model.Address
object CustomerGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): Customer =
    Customer(
      id = generator.generate(path + listOf("id"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldInteger64(
        min = null,
        max = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      username = generator.generate(path + listOf("username"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      address = generator.generate(path + listOf("address"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldArray(generate = { p1 -> generator.generate(p1, Wirespec.GeneratorFieldShape(
        annotations = mapOf("street" to emptyList<Map<String, Any>>(), "city" to emptyList<Map<String, Any>>(), "state" to emptyList<Map<String, Any>>(), "zip" to emptyList<Map<String, Any>>()),
        generate = { p2 -> AddressGenerator.generate(generator, p2) },
        type = typeOf<Address>()
      )) })) }))
    )
}
