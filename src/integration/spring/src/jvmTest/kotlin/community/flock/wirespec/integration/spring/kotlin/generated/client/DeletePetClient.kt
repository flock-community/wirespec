package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeletePet



interface DeletePetClient {
  suspend fun deletePet(petId: Long, api_key: String?): DeletePet.Response<*>
}