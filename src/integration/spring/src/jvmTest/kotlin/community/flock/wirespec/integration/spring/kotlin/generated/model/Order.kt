package community.flock.wirespec.integration.spring.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf

data class Order(
  val id: Long?,
  val petId: Long?,
  val quantity: Int?,
  val shipDate: String?,
  val status: OrderStatus?,
  val complete: Boolean?
)
