package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.CreateUsersWithListInput

import community.flock.wirespec.integration.spring.kotlin.generated.model.User

interface CreateUsersWithListInputClient {
  suspend fun createUsersWithListInput(body: List<User>): CreateUsersWithListInput.Response<*>
}