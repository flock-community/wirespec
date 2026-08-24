package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.Order
object PlaceOrder : Wirespec.Endpoint {
  object Path : Wirespec.Path
  object Queries : Wirespec.Queries
  object RequestHeaders : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: Order
    ) : Wirespec.Request<Order> {
      constructor(body: Order) : this(Path, Wirespec.Method.POST, Queries, RequestHeaders, body)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>
  sealed interface ResponseOrder : Response<Order>
  sealed interface ResponseUnit : Response<Unit>
  object Response200Headers : Wirespec.Response.Headers
  data class Response200(
      override val status: Int,
      override val headers: Response200Headers,
      override val body: Order
    ) : Response2XX<Order>, ResponseOrder {
      constructor(body: Order) : this(200, Response200Headers, body)
    }
  object Response405Headers : Wirespec.Response.Headers
  data object Response405 : Response4XX<Unit>, ResponseUnit {
      override val status: Int = 405
      override val headers: Response405Headers = Response405Headers
      override val body: Unit = Unit  }
  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = request.method.name,
      path = listOf("store", "order"),
      queries = emptyMap<String, List<String>>(),
      headers = emptyMap<String, List<String>>(),
      body = serialization.serializeBody<Order>(request.body, typeOf<Order>())
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(body = (request.body?.let { serialization.deserializeBody<Order>(it, typeOf<Order>()) } ?: error("body is null")))
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response200 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = emptyMap<String, List<String>>(),
            body = serialization.serializeBody(r.body, typeOf<Order>())
          )
        }
        is Response405 -> {
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
        200 -> {
          return Response200(body = (response.body?.let { serialization.deserializeBody<Order>(it, typeOf<Order>()) } ?: error("body is null")))
        }
        405 -> {
          return Response405
        }
        else -> {
          error(("Cannot match response with status: " + response.statusCode))
        }
    }
  }
  interface Handler : Wirespec.Handler {
      @org.springframework.web.bind.annotation.PostMapping("/store/order")
      suspend fun placeOrder(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/store/order"
        override val method = "POST"
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
      suspend fun placeOrder(body: Order): Response<*>
  }
  val api = Handler
}
