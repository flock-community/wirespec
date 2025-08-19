package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByStatus

import community.flock.wirespec.integration.spring.kotlin.generated.model.FindPetsByStatusParameterStatus

interface FindPetsByStatusClient {
  suspend fun findPetsByStatus(status: FindPetsByStatusParameterStatus?): FindPetsByStatus.Response<*>
}