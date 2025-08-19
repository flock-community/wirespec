package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByTags



interface FindPetsByTagsClient {
  suspend fun findPetsByTags(tags: List<String>?): FindPetsByTags.Response<*>
}