package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap.Companion.toCaseInsensitive
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

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", request.path.petId.let{serialization.serializePath(it, typeOf<Long>())}, "uploadImage"),
      method = request.method.name,
      queries = (mapOf("additionalMetadata" to (request.queries.additionalMetadata?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))),
      headers = CaseInsensitiveMap(),
      body = serialization.serializeBody(request.body, typeOf<UploadFileRequestBody>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserializePath(request.path[1], typeOf<Long>()),
      additionalMetadata = request.queries["additionalMetadata"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
      body = serialization.deserializeBody(requireNotNull(request.body) { "body is null" }, typeOf<UploadFileRequestBody>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>

  sealed interface ResponseApiResponse : Response<ApiResponse>

  data class Response200(override val body: ApiResponse) : Response2XX<ApiResponse>, ResponseApiResponse {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = CaseInsensitiveMap(),
        body = serialization.serializeBody(response.body, typeOf<ApiResponse>()),
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<ApiResponse>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}/uploadImage")
    suspend fun uploadFile(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/{petId}/uploadImage"
      override val method = "POST"
      override fun server(serialization: Wirespec.Serialization) = object : Wirespec.ServerEdge<Request, Response<*>> {
        override fun from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
        override fun to(response: Response<*>) = toResponse(serialization, response)
      }
      override fun client(serialization: Wirespec.Serialization) = object : Wirespec.ClientEdge<Request, Response<*>> {
        override fun to(request: Request) = toRequest(serialization, request)
        override fun from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
      }
    }
  }
}
