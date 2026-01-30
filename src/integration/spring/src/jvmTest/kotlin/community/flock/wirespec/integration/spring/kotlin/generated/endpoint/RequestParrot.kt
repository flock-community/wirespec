package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

import community.flock.wirespec.integration.spring.kotlin.generated.model.RequestBodyParrot
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error

object RequestParrot : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data class Queries(
    val QueryParam: String?,
    val RanDoMQueRY: String?,
  ) : Wirespec.Queries

  data class Headers(
    val XRequestID: String?,
    val RanDoMHeADer: String?,
  ) : Wirespec.Request.Headers

  class Request(
    QueryParam: String?,     RanDoMQueRY: String?,
    XRequestID: String?,     RanDoMHeADer: String?,
    override val body: RequestBodyParrot,
  ) : Wirespec.Request<RequestBodyParrot> {
    override val path = Path
    override val method = Wirespec.Method.POST
    override val queries = Queries(QueryParam, RanDoMQueRY)
    override val headers = Headers(XRequestID, RanDoMHeADer)
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("api", "parrot"),
      method = request.method.name,
      queries = (mapOf("Query-Param" to (request.queries.QueryParam?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))) + (mapOf("RanDoMQueRY" to (request.queries.RanDoMQueRY?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))),
      headers = (mapOf("x-request-id" to (request.headers.XRequestID?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))) + (mapOf("randomheader" to (request.headers.RanDoMHeADer?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))),
      body = serialization.serializeBody(request.body, typeOf<RequestBodyParrot>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      QueryParam = request.queries["Query-Param"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },       RanDoMQueRY = request.queries["RanDoMQueRY"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
      XRequestID = request.headers["x-request-id"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },       RanDoMHeADer = request.headers["randomheader"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
      body = serialization.deserializeBody(requireNotNull(request.body) { "body is null" }, typeOf<RequestBodyParrot>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response5XX<T: Any> : Response<T>

  sealed interface ResponseRequestBodyParrot : Response<RequestBodyParrot>
  sealed interface ResponseError : Response<Error>

  data class Response200(override val body: RequestBodyParrot, val XRequestID: String?, val RanDoMHeADer: String?, val QueryParamParrot: String?, val RanDoMQueRYParrot: String?) : Response2XX<RequestBodyParrot>, ResponseRequestBodyParrot {
    override val status = 200
    override val headers = ResponseHeaders(XRequestID, RanDoMHeADer, QueryParamParrot, RanDoMQueRYParrot)
    data class ResponseHeaders(
      val XRequestID: String?,
      val RanDoMHeADer: String?,
      val QueryParamParrot: String?,
      val RanDoMQueRYParrot: String?,
    ) : Wirespec.Response.Headers
  }

  data class Response500(override val body: Error) : Response5XX<Error>, ResponseError {
    override val status = 500
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = (mapOf("x-request-id" to (response.headers.XRequestID?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))) + (mapOf("randomheader" to (response.headers.RanDoMHeADer?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))) + (mapOf("query-param-parrot" to (response.headers.QueryParamParrot?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))) + (mapOf("randomqueryparrot" to (response.headers.RanDoMQueRYParrot?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))),
        body = serialization.serializeBody(response.body, typeOf<RequestBodyParrot>()),
      )
      is Response500 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = serialization.serializeBody(response.body, typeOf<Error>()),
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<RequestBodyParrot>()),
        XRequestID = response.headers["x-request-id"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
        RanDoMHeADer = response.headers["randomheader"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
        QueryParamParrot = response.headers["query-param-parrot"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
        RanDoMQueRYParrot = response.headers["randomqueryparrot"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) }
      )
      500 -> Response500(
        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<Error>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/api/parrot")
    suspend fun requestParrot(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/api/parrot"
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
