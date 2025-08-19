package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.CreateUser

import community.flock.wirespec.integration.spring.kotlin.generated.model.User

interface CreateUserClient {
  suspend fun createUser(body: User): CreateUser.Response<*>
}