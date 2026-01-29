package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

import community.flock.wirespec.integration.spring.kotlin.generated.model.User

object UpdateUser : Wirespec.Endpoint {

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

object Adapter: Wirespec.Adapter<Request, Response<*>> {

  override fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", request.path.username.let{serialization.serializePath(it, typeOf<String>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serializeBody(request.body, typeOf<User>()),
    )

  override fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      username = serialization.deserializePath(request.path[1], typeOf<String>()),
      body = serialization.deserializeBody(requireNotNull(request.body) { "body is null" }, typeOf<User>()),
    )

  override val pathTemplate = "/user/{username}"
  override val method = "PUT"

  override fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Responsedefault -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = null,
      )
    }

  override fun fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {

      else -> error("Cannot match response with status: ${response.statusCode}")
    }

}

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface ResponsedXX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Responsedefault(override val body: Unit) : ResponsedXX<Unit>, ResponseUnit {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PutMapping("/user/{username}")
    suspend fun updateUser(request: Request): Response<*>

  }
}
