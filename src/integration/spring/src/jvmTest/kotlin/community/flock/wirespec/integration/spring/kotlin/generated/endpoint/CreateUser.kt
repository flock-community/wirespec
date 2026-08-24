package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.User
object CreateUser : Wirespec.Endpoint {
  object Path : Wirespec.Path
  object Queries : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: User
    ) : Wirespec.Request<User> {
      constructor(body: User) : this(Path, Wirespec.Method.POST, Queries, RequestHeaders, body)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface ResponsedXX<T: Any> : Response<T>
  sealed interface ResponseUser : Response<User>
  object ResponseDefaultHeaders : Wirespec.Response.Headers
  data class ResponseDefault(
      override val status: Int,
      override val headers: ResponseDefaultHeaders,
      override val body: User
    ) : ResponsedXX<User>, ResponseUser {
      constructor(body: User) : this(0, ResponseDefaultHeaders, body)
    }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("user"),
      queries = emptyMap<String, List<String>>(),
      headers = emptyMap<String, List<String>>(),
      body = serialization.serializeBody<User>(request.body, typeOf<User>())
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(body = (request.body?.let { serialization.deserializeBody<User>(it, typeOf<User>()) } ?: error("body is null")))
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is ResponseDefault -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = emptyMap<String, List<String>>(),
            body = serialization.serializeBody(r.body, typeOf<User>())
          )
        }
        else -> {
          error(("Cannot match response with status: " + response.status))
        }
    }
  }
  fun fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> {
    when (response.statusCode) {
        else -> {
          error(("Cannot match response with status: " + response.statusCode))
        }
    }
  }
  interface Handler : Wirespec.Handler {
      @org.springframework.web.bind.annotation.PostMapping("/user")
      suspend fun createUser(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/user"
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
      suspend fun createUser(body: User): Response<*>
  }
  val api = Handler
}
