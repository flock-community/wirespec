package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdateUser

import community.flock.wirespec.integration.spring.kotlin.generated.model.User

interface UpdateUserClient {
  suspend fun updateUser(username: String, body: User): UpdateUser.Response<*>
}