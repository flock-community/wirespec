package community.flock.wirespec.integration.spring.kotlin.generated.endpoint

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet

object FindPetsByTags : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data class Queries(
    val tags: List<String>?,
  ) : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    tags: List<String>?
  ) : Wirespec.Request<Unit> {
    override val path = Path
    override val method = Wirespec.Method.GET
    override val queries = Queries(tags)
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", "findByTags"),
      method = request.method.name,
      queries = mapOf(
          "tags" to request.queries.tags?.let{ serialization.serializeParam(it, typeOf<List<String>?>()) }.orEmpty()
        ),
      headers = emptyMap(),
      body = null,
    )

  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
    Request(
      tags =
        request.queries
          .entries
          .find { it.key.equals("tags", ignoreCase = false) }
          ?.let { serialization.deserializeParam(it.value, typeOf<List<String>?>()) }
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

  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = serialization.serializeBody(response.body, typeOf<List<Pet>>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = emptyMap(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<List<Pet>>()),
      )
      400 -> Response400(
        body = Unit,
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/pet/findByTags")
    suspend fun findPetsByTags(request: Request): Response<*>

    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/findByTags"
      override val method = "GET"
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
