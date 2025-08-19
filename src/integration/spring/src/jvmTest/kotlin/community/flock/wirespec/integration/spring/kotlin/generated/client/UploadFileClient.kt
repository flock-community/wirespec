package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UploadFile



interface UploadFileClient {
  suspend fun uploadFile(petId: Long, additionalMetadata: String?, body: String): UploadFile.Response<*>
}