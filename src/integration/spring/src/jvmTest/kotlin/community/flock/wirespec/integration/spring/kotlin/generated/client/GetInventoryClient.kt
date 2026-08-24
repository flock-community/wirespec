package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetInventory
data class GetInventoryClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : GetInventory.Call {
  override suspend fun getInventory(): GetInventory.Response<*> {
    val request = GetInventory.Request
    val rawRequest = GetInventory.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return GetInventory.fromRawResponse(serialization, rawResponse)
  }
}
