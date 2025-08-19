package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.AddPet

import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet

interface AddPetClient {
  suspend fun addPet(body: Pet): AddPet.Response<*>
}