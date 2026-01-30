package community.flock.wirespec.integration.spring.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import io.ktor.util.CaseInsensitiveMap
import io.ktor.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf

data class Order(
  val id: Long?,
  val petId: Long?,
  val quantity: Int?,
  val shipDate: String?,
  val status: OrderStatus?,
  val complete: Boolean?
)
