package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByTags
data class FindPetsByTagsClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : FindPetsByTags.Call {
  override suspend fun findPetsByTags(tags: List<String>?): FindPetsByTags.Response<*> {
    val request = FindPetsByTags.Request(tags = tags)
    val rawRequest = FindPetsByTags.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return FindPetsByTags.fromRawResponse(serialization, rawResponse)
  }
}
