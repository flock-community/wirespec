package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
object FindPetsByTags : Wirespec.Endpoint {
  object Path : Wirespec.Path
  data class Queries(
      val tags: List<String>?
    ) : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: Unit
    ) : Wirespec.Request<Unit> {
      constructor(tags: List<String>?) : this(Path, Wirespec.Method.GET, Queries(tags = tags), RequestHeaders, Unit)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>
  sealed interface ResponseListPet : Response<List<Pet>>
  sealed interface ResponseUnit : Response<Unit>
  object Response200Headers : Wirespec.Response.Headers
  data class Response200(
      override val status: Int,
      override val headers: Response200Headers,
      override val body: List<Pet>
    ) : Response2XX<List<Pet>>, ResponseListPet {
      constructor(body: List<Pet>) : this(200, Response200Headers, body)
    }
  object Response400Headers : Wirespec.Response.Headers
  data object Response400 : Response4XX<Unit>, ResponseUnit {
      override val status: Int = 400
      override val headers: Response400Headers = Response400Headers
      override val body: Unit = Unit  }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("pet", "findByTags"),
      queries = mapOf("tags" to (request.queries.tags?.let { serialization.serializeParam<List<String>>(it, typeOf<List<String>>()) } ?: emptyList<String>())),
      headers = emptyMap<String, List<String>>(),
      body = null
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(tags = (request.queries["tags"]?.let { serialization.deserializeParam<List<String>>(it, typeOf<List<String>>()) }))
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response200 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = emptyMap<String, List<String>>(),
            body = serialization.serializeBody(r.body, typeOf<List<Pet>>())
          )
        }
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
        200 -> {
          return Response200(body = (response.body?.let { serialization.deserializeBody<List<Pet>>(it, typeOf<List<Pet>>()) } ?: error("body is null")))
        }
        400 -> {
          return Response400
        }
        else -> {
          error(("Cannot match response with status: " + response.statusCode))
        }
    }
  }
  interface Handler : Wirespec.Handler {
      @org.springframework.web.bind.annotation.GetMapping("/pet/findByTags")
      suspend fun findPetsByTags(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/pet/findByTags"
        override val method = "GET"
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
      suspend fun findPetsByTags(tags: List<String>?): Response<*>
  }
  val api = Handler
}
