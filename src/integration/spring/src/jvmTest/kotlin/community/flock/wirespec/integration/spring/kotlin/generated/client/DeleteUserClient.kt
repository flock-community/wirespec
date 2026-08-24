package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeleteUser
data class DeleteUserClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : DeleteUser.Call {
  override suspend fun deleteUser(username: String): DeleteUser.Response<*> {
    val request = DeleteUser.Request(username = username)
    val rawRequest = DeleteUser.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return DeleteUser.fromRawResponse(serialization, rawResponse)
  }
}
