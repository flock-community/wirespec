package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

import community.flock.wirespec.integration.spring.kotlin.generated.model.Order

object PlaceOrder : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    override val body: Order,
  ) : Wirespec.Request<Order> {
    override val path = Path
    override val method = Wirespec.Method.POST
    override val queries = Queries
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("store", "order"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Order>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<Order>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseOrder : Response<Order>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: Order) : Response2XX<Order>, ResponseOrder {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  data class Response405(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 405
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = serialization.serialize(response.body, typeOf<Order>()),
      )
      is Response405 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Order>()),
      )
      405 -> Response405(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/store/order")
    suspend fun placeOrder(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/store/order"
      override val method = "POST"
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
