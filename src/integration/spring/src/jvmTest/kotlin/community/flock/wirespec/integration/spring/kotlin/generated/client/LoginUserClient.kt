package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.LoginUser
data class LoginUserClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : LoginUser.Call {
  override suspend fun loginUser(username: String?, password: String?): LoginUser.Response<*> {
    val request = LoginUser.Request(
      username = username,
      password = password
    )
    val rawRequest = LoginUser.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return LoginUser.fromRawResponse(serialization, rawResponse)
  }
}
