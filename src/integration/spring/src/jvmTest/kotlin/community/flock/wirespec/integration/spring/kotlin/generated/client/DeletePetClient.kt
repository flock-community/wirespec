package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeletePet
data class DeletePetClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : DeletePet.Call {
  override suspend fun deletePet(petId: Long, api_key: String?): DeletePet.Response<*> {
    val request = DeletePet.Request(
      petId = petId,
      api_key = api_key
    )
    val rawRequest = DeletePet.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return DeletePet.fromRawResponse(serialization, rawResponse)
  }
}
