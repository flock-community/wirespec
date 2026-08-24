package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Order
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.PlaceOrder
data class PlaceOrderClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : PlaceOrder.Call {
  override suspend fun placeOrder(body: Order): PlaceOrder.Response<*> {
    val request = PlaceOrder.Request(body = body)
    val rawRequest = PlaceOrder.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return PlaceOrder.fromRawResponse(serialization, rawResponse)
  }
}
