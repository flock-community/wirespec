package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
object LoginUser : Wirespec.Endpoint {
  object Path : Wirespec.Path
  data class Queries(
      val username: String?,
      val password: String?
    ) : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: Unit
    ) : Wirespec.Request<Unit> {
      constructor(username: String?, password: String?) : this(Path, Wirespec.Method.GET, Queries(
        username = username,
        password = password
      ), RequestHeaders, Unit)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>
  sealed interface ResponseString : Response<String>
  sealed interface ResponseUnit : Response<Unit>
  data class Response200Headers(
      val xRateLimit: Int?,
      val xExpiresAfter: String?
    ) : Wirespec.Response.Headers
  data class Response200(
      override val status: Int,
      override val headers: Response200Headers,
      override val body: String
    ) : Response2XX<String>, ResponseString {
      constructor(xRateLimit: Int?, xExpiresAfter: String?, body: String) : this(200, Response200Headers(
        xRateLimit = xRateLimit,
        xExpiresAfter = xExpiresAfter
      ), body)
    }
  object Response400Headers : Wirespec.Response.Headers
  data object Response400 : Response4XX<Unit>, ResponseUnit {
      override val status: Int = 400
      override val headers: Response400Headers = Response400Headers
      override val body: Unit = Unit  }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("user", "login"),
      queries = mapOf("username" to (request.queries.username?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>()), "password" to (request.queries.password?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>())),
      headers = emptyMap<String, List<String>>(),
      body = null
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      username = (request.queries["username"]?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
      password = (request.queries["password"]?.let { serialization.deserializeParam<String>(it, typeOf<String>()) })
    )
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response200 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = mapOf("X-Rate-Limit" to (r.headers.xRateLimit?.let { serialization.serializeParam<Int>(it, typeOf<Int>()) } ?: emptyList<String>()), "X-Expires-After" to (r.headers.xExpiresAfter?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>())),
            body = serialization.serializeBody(r.body, typeOf<String>())
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
          return Response200(
            xRateLimit = (response.headers.entries.find { it.key.equals("X-Rate-Limit", ignoreCase = true) }?.value?.let { serialization.deserializeParam<Int>(it, typeOf<Int>()) }),
            xExpiresAfter = (response.headers.entries.find { it.key.equals("X-Expires-After", ignoreCase = true) }?.value?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
            body = (response.body?.let { serialization.deserializeBody<String>(it, typeOf<String>()) } ?: error("body is null"))
          )
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
      @org.springframework.web.bind.annotation.GetMapping("/user/login")
      suspend fun loginUser(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/user/login"
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
      suspend fun loginUser(username: String?, password: String?): Response<*>
  }
  val api = Handler
}
