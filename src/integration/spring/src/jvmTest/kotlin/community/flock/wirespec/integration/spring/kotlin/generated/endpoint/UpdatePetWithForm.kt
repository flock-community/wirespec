package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import io.ktor.util.CaseInsensitiveMap
import io.ktor.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf



object UpdatePetWithForm : Wirespec.Endpoint {
  data class Path(
    val petId: Long,
  ) : Wirespec.Path

  data class Queries(
    val name: String?,
    val status: String?,
  ) : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    petId: Long,
    name: String?,     status: String?
  ) : Wirespec.Request<Unit> {
    override val path = Path(petId)
    override val method = Wirespec.Method.POST
    override val queries = Queries(name, status)
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", request.path.petId.let{serialization.serializePath(it, typeOf<Long>())}),
      method = request.method.name,
      queries = (mapOf("name" to (request.queries.name?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))) + (mapOf("status" to (request.queries.status?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))),
      headers = CaseInsensitiveMap(),
      body = null,
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserializePath(request.path[1], typeOf<Long>()),
      name = request.queries["name"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },       status = request.queries["status"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) }
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Response405(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 405
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response405 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = CaseInsensitiveMap(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      405 -> Response405(
        body = Unit,
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}")
    suspend fun updatePetWithForm(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/{petId}"
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
