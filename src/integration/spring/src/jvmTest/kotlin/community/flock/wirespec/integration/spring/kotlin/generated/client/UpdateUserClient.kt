package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.User
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdateUser
data class UpdateUserClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : UpdateUser.Call {
  override suspend fun updateUser(username: String, body: User): UpdateUser.Response<*> {
    val request = UpdateUser.Request(
      username = username,
      body = body
    )
    val rawRequest = UpdateUser.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return UpdateUser.fromRawResponse(serialization, rawResponse)
  }
}
