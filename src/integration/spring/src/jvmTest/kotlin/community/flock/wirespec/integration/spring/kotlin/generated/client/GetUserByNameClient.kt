package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetUserByName



interface GetUserByNameClient {
  suspend fun getUserByName(username: String): GetUserByName.Response<*>
}