package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
object UpdatePetWithForm : Wirespec.Endpoint {
  data class Path(
      val petId: Long
    ) : Wirespec.Path
  data class Queries(
      val name: String?,
      val status: String?
    ) : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: Unit
    ) : Wirespec.Request<Unit> {
      constructor(petId: Long, name: String?, status: String?) : this(Path(petId = petId), Wirespec.Method.POST, Queries(
        name = name,
        status = status
      ), RequestHeaders, Unit)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response4XX<T: Any> : Response<T>
  sealed interface ResponseUnit : Response<Unit>
  object Response405Headers : Wirespec.Response.Headers
  data object Response405 : Response4XX<Unit>, ResponseUnit {
      override val status: Int = 405
      override val headers: Response405Headers = Response405Headers
      override val body: Unit = Unit  }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("pet", serialization.serializePath<Long>(request.path.petId, typeOf<Long>())),
      queries = mapOf("name" to (request.queries.name?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>()), "status" to (request.queries.status?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>())),
      headers = emptyMap<String, List<String>>(),
      body = null
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserializePath<Long>(request.path[1], typeOf<Long>()),
      name = (request.queries["name"]?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
      status = (request.queries["status"]?.let { serialization.deserializeParam<String>(it, typeOf<String>()) })
    )
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response405 -> {
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
        405 -> {
          return Response405
        }
        else -> {
          error(("Cannot match response with status: " + response.statusCode))
        }
    }
  }
  interface Handler : Wirespec.Handler {
      @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}")
      suspend fun updatePetWithForm(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/pet/{petId}"
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
      suspend fun updatePetWithForm(petId: Long, name: String?, status: String?): Response<*>
  }
  val api = Handler
}
