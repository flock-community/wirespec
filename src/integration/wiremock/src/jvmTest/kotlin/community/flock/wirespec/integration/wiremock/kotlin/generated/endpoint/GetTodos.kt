package community.flock.wirespec.integration.wiremock.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

import community.flock.wirespec.integration.wiremock.kotlin.generated.model.TodoDto

object GetTodos : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  object Request : Wirespec.Request<Unit> {
    override val path = Path
    override val method = Wirespec.Method.GET
    override val queries = Queries
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("api", "todos"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = null,
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>

  sealed interface ResponseListTodoDto : Response<List<TodoDto>>

  data class Response200(override val body: List<TodoDto>) : Response2XX<List<TodoDto>>, ResponseListTodoDto {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = serialization.serializeBody(response.body, typeOf<List<TodoDto>>()),
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<List<TodoDto>>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    suspend fun getTodos(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/api/todos"
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
