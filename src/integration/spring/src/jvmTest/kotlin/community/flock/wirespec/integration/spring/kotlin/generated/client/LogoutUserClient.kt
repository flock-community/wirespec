package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.LogoutUser



interface LogoutUserClient {
  suspend fun logoutUser(): LogoutUser.Response<*>
}