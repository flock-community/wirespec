package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.AddPet
data class AddPetClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : AddPet.Call {
  override suspend fun addPet(body: Pet): AddPet.Response<*> {
    val request = AddPet.Request(body = body)
    val rawRequest = AddPet.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return AddPet.fromRawResponse(serialization, rawResponse)
  }
}
