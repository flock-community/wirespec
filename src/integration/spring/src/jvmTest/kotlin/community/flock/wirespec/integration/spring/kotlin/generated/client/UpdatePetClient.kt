package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePet

import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet

interface UpdatePetClient {
  suspend fun updatePet(body: Pet): UpdatePet.Response<*>
}