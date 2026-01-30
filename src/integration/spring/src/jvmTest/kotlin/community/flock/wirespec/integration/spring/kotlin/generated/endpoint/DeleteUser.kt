package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf



object DeleteUser : Wirespec.Endpoint {
  data class Path(
    val username: String,
  ) : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    username: String
  ) : Wirespec.Request<Unit> {
    override val path = Path(username)
    override val method = Wirespec.Method.DELETE
    override val queries = Queries
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", request.path.username.let{serialization.serializePath(it, typeOf<String>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = CaseInsensitiveMap(),
      body = null,
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      username = serialization.deserializePath(request.path[1], typeOf<String>())
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  data class Response404(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 404
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = CaseInsensitiveMap(),
        body = null,
      )
      is Response404 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = CaseInsensitiveMap(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      400 -> Response400(
        body = Unit,
      )
      404 -> Response404(
        body = Unit,
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.DeleteMapping("/user/{username}")
    suspend fun deleteUser(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user/{username}"
      override val method = "DELETE"
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
