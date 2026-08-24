package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeleteOrder
data class DeleteOrderClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : DeleteOrder.Call {
  override suspend fun deleteOrder(orderId: Long): DeleteOrder.Response<*> {
    val request = DeleteOrder.Request(orderId = orderId)
    val rawRequest = DeleteOrder.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return DeleteOrder.fromRawResponse(serialization, rawResponse)
  }
}
