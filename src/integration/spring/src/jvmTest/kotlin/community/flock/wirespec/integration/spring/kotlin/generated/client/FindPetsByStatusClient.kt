package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.FindPetsByStatusParameterStatus
import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByStatus
data class FindPetsByStatusClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : FindPetsByStatus.Call {
  override suspend fun findPetsByStatus(status: FindPetsByStatusParameterStatus?): FindPetsByStatus.Response<*> {
    val request = FindPetsByStatus.Request(status = status)
    val rawRequest = FindPetsByStatus.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return FindPetsByStatus.fromRawResponse(serialization, rawResponse)
  }
}
