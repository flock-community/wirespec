package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf



object DeletePet : Wirespec.Endpoint {
  data class Path(
    val petId: Long,
  ) : Wirespec.Path

  data object Queries : Wirespec.Queries

  data class Headers(
    val api_key: String?,
  ) : Wirespec.Request.Headers

  class Request(
    petId: Long,
    api_key: String?
  ) : Wirespec.Request<Unit> {
    override val path = Path(petId)
    override val method = Wirespec.Method.DELETE
    override val queries = Queries
    override val headers = Headers(api_key)
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", request.path.petId.let{serialization.serializePath(it, typeOf<Long>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = mapOf(
          "api_key" to request.headers.api_key?.let{ serialization.serializeParam(it, typeOf<String?>()) }.orEmpty()
        ),
      body = null,
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserializePath(request.path[1], typeOf<Long>()),
      api_key =
        request.headers
          .entries
          .find { it.key.equals("api_key", ignoreCase = true) }
          ?.let { serialization.deserializeParam(it.value, typeOf<String?>()) }
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = ResponseHeaders
    data object ResponseHeaders : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      400 -> Response400(
        body = Unit,
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.DeleteMapping("/pet/{petId}")
    suspend fun deletePet(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/{petId}"
      override val method = "DELETE"
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
