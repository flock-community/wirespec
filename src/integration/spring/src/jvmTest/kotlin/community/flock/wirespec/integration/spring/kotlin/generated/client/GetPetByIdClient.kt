package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetPetById



interface GetPetByIdClient {
  suspend fun getPetById(petId: Long): GetPetById.Response<*>
}