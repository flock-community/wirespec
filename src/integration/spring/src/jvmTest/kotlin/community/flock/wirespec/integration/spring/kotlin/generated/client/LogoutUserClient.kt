package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.LogoutUser
data class LogoutUserClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : LogoutUser.Call {
  override suspend fun logoutUser(): LogoutUser.Response<*> {
    val request = LogoutUser.Request
    val rawRequest = LogoutUser.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return LogoutUser.fromRawResponse(serialization, rawResponse)
  }
}
