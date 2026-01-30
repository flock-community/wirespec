package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf

import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet

object AddPet : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data class Headers(
    val XCorrelationID: String?,
  ) : Wirespec.Request.Headers

  class Request(
    XCorrelationID: String?,
    override val body: Pet,
  ) : Wirespec.Request<Pet> {
    override val path = Path
    override val method = Wirespec.Method.POST
    override val queries = Queries
    override val headers = Headers(XCorrelationID)
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet"),
      method = request.method.name,
      queries = emptyMap(),
      headers = ((mapOf("X-Correlation-ID" to (request.headers.XCorrelationID?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList())))).toCaseInsensitive(),
      body = serialization.serializeBody(request.body, typeOf<Pet>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      XCorrelationID = request.headers["X-Correlation-ID"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
      body = serialization.deserializeBody(requireNotNull(request.body) { "body is null" }, typeOf<Pet>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponsePet : Response<Pet>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: Pet, val XRateLimit: Int?, val XCorrelationID: String?) : Response2XX<Pet>, ResponsePet {
    override val status = 200
    override val headers = ResponseHeaders(XRateLimit, XCorrelationID)
    data class ResponseHeaders(
      val XRateLimit: Int?,
      val XCorrelationID: String?,
    ) : Wirespec.Response.Headers
  }

  data class Response405(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 405
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = ((mapOf("X-Rate-Limit" to (response.headers.XRateLimit?.let{ serialization.serializeParam(it, typeOf<Int?>()) } ?: emptyList()))) + (mapOf("X-Correlation-ID" to (response.headers.XCorrelationID?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList())))).toCaseInsensitive(),
        body = serialization.serializeBody(response.body, typeOf<Pet>()),
      )
      is Response405 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = CaseInsensitiveMap(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<Pet>()),
        XRateLimit = response.headers["X-Rate-Limit"]?.let{ serialization.deserializeParam(it, typeOf<Int?>()) },
        XCorrelationID = response.headers["X-Correlation-ID"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) }
      )
      405 -> Response405(
        body = Unit,
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet")
    suspend fun addPet(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet"
      override val method = "POST"
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
