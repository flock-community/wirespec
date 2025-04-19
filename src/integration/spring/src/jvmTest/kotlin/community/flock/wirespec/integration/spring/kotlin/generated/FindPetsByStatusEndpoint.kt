package community.flock.wirespec.integration.spring.kotlin.generated

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

object FindPetsByStatusEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data class Queries(
    val status: FindPetsByStatusParameterStatus?,
  ) : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    status: FindPetsByStatusParameterStatus?
  ) : Wirespec.Request<Unit> {
    override val path = Path
    override val method = Wirespec.Method.GET
    override val queries = Queries(status)
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", "findByStatus"),
      method = request.method.name,
      queries = (mapOf("status" to (request.queries.status?.let{ serialization.serializeParam(it, typeOf<FindPetsByStatusParameterStatus?>()) } ?: emptyList()))),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      status = request.queries["status"]?.let{ serialization.deserializeParam(it, typeOf<FindPetsByStatusParameterStatus?>()) }
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseListPet : Response<List<Pet>>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: List<Pet>) : Response2XX<List<Pet>>, ResponseListPet {
    override val status = 200
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = serialization.serialize(response.body, typeOf<List<Pet>>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<List<Pet>>()),
      )
      400 -> Response400(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/pet/findByStatus")
    suspend fun findPetsByStatus(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/findByStatus"
      override val method = "GET"
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
