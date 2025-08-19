package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeleteUser



interface DeleteUserClient {
  suspend fun deleteUser(username: String): DeleteUser.Response<*>
}