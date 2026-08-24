package community.flock.wirespec.integration.spring.kotlin.generated.generator
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.OrderStatus
object OrderStatusGenerator {
  fun generate(generator: Wirespec.Generator, path: List<String>): OrderStatus =
    OrderStatus.valueOf(generator.generate(path + listOf("value"), Wirespec.GeneratorFieldEnum(
      values = listOf("placed", "approved", "delivered"),
      annotations = listOf(mapOf("name" to "Description", "parameters" to mapOf("default" to "Order Status"))),
      type = typeOf<OrderStatus>()
    )))
}
