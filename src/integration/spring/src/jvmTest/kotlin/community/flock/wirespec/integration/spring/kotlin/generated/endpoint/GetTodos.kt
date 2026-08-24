package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDto
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error
object GetTodos : Wirespec.Endpoint {
  object Path : Wirespec.Path
  data class Queries(
      val done: Boolean?
    ) : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: Unit
    ) : Wirespec.Request<Unit> {
      constructor(done: Boolean?) : this(Path, Wirespec.Method.GET, Queries(done = done), RequestHeaders, Unit)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response5XX<T: Any> : Response<T>
  sealed interface ResponseListTodoDto : Response<List<TodoDto>>
  sealed interface ResponseError : Response<Error>
  data class Response200Headers(
      val total: Long
    ) : Wirespec.Response.Headers
  data class Response200(
      override val status: Int,
      override val headers: Response200Headers,
      override val body: List<TodoDto>
    ) : Response2XX<List<TodoDto>>, ResponseListTodoDto {
      constructor(total: Long, body: List<TodoDto>) : this(200, Response200Headers(total = total), body)
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
      path = listOf("api", "todos"),
      queries = mapOf("done" to (request.queries.done?.let { serialization.serializeParam<Boolean>(it, typeOf<Boolean>()) } ?: emptyList<String>())),
      headers = emptyMap<String, List<String>>(),
      body = null
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(done = (request.queries["done"]?.let { serialization.deserializeParam<Boolean>(it, typeOf<Boolean>()) }))
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response200 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = mapOf("total" to serialization.serializeParam<Long>(r.headers.total, typeOf<Long>())),
            body = serialization.serializeBody(r.body, typeOf<List<TodoDto>>())
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
          return Response200(
            total = (response.headers.entries.find { it.key.equals("total", ignoreCase = true) }?.value?.let { serialization.deserializeParam<Long>(it, typeOf<Long>()) } ?: error("Param total cannot be null")),
            body = (response.body?.let { serialization.deserializeBody<List<TodoDto>>(it, typeOf<List<TodoDto>>()) } ?: error("body is null"))
          )
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
      @org.springframework.web.bind.annotation.GetMapping("/api/todos")
      suspend fun getTodos(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/api/todos"
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
      suspend fun getTodos(done: Boolean?): Response<*>
  }
  val api = Handler
}
