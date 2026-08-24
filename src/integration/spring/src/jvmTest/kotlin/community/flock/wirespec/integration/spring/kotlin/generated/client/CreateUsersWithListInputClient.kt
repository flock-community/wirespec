package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.User
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.CreateUsersWithListInput
data class CreateUsersWithListInputClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : CreateUsersWithListInput.Call {
  override suspend fun createUsersWithListInput(body: List<User>): CreateUsersWithListInput.Response<*> {
    val request = CreateUsersWithListInput.Request(body = body)
    val rawRequest = CreateUsersWithListInput.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return CreateUsersWithListInput.fromRawResponse(serialization, rawResponse)
  }
}
