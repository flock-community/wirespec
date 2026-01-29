package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

import community.flock.wirespec.integration.spring.kotlin.generated.model.UploadFileRequestBody
import community.flock.wirespec.integration.spring.kotlin.generated.model.ApiResponse

object UploadFile : Wirespec.Endpoint {

  data class Path(
    val petId: Long,
  ) : Wirespec.Path

  data class Queries(
    val additionalMetadata: String?,
  ) : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    petId: Long,
    additionalMetadata: String?,
    override val body: UploadFileRequestBody,
  ) : Wirespec.Request<UploadFileRequestBody> {
    override val path = Path(petId)
    override val method = Wirespec.Method.POST
    override val queries = Queries(additionalMetadata)
    override val headers = Headers
  }

object Adapter: Wirespec.Adapter<Request, Response<*>> {

  override fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", request.path.petId.let{serialization.serializePath(it, typeOf<Long>())}, "uploadImage"),
      method = request.method.name,
      queries = (mapOf("additionalMetadata" to (request.queries.additionalMetadata?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))),
      headers = emptyMap(),
      body = serialization.serializeBody(request.body, typeOf<UploadFileRequestBody>()),
    )

  override fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserializePath(request.path[1], typeOf<Long>()),
      additionalMetadata = request.queries["additionalMetadata"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
      body = serialization.deserializeBody(requireNotNull(request.body) { "body is null" }, typeOf<UploadFileRequestBody>()),
    )

  override val pathTemplate = "/pet/{petId}/uploadImage"
  override val method = "POST"

  override fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = serialization.serializeBody(response.body, typeOf<ApiResponse>()),
      )
    }

  override fun fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<ApiResponse>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

}

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>

  sealed interface ResponseApiResponse : Response<ApiResponse>

  data class Response200(override val body: ApiResponse) : Response2XX<ApiResponse>, ResponseApiResponse {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}/uploadImage")
    suspend fun uploadFile(request: Request): Response<*>

  }
}
