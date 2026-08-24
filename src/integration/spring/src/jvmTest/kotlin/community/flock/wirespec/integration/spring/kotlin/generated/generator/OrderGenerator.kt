package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Order
import community.flock.wirespec.integration.spring.kotlin.generated.model.OrderStatus
object OrderGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): Order =
    Order(
      id = generator.generate(path + listOf("id"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldInteger64(
        min = null,
        max = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      petId = generator.generate(path + listOf("petId"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldInteger64(
        min = null,
        max = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      quantity = generator.generate(path + listOf("quantity"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldInteger32(
        min = null,
        max = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      shipDate = generator.generate(path + listOf("shipDate"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
        regex = null,
        annotations = emptyList<Map<String, Any>>()
      )) })),
      status = generator.generate(path + listOf("status"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldShape(
        annotations = mapOf("value" to listOf(mapOf("name" to "Description", "parameters" to mapOf("default" to "Order Status")))),
        generate = { p1 -> OrderStatusGenerator.generate(generator, p1) },
        type = typeOf<OrderStatus>()
      )) })),
      complete = generator.generate(path + listOf("complete"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldBoolean(annotations = emptyList<Map<String, Any>>())) }))
    )
}
