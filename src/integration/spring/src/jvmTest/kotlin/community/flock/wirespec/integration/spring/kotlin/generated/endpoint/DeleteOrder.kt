package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
object DeleteOrder : Wirespec.Endpoint {
  data class Path(
      val orderId: Long
    ) : Wirespec.Path
  object Queries : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: Unit
    ) : Wirespec.Request<Unit> {
      constructor(orderId: Long) : this(Path(orderId = orderId), Wirespec.Method.DELETE, Queries, RequestHeaders, Unit)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response4XX<T: Any> : Response<T>
  sealed interface ResponseUnit : Response<Unit>
  object Response400Headers : Wirespec.Response.Headers
  data object Response400 : Response4XX<Unit>, ResponseUnit {
      override val status: Int = 400
      override val headers: Response400Headers = Response400Headers
      override val body: Unit = Unit  }
  object Response404Headers : Wirespec.Response.Headers
  data object Response404 : Response4XX<Unit>, ResponseUnit {
      override val status: Int = 404
      override val headers: Response404Headers = Response404Headers
      override val body: Unit = Unit  }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("store", "order", serialization.serializePath<Long>(request.path.orderId, typeOf<Long>())),
      queries = emptyMap<String, List<String>>(),
      headers = emptyMap<String, List<String>>(),
      body = null
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(orderId = serialization.deserializePath<Long>(request.path[2], typeOf<Long>()))
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response400 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = emptyMap<String, List<String>>(),
            body = null
          )
        }
        is Response404 -> {
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
        400 -> {
          return Response400
        }
        404 -> {
          return Response404
        }
        else -> {
          error(("Cannot match response with status: " + response.statusCode))
        }
    }
  }
  interface Handler : Wirespec.Handler {
      @org.springframework.web.bind.annotation.DeleteMapping("/store/order/{orderId}")
      suspend fun deleteOrder(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/store/order/{orderId}"
        override val method = "DELETE"
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
      suspend fun deleteOrder(orderId: Long): Response<*>
  }
  val api = Handler
}
