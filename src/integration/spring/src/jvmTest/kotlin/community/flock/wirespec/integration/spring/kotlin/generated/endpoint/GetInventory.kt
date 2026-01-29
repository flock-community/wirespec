package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf



object GetInventory : Wirespec.Endpoint {

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

object Adapter: Wirespec.Adapter<Request, Response<*>> {

  override fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("store", "inventory"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = null,
    )

  override fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request

  override val pathTemplate = "/store/inventory"
  override val method = "GET"

  override fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = serialization.serializeBody(response.body, typeOf<Map<String, Int>>()),
      )
    }

  override fun fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<Map<String, Int>>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

}

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>

  sealed interface ResponseMapStringInt : Response<Map<String, Int>>

  data class Response200(override val body: Map<String, Int>) : Response2XX<Map<String, Int>>, ResponseMapStringInt {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/store/inventory")
    suspend fun getInventory(request: Request): Response<*>

  }
}
