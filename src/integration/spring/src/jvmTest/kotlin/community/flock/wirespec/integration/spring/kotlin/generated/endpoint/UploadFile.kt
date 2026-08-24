package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.UploadFileRequestBody
import community.flock.wirespec.integration.spring.kotlin.generated.model.ApiResponse
object UploadFile : Wirespec.Endpoint {
  data class Path(
      val petId: Long
    ) : Wirespec.Path
  data class Queries(
      val additionalMetadata: String?
    ) : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: UploadFileRequestBody
    ) : Wirespec.Request<UploadFileRequestBody> {
      constructor(petId: Long, additionalMetadata: String?, body: UploadFileRequestBody) : this(Path(petId = petId), Wirespec.Method.POST, Queries(additionalMetadata = additionalMetadata), RequestHeaders, body)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface ResponseApiResponse : Response<ApiResponse>
  object Response200Headers : Wirespec.Response.Headers
  data class Response200(
      override val status: Int,
      override val headers: Response200Headers,
      override val body: ApiResponse
    ) : Response2XX<ApiResponse>, ResponseApiResponse {
      constructor(body: ApiResponse) : this(200, Response200Headers, body)
    }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("pet", serialization.serializePath<Long>(request.path.petId, typeOf<Long>()), "uploadImage"),
      queries = mapOf("additionalMetadata" to (request.queries.additionalMetadata?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>())),
      headers = emptyMap<String, List<String>>(),
      body = serialization.serializeBody<UploadFileRequestBody>(request.body, typeOf<UploadFileRequestBody>())
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserializePath<Long>(request.path[1], typeOf<Long>()),
      additionalMetadata = (request.queries["additionalMetadata"]?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
      body = (request.body?.let { serialization.deserializeBody<UploadFileRequestBody>(it, typeOf<UploadFileRequestBody>()) } ?: error("body is null"))
    )
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response200 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = emptyMap<String, List<String>>(),
            body = serialization.serializeBody(r.body, typeOf<ApiResponse>())
          )
        }
        else -> {
          error(("Cannot match response with status: " + response.status))
        }
    }
  }
  fun fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> {
    when (response.statusCode) {
        200 -> {
          return Response200(body = (response.body?.let { serialization.deserializeBody<ApiResponse>(it, typeOf<ApiResponse>()) } ?: error("body is null")))
        }
        else -> {
          error(("Cannot match response with status: " + response.statusCode))
        }
    }
  }
  interface Handler : Wirespec.Handler {
      @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}/uploadImage")
      suspend fun uploadFile(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/pet/{petId}/uploadImage"
        override val method = "POST"
        override fun server(serialization: Wirespec.Serialization) = object : Wirespec.ServerEdge<Request, Response<*>> {
          override fun from(request: Wirespec.RawRequest) = fromRawRequest(serialization, request)
          override fun to(response: Response<*>) = toRawResponse(serialization, response)
        }
        override fun client(serialization: Wirespec.Serialization) = object : Wirespec.ClientEdge<Request, Response<*>> {
          override fun to(request: Request) = toRawRequest(serialization, request)
          override fun from(response: Wirespec.RawResponse) = fromRawResponse(serialization, response)
        }
      }
  }
  fun interface Call : Wirespec.Call {
      suspend fun uploadFile(petId: Long, additionalMetadata: String?, body: UploadFileRequestBody): Response<*>
  }
  val api = Handler
}
