package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
object LogoutUser : Wirespec.Endpoint {
  object Path : Wirespec.Path
  object Queries : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data object Request : Wirespec.Request<Unit> {
      override val path: Path = Path
      override val method: Wirespec.Method = Wirespec.Method.GET
      override val queries: Queries = Queries
      override val headers: RequestHeaders = RequestHeaders
      override val body: Unit = Unit  }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface ResponsedXX<T: Any> : Response<T>
  sealed interface ResponseUnit : Response<Unit>
  object ResponseDefaultHeaders : Wirespec.Response.Headers
  data object ResponseDefault : ResponsedXX<Unit>, ResponseUnit {
      override val status: Int = 0
      override val headers: ResponseDefaultHeaders = ResponseDefaultHeaders
      override val body: Unit = Unit  }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("user", "logout"),
      queries = emptyMap<String, List<String>>(),
      headers = emptyMap<String, List<String>>(),
      body = null
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is ResponseDefault -> {
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
        else -> {
          error(("Cannot match response with status: " + response.statusCode))
        }
    }
  }
  interface Handler : Wirespec.Handler {
      @org.springframework.web.bind.annotation.GetMapping("/user/logout")
      suspend fun logoutUser(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/user/logout"
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
      suspend fun logoutUser(): Response<*>
  }
  val api = Handler
}
