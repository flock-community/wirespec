package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.integration.spring.kotlin.generated.model.User
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

object CreateUser : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    override val body: User,
  ) : Wirespec.Request<User> {
    override val path = Path
    override val method = Wirespec.Method.POST
    override val queries = Queries
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serializeBody(request.body, typeOf<User>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      body = serialization.deserializeBody(requireNotNull(request.body) { "body is null" }, typeOf<User>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface ResponsedXX<T: Any> : Response<T>

  sealed interface ResponseUser : Response<User>

  data class Responsedefault(override val body: User) : ResponsedXX<User>, ResponseUser {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Responsedefault -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = serialization.serializeBody(response.body, typeOf<User>()),
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {

      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/user")
    suspend fun createUser(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user"
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
