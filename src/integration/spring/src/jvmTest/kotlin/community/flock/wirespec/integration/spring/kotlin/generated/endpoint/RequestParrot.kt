package community.flock.wirespec.integration.spring.kotlin.generated.endpoint
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.RequestBodyParrot
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error
object RequestParrot : Wirespec.Endpoint {
  object Path : Wirespec.Path
  data class Queries(
      val queryParam: String?,
      val RanDoMQueRY: String?
    ) : Wirespec.Queries
  data class RequestHeaders(
      val xRequestID: String?,
      val RanDoMHeADer: String?
    ) : Wirespec.Request.Headers
  data class Request(
      override val path: Path,
      override val method: Wirespec.Method,
      override val queries: Queries,
      override val headers: RequestHeaders,
      override val body: RequestBodyParrot
    ) : Wirespec.Request<RequestBodyParrot> {
      constructor(queryParam: String?, RanDoMQueRY: String?, xRequestID: String?, RanDoMHeADer: String?, body: RequestBodyParrot) : this(Path, Wirespec.Method.POST, Queries(
        queryParam = queryParam,
        RanDoMQueRY = RanDoMQueRY
      ), RequestHeaders(
        xRequestID = xRequestID,
        RanDoMHeADer = RanDoMHeADer
      ), body)
    }
  sealed interface Response<T: Any> : Wirespec.Response<T>
  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response5XX<T: Any> : Response<T>
  sealed interface ResponseRequestBodyParrot : Response<RequestBodyParrot>
  sealed interface ResponseError : Response<Error>
  data class Response200Headers(
      val xRequestID: String?,
      val RanDoMHeADer: String?,
      val queryParamParrot: String?,
      val RanDoMQueRYParrot: String?
    ) : Wirespec.Response.Headers
  data class Response200(
      override val status: Int,
      override val headers: Response200Headers,
      override val body: RequestBodyParrot
    ) : Response2XX<RequestBodyParrot>, ResponseRequestBodyParrot {
      constructor(xRequestID: String?, RanDoMHeADer: String?, queryParamParrot: String?, RanDoMQueRYParrot: String?, body: RequestBodyParrot) : this(200, Response200Headers(
        xRequestID = xRequestID,
        RanDoMHeADer = RanDoMHeADer,
        queryParamParrot = queryParamParrot,
        RanDoMQueRYParrot = RanDoMQueRYParrot
      ), body)
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
      path = listOf("api", "parrot"),
      queries = mapOf("Query-Param" to (request.queries.queryParam?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>()), "RanDoMQueRY" to (request.queries.RanDoMQueRY?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>())),
      headers = mapOf("X-Request-ID" to (request.headers.xRequestID?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>()), "RanDoMHeADer" to (request.headers.RanDoMHeADer?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>())),
      body = serialization.serializeBody<RequestBodyParrot>(request.body, typeOf<RequestBodyParrot>())
    )
  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      queryParam = (request.queries["Query-Param"]?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
      RanDoMQueRY = (request.queries["RanDoMQueRY"]?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
      xRequestID = (request.headers.entries.find { it.key.equals("X-Request-ID", ignoreCase = true) }?.value?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
      RanDoMHeADer = (request.headers.entries.find { it.key.equals("RanDoMHeADer", ignoreCase = true) }?.value?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
      body = (request.body?.let { serialization.deserializeBody<RequestBodyParrot>(it, typeOf<RequestBodyParrot>()) } ?: error("body is null"))
    )
  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
    when(val r = response) {
        is Response200 -> {
          return Wirespec.RawResponse(
            statusCode = r.status,
            headers = mapOf("X-Request-ID" to (r.headers.xRequestID?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>()), "RanDoMHeADer" to (r.headers.RanDoMHeADer?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>()), "Query-Param-Parrot" to (r.headers.queryParamParrot?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>()), "RanDoMQueRYParrot" to (r.headers.RanDoMQueRYParrot?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>())),
            body = serialization.serializeBody(r.body, typeOf<RequestBodyParrot>())
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
            xRequestID = (response.headers.entries.find { it.key.equals("X-Request-ID", ignoreCase = true) }?.value?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
            RanDoMHeADer = (response.headers.entries.find { it.key.equals("RanDoMHeADer", ignoreCase = true) }?.value?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
            queryParamParrot = (response.headers.entries.find { it.key.equals("Query-Param-Parrot", ignoreCase = true) }?.value?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
            RanDoMQueRYParrot = (response.headers.entries.find { it.key.equals("RanDoMQueRYParrot", ignoreCase = true) }?.value?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
            body = (response.body?.let { serialization.deserializeBody<RequestBodyParrot>(it, typeOf<RequestBodyParrot>()) } ?: error("body is null"))
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
      @org.springframework.web.bind.annotation.PostMapping("/api/parrot")
      suspend fun requestParrot(request: Request): Response<*>
      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        override val pathTemplate = "/api/parrot"
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
      suspend fun requestParrot(queryParam: String?, RanDoMQueRY: String?, xRequestID: String?, RanDoMHeADer: String?, body: RequestBodyParrot): Response<*>
  }
  val api = Handler
}
