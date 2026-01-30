package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf



object LoginUser : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data class Queries(
    val username: String?,
    val password: String?,
  ) : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    username: String?,     password: String?
  ) : Wirespec.Request<Unit> {
    override val path = Path
    override val method = Wirespec.Method.GET
    override val queries = Queries(username, password)
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", "login"),
      method = request.method.name,
      queries = (mapOf("username" to (request.queries.username?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))) + (mapOf("password" to (request.queries.password?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))),
      headers = CaseInsensitiveMap(),
      body = null,
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      username = request.queries["username"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },       password = request.queries["password"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) }
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseString : Response<String>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: String, val XRateLimit: Int?, val XExpiresAfter: String?) : Response2XX<String>, ResponseString {
    override val status = 200
    override val headers = ResponseHeaders(XRateLimit, XExpiresAfter)
    data class ResponseHeaders(
      val XRateLimit: Int?,
      val XExpiresAfter: String?,
    ) : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = ((mapOf("X-Rate-Limit" to (response.headers.XRateLimit?.let{ serialization.serializeParam(it, typeOf<Int?>()) } ?: emptyList()))) + (mapOf("X-Expires-After" to (response.headers.XExpiresAfter?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList())))).toCaseInsensitive(),
        body = serialization.serializeBody(response.body, typeOf<String>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = CaseInsensitiveMap(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<String>()),
        XRateLimit = response.headers["X-Rate-Limit"]?.let{ serialization.deserializeParam(it, typeOf<Int?>()) },
        XExpiresAfter = response.headers["X-Expires-After"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) }
      )
      400 -> Response400(
        body = Unit,
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/user/login")
    suspend fun loginUser(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user/login"
      override val method = "GET"
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
