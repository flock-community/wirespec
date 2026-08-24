package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePet
data class UpdatePetClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : UpdatePet.Call {
  override suspend fun updatePet(body: Pet): UpdatePet.Response<*> {
    val request = UpdatePet.Request(body = body)
    val rawRequest = UpdatePet.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return UpdatePet.fromRawResponse(serialization, rawResponse)
  }
}
