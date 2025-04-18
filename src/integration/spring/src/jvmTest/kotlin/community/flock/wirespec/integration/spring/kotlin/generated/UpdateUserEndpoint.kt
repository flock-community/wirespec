package community.flock.wirespec.integration.spring.kotlin.generated

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

object UpdateUserEndpoint : Wirespec.Endpoint {
  data class Path(
    val username: String,
  ) : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    username: String,
    override val body: User,
  ) : Wirespec.Request<User> {
    override val path = Path(username)
    override val method = Wirespec.Method.PUT
    override val queries = Queries
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", request.path.username.let{serialization.serialize(it, typeOf<String>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<User>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      username = serialization.deserialize(request.path[1], typeOf<String>()),
      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<User>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface ResponsedXX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Responsedefault(override val body: Unit) : ResponsedXX<Unit>, ResponseUnit {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Responsedefault -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {

      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PutMapping("/user/{username}")
    suspend fun updateUser(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user/{username}"
      override val method = "PUT"
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
