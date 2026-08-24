package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.UploadFileRequestBody
import community.flock.wirespec.integration.spring.kotlin.generated.model.ApiResponse
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UploadFile
data class UploadFileClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : UploadFile.Call {
  override suspend fun uploadFile(petId: Long, additionalMetadata: String?, body: UploadFileRequestBody): UploadFile.Response<*> {
    val request = UploadFile.Request(
      petId = petId,
      additionalMetadata = additionalMetadata,
      body = body
    )
    val rawRequest = UploadFile.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return UploadFile.fromRawResponse(serialization, rawResponse)
  }
}
