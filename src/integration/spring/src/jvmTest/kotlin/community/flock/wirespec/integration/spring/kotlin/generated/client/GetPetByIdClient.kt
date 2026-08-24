package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetPetById
data class GetPetByIdClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : GetPetById.Call {
  override suspend fun getPetById(petId: Long): GetPetById.Response<*> {
    val request = GetPetById.Request(petId = petId)
    val rawRequest = GetPetById.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return GetPetById.fromRawResponse(serialization, rawResponse)
  }
}
