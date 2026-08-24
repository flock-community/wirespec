package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDtoPatch
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDto
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error
object PatchTodos : Wirespec.Endpoint {
  data class Path(
      val id: String
    ) : Wirespec.Path
  object Queries : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: TodoDtoPatch
    ) : Wirespec.Request<TodoDtoPatch> {
      constructor(id: String, body: TodoDtoPatch) : this(Path(id = id), Wirespec.Method.PATCH, Queries, RequestHeaders, body)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response5XX<T: Any> : Response<T>
  sealed interface ResponseTodoDto : Response<TodoDto>
  sealed interface ResponseError : Response<Error>
  object Response200Headers : Wirespec.Response.Headers
  data class Response200(
      override val status: Int,
      override val headers: Response200Headers,
      override val body: TodoDto
    ) : Response2XX<TodoDto>, ResponseTodoDto {
      constructor(body: TodoDto) : this(200, Response200Headers, body)
    }
  object Response500Headers : Wirespec.Response.Headers
  data class Response500(
      override val status: Int,
      override val headers: Response500Headers,
      override val body: Error
    ) : Response5XX<Error>, ResponseError {
      constructor(body: Error) : this(500, Response500Headers, body)
    }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("api", "todos", serialization.serializePath<String>(request.path.id, typeOf<String>())),
      queries = emptyMap<String, List<String>>(),
      headers = emptyMap<String, List<String>>(),
      body = serialization.serializeBody<TodoDtoPatch>(request.body, typeOf<TodoDtoPatch>())
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      id = serialization.deserializePath<String>(request.path[2], typeOf<String>()),
      body = (request.body?.let { serialization.deserializeBody<TodoDtoPatch>(it, typeOf<TodoDtoPatch>()) } ?: error("body is null"))
    )
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response200 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = emptyMap<String, List<String>>(),
            body = serialization.serializeBody(r.body, typeOf<TodoDto>())
          )
        }
        is Response500 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = emptyMap<String, List<String>>(),
            body = serialization.serializeBody(r.body, typeOf<Error>())
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
          return Response200(body = (response.body?.let { serialization.deserializeBody<TodoDto>(it, typeOf<TodoDto>()) } ?: error("body is null")))
        }
        500 -> {
          return Response500(body = (response.body?.let { serialization.deserializeBody<Error>(it, typeOf<Error>()) } ?: error("body is null")))
        }
        else -> {
          error(("Cannot match response with status: " + response.statusCode))
        }
    }
  }
  interface Handler : Wirespec.Handler {
      @org.springframework.web.bind.annotation.PatchMapping("/api/todos/{id}")
      suspend fun patchTodos(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/api/todos/{id}"
        override val method = "PATCH"
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
      suspend fun patchTodos(id: String, body: TodoDtoPatch): Response<*>
  }
  val api = Handler
}
