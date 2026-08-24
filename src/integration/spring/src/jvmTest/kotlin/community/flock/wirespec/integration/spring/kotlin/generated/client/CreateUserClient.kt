package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.User
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.CreateUser
data class CreateUserClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : CreateUser.Call {
  override suspend fun createUser(body: User): CreateUser.Response<*> {
    val request = CreateUser.Request(body = body)
    val rawRequest = CreateUser.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return CreateUser.fromRawResponse(serialization, rawResponse)
  }
}
