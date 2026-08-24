package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePetWithForm
data class UpdatePetWithFormClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : UpdatePetWithForm.Call {
  override suspend fun updatePetWithForm(petId: Long, name: String?, status: String?): UpdatePetWithForm.Response<*> {
    val request = UpdatePetWithForm.Request(
      petId = petId,
      name = name,
      status = status
    )
    val rawRequest = UpdatePetWithForm.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return UpdatePetWithForm.fromRawResponse(serialization, rawResponse)
  }
}
