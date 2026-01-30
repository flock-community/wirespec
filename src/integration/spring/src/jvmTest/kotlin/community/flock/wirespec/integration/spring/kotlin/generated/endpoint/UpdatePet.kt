package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf

import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet

object UpdatePet : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    override val body: Pet,
  ) : Wirespec.Request<Pet> {
    override val path = Path
    override val method = Wirespec.Method.PUT
    override val queries = Queries
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet"),
      method = request.method.name,
      queries = emptyMap(),
      headers = CaseInsensitiveMap(),
      body = serialization.serializeBody(request.body, typeOf<Pet>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      body = serialization.deserializeBody(requireNotNull(request.body) { "body is null" }, typeOf<Pet>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponsePet : Response<Pet>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: Pet) : Response2XX<Pet>, ResponsePet {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  data class Response404(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 404
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
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
        headers = CaseInsensitiveMap(),
        body = serialization.serializeBody(response.body, typeOf<Pet>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = CaseInsensitiveMap(),
        body = null,
      )
      is Response404 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = CaseInsensitiveMap(),
        body = null,
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
      )
      400 -> Response400(
        body = Unit,
      )
      404 -> Response404(
        body = Unit,
      )
      405 -> Response405(
        body = Unit,
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PutMapping("/pet")
    suspend fun updatePet(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet"
      override val method = "PUT"
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
