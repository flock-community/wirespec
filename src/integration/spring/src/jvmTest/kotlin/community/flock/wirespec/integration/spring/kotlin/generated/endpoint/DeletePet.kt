package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
object DeletePet : Wirespec.Endpoint {
  data class Path(
      val petId: Long
    ) : Wirespec.Path
  object Queries : Wirespec.Queries
  data class RequestHeaders(
      val api_key: String?
    ) : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: Unit
    ) : Wirespec.Request<Unit> {
      constructor(petId: Long, api_key: String?) : this(Path(petId = petId), Wirespec.Method.DELETE, Queries, RequestHeaders(api_key = api_key), Unit)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response4XX<T: Any> : Response<T>
  sealed interface ResponseUnit : Response<Unit>
  object Response400Headers : Wirespec.Response.Headers
  data object Response400 : Response4XX<Unit>, ResponseUnit {
      override val status: Int = 400
      override val headers: Response400Headers = Response400Headers
      override val body: Unit = Unit  }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("pet", serialization.serializePath<Long>(request.path.petId, typeOf<Long>())),
      queries = emptyMap<String, List<String>>(),
      headers = mapOf("api_key" to (request.headers.api_key?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>())),
      body = null
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserializePath<Long>(request.path[1], typeOf<Long>()),
      api_key = (request.headers.entries.find { it.key.equals("api_key", ignoreCase = true) }?.value?.let { serialization.deserializeParam<String>(it, typeOf<String>()) })
    )
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response400 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = emptyMap<String, List<String>>(),
            body = null
          )
        }
        else -> {
          error(("Cannot match response with status: " + response.status))
        }
    }
  }
  fun fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> {
    when (response.statusCode) {
        400 -> {
          return Response400
        }
        else -> {
          error(("Cannot match response with status: " + response.statusCode))
        }
    }
  }
  interface Handler : Wirespec.Handler {
      @org.springframework.web.bind.annotation.DeleteMapping("/pet/{petId}")
      suspend fun deletePet(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/pet/{petId}"
        override val method = "DELETE"
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
      suspend fun deletePet(petId: Long, api_key: String?): Response<*>
  }
  val api = Handler
}
