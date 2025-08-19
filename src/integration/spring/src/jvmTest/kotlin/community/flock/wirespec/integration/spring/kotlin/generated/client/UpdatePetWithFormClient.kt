package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePetWithForm



interface UpdatePetWithFormClient {
  suspend fun updatePetWithForm(petId: Long, name: String?, status: String?): UpdatePetWithForm.Response<*>
}