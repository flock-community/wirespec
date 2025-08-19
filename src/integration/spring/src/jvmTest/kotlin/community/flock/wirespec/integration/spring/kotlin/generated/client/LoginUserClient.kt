package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.LoginUser



interface LoginUserClient {
  suspend fun loginUser(username: String?, password: String?): LoginUser.Response<*>
}