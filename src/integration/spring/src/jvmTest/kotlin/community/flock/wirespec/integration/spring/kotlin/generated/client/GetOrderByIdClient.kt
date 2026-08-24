package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Order
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetOrderById
data class GetOrderByIdClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : GetOrderById.Call {
  override suspend fun getOrderById(orderId: Long): GetOrderById.Response<*> {
    val request = GetOrderById.Request(orderId = orderId)
    val rawRequest = GetOrderById.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return GetOrderById.fromRawResponse(serialization, rawResponse)
  }
}
