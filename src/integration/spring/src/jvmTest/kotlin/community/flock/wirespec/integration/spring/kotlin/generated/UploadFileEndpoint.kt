package community.flock.wirespec.integration.spring.kotlin.generated

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

object UploadFileEndpoint : Wirespec.Endpoint {
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
    override val body: String,
  ) : Wirespec.Request<String> {
    override val path = Path(petId)
    override val method = Wirespec.Method.POST
    override val queries = Queries(additionalMetadata)
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", request.path.petId.let{serialization.serialize(it, typeOf<Long>())}, "uploadImage"),
      method = request.method.name,
      queries = (mapOf("additionalMetadata" to (request.queries.additionalMetadata?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<String>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserialize(request.path[1], typeOf<Long>()),
      additionalMetadata = request.queries["additionalMetadata"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<String>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>

  sealed interface ResponseApiResponse : Response<ApiResponse>

  data class Response200(override val body: ApiResponse) : Response2XX<ApiResponse>, ResponseApiResponse {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = serialization.serialize(response.body, typeOf<ApiResponse>()),
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<ApiResponse>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}/uploadImage")
    suspend fun uploadFile(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/{petId}/uploadImage"
      override val method = "POST"
      override fun server(serialization: Wirespec.Serialization<String>) = object : Wirespec.ServerEdge<Request, Response<*>> {
        override fun from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
        override fun to(response: Response<*>) = toResponse(serialization, response)
      }
      override fun client(serialization: Wirespec.Serialization<String>) = object : Wirespec.ClientEdge<Request, Response<*>> {
        override fun to(request: Request) = toRequest(serialization, request)
        override fun from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
      }
    }
  }
}
