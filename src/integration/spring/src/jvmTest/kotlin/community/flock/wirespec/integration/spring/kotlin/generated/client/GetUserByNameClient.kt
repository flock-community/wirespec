package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.User
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetUserByName
data class GetUserByNameClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : GetUserByName.Call {
  override suspend fun getUserByName(username: String): GetUserByName.Response<*> {
    val request = GetUserByName.Request(username = username)
    val rawRequest = GetUserByName.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return GetUserByName.fromRawResponse(serialization, rawResponse)
  }
}
