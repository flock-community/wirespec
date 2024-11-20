package community.flock.wirespec.integration.spring.kotlin.generated

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.Method.*
import kotlin.reflect.typeOf

object AddPetEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    override val body: Pet,
  ) : Wirespec.Request<Pet> {
    override val path = Path
    override val method = POST
    override val queries = Queries
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Pet>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<Pet>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponsePet : Response<Pet>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: Pet) : Response2XX<Pet>, ResponsePet {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response405(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 405
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<Pet>()),
      )
      is Response405 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Pet>()),
      )
      405 -> Response405(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet")

suspend fun addPet(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet"
      override val method = POST
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

object UpdatePetEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    override val body: Pet,
  ) : Wirespec.Request<Pet> {
    override val path = Path
    override val method = PUT
    override val queries = Queries
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Pet>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<Pet>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponsePet : Response<Pet>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: Pet) : Response2XX<Pet>, ResponsePet {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response404(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 404
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response405(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 405
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<Pet>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
      is Response404 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
      is Response405 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Pet>()),
      )
      400 -> Response400(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      404 -> Response404(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      405 -> Response405(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PutMapping("/pet")

suspend fun updatePet(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet"
      override val method = PUT
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
    override val method = GET
    override val queries = Queries(status)
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", "findByStatus"),
      method = request.method.name,
      queries = listOf(request.queries.status?.let{"status" to serialization.serialize(it, typeOf<FindPetsByStatusParameterStatus>())}).filterNotNull().toMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      status = request.queries["status"]?.let{ serialization.deserialize(it, typeOf<FindPetsByStatusParameterStatus>()) }
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseListPet : Response<List<Pet>>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: List<Pet>) : Response2XX<List<Pet>>, ResponseListPet {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<List<Pet>>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
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
      override val method = GET
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

object FindPetsByTagsEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data class Queries(
    val tags: List<String>?,
  ) : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    tags: List<String>?
  ) : Wirespec.Request<Unit> {
    override val path = Path
    override val method = GET
    override val queries = Queries(tags)
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", "findByTags"),
      method = request.method.name,
      queries = listOf(request.queries.tags?.let{"tags" to serialization.serialize(it, typeOf<List<String>>())}).filterNotNull().toMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      tags = request.queries["tags"]?.let{ serialization.deserialize(it, typeOf<List<String>>()) }
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseListPet : Response<List<Pet>>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: List<Pet>) : Response2XX<List<Pet>>, ResponseListPet {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<List<Pet>>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
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
    @org.springframework.web.bind.annotation.GetMapping("/pet/findByTags")

suspend fun findPetsByTags(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/findByTags"
      override val method = GET
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

object GetPetByIdEndpoint : Wirespec.Endpoint {
  data class Path(
    val petId: Long,
  ) : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    petId: Long
  ) : Wirespec.Request<Unit> {
    override val path = Path(petId)
    override val method = GET
    override val queries = Queries
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", request.path.petId.let{serialization.serialize(it, typeOf<Long>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserialize(request.path[1], typeOf<Long>())
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponsePet : Response<Pet>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: Pet) : Response2XX<Pet>, ResponsePet {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response404(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 404
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<Pet>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
      is Response404 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Pet>()),
      )
      400 -> Response400(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      404 -> Response404(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/pet/{petId}")

suspend fun getPetById(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/{petId}"
      override val method = GET
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

object UpdatePetWithFormEndpoint : Wirespec.Endpoint {
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
    override val method = POST
    override val queries = Queries(name, status)
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", request.path.petId.let{serialization.serialize(it, typeOf<Long>())}),
      method = request.method.name,
      queries = listOf(request.queries.name?.let{"name" to serialization.serialize(it, typeOf<String>())}, request.queries.status?.let{"status" to serialization.serialize(it, typeOf<String>())}).filterNotNull().toMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserialize(request.path[1], typeOf<Long>()),
      name = request.queries["name"]?.let{ serialization.deserialize(it, typeOf<String>()) },       status = request.queries["status"]?.let{ serialization.deserialize(it, typeOf<String>()) }
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Response405(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 405
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response405 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      405 -> Response405(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}")

suspend fun updatePetWithForm(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/{petId}"
      override val method = POST
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

object DeletePetEndpoint : Wirespec.Endpoint {
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
    override val method = DELETE
    override val queries = Queries
    override val headers = Headers(api_key)
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", request.path.petId.let{serialization.serialize(it, typeOf<Long>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = listOf(request.headers.api_key?.let{"api_key" to serialization.serialize(it, typeOf<String>())}).filterNotNull().toMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserialize(request.path[1], typeOf<Long>()),
      api_key = request.headers["api_key"]?.let{ serialization.deserialize(it, typeOf<String>()) }
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      400 -> Response400(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.DeleteMapping("/pet/{petId}")

suspend fun deletePet(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/{petId}"
      override val method = DELETE
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

object UploadFileEndpoint : Wirespec.Endpoint {
  data class Path(
    val petId: Long,
  ) : Wirespec.Path

  data class Queries(
    val additionalMetadata: String?,
  ) : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    petId: Long,
    additionalMetadata: String?,
    override val body: String,
  ) : Wirespec.Request<String> {
    override val path = Path(petId)
    override val method = POST
    override val queries = Queries(additionalMetadata)
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("pet", request.path.petId.let{serialization.serialize(it, typeOf<Long>())}, "uploadImage"),
      method = request.method.name,
      queries = listOf(request.queries.additionalMetadata?.let{"additionalMetadata" to serialization.serialize(it, typeOf<String>())}).filterNotNull().toMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<String>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      petId = serialization.deserialize(request.path[1], typeOf<Long>()),
      additionalMetadata = request.queries["additionalMetadata"]?.let{ serialization.deserialize(it, typeOf<String>()) },
      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<String>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>

  sealed interface ResponseApiResponse : Response<ApiResponse>

  data class Response200(override val body: ApiResponse) : Response2XX<ApiResponse>, ResponseApiResponse {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<ApiResponse>()),
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<ApiResponse>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}/uploadImage")

suspend fun uploadFile(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/pet/{petId}/uploadImage"
      override val method = POST
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

object GetInventoryEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  object Request : Wirespec.Request<Unit> {
    override val path = Path
    override val method = GET
    override val queries = Queries
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("store", "inventory"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>

  sealed interface ResponseMapStringLong : Response<Map<String, Long>>

  data class Response200(override val body: Map<String, Long>) : Response2XX<Map<String, Long>>, ResponseMapStringLong {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<Map<String, Long>>()),
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Map<String, Long>>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/store/inventory")

suspend fun getInventory(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/store/inventory"
      override val method = GET
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

object PlaceOrderEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    override val body: Order,
  ) : Wirespec.Request<Order> {
    override val path = Path
    override val method = POST
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
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response405(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 405
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<Order>()),
      )
      is Response405 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
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
      override val method = POST
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

object GetOrderByIdEndpoint : Wirespec.Endpoint {
  data class Path(
    val orderId: Long,
  ) : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    orderId: Long
  ) : Wirespec.Request<Unit> {
    override val path = Path(orderId)
    override val method = GET
    override val queries = Queries
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("store", "order", request.path.orderId.let{serialization.serialize(it, typeOf<Long>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      orderId = serialization.deserialize(request.path[2], typeOf<Long>())
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseOrder : Response<Order>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: Order) : Response2XX<Order>, ResponseOrder {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response404(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 404
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<Order>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
      is Response404 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Order>()),
      )
      400 -> Response400(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      404 -> Response404(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/store/order/{orderId}")

suspend fun getOrderById(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/store/order/{orderId}"
      override val method = GET
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

object DeleteOrderEndpoint : Wirespec.Endpoint {
  data class Path(
    val orderId: Long,
  ) : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    orderId: Long
  ) : Wirespec.Request<Unit> {
    override val path = Path(orderId)
    override val method = DELETE
    override val queries = Queries
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("store", "order", request.path.orderId.let{serialization.serialize(it, typeOf<Long>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      orderId = serialization.deserialize(request.path[2], typeOf<Long>())
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response404(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 404
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
      is Response404 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      400 -> Response400(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      404 -> Response404(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.DeleteMapping("/store/order/{orderId}")

suspend fun deleteOrder(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/store/order/{orderId}"
      override val method = DELETE
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

object CreateUserEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    override val body: User,
  ) : Wirespec.Request<User> {
    override val path = Path
    override val method = POST
    override val queries = Queries
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<User>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<User>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface ResponsedXX<T: Any> : Response<T>

  sealed interface ResponseUser : Response<User>

  data class Responsedefault(override val body: User) : ResponsedXX<User>, ResponseUser {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Responsedefault -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<User>()),
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {

      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/user")

suspend fun createUser(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user"
      override val method = POST
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

object CreateUsersWithListInputEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    override val body: List<User>,
  ) : Wirespec.Request<List<User>> {
    override val path = Path
    override val method = POST
    override val queries = Queries
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", "createWithList"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<List<User>>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<List<User>>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface ResponsedXX<T: Any> : Response<T>

  sealed interface ResponseUser : Response<User>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: User) : Response2XX<User>, ResponseUser {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Responsedefault(override val body: Unit) : ResponsedXX<Unit>, ResponseUnit {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<User>()),
      )
      is Responsedefault -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<User>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/user/createWithList")

suspend fun createUsersWithListInput(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user/createWithList"
      override val method = POST
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

object LoginUserEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data class Queries(
    val username: String?,
    val password: String?,
  ) : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    username: String?,     password: String?
  ) : Wirespec.Request<Unit> {
    override val path = Path
    override val method = GET
    override val queries = Queries(username, password)
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", "login"),
      method = request.method.name,
      queries = listOf(request.queries.username?.let{"username" to serialization.serialize(it, typeOf<String>())}, request.queries.password?.let{"password" to serialization.serialize(it, typeOf<String>())}).filterNotNull().toMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      username = request.queries["username"]?.let{ serialization.deserialize(it, typeOf<String>()) },       password = request.queries["password"]?.let{ serialization.deserialize(it, typeOf<String>()) }
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseString : Response<String>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: String) : Response2XX<String>, ResponseString {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<String>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<String>()),
      )
      400 -> Response400(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/user/login")

suspend fun loginUser(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user/login"
      override val method = GET
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

object LogoutUserEndpoint : Wirespec.Endpoint {
  data object Path : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  object Request : Wirespec.Request<Unit> {
    override val path = Path
    override val method = GET
    override val queries = Queries
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", "logout"),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface ResponsedXX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Responsedefault(override val body: Unit) : ResponsedXX<Unit>, ResponseUnit {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Responsedefault -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {

      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/user/logout")

suspend fun logoutUser(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user/logout"
      override val method = GET
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

object GetUserByNameEndpoint : Wirespec.Endpoint {
  data class Path(
    val username: String,
  ) : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    username: String
  ) : Wirespec.Request<Unit> {
    override val path = Path(username)
    override val method = GET
    override val queries = Queries
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", request.path.username.let{serialization.serialize(it, typeOf<String>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      username = serialization.deserialize(request.path[1], typeOf<String>())
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response2XX<T: Any> : Response<T>
  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseUser : Response<User>
  sealed interface ResponseUnit : Response<Unit>

  data class Response200(override val body: User) : Response2XX<User>, ResponseUser {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response404(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 404
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response200 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = serialization.serialize(response.body, typeOf<User>()),
      )
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
      is Response404 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      200 -> Response200(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<User>()),
      )
      400 -> Response400(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      404 -> Response404(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/user/{username}")

suspend fun getUserByName(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user/{username}"
      override val method = GET
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

object UpdateUserEndpoint : Wirespec.Endpoint {
  data class Path(
    val username: String,
  ) : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    username: String,
    override val body: User,
  ) : Wirespec.Request<User> {
    override val path = Path(username)
    override val method = PUT
    override val queries = Queries
    override val headers = Headers
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", request.path.username.let{serialization.serialize(it, typeOf<String>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<User>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      username = serialization.deserialize(request.path[1], typeOf<String>()),
      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<User>()),
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface ResponsedXX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Responsedefault(override val body: Unit) : ResponsedXX<Unit>, ResponseUnit {
    override val status = 200
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Responsedefault -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {

      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.PutMapping("/user/{username}")

suspend fun updateUser(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user/{username}"
      override val method = PUT
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

object DeleteUserEndpoint : Wirespec.Endpoint {
  data class Path(
    val username: String,
  ) : Wirespec.Path

  data object Queries : Wirespec.Queries

  data object Headers : Wirespec.Request.Headers

  class Request(
    username: String
  ) : Wirespec.Request<Unit> {
    override val path = Path(username)
    override val method = DELETE
    override val queries = Queries
    override val headers = Headers
    override val body = Unit
  }

  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
    Wirespec.RawRequest(
      path = listOf("user", request.path.username.let{serialization.serialize(it, typeOf<String>())}),
      method = request.method.name,
      queries = emptyMap(),
      headers = emptyMap(),
      body = serialization.serialize(request.body, typeOf<Unit>()),
    )

  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
    Request(
      username = serialization.deserialize(request.path[1], typeOf<String>())
    )

  sealed interface Response<T: Any> : Wirespec.Response<T>

  sealed interface Response4XX<T: Any> : Response<T>

  sealed interface ResponseUnit : Response<Unit>

  data class Response400(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 400
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  data class Response404(override val body: Unit) : Response4XX<Unit>, ResponseUnit {
    override val status = 404
    override val headers = Headers
    data object Headers : Wirespec.Response.Headers
  }

  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
    when(response) {
      is Response400 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
      is Response404 -> Wirespec.RawResponse(
        statusCode = response.status,
        headers = mapOf(),
        body = null,
      )
    }

  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
    when (response.statusCode) {
      400 -> Response400(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      404 -> Response404(
        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Unit>()),
      )
      else -> error("Cannot match response with status: ${response.statusCode}")
    }

  interface Handler: Wirespec.Handler {
    @org.springframework.web.bind.annotation.DeleteMapping("/user/{username}")

suspend fun deleteUser(request: Request): Response<*>
    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
      override val pathTemplate = "/user/{username}"
      override val method = DELETE
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

enum class FindPetsByStatusParameterStatus (override val label: String): Wirespec.Enum {
  available("available"),
  pending("pending"),
  sold("sold");
  override fun toString(): String {
    return label
  }
}

data class Order(
  val id: Long?,
  val petId: Long?,
  val quantity: Long?,
  val shipDate: String?,
  val status: OrderStatus?,
  val complete: Boolean?
)

enum class OrderStatus (override val label: String): Wirespec.Enum {
  placed("placed"),
  approved("approved"),
  delivered("delivered");
  override fun toString(): String {
    return label
  }
}

data class Customer(
  val id: Long?,
  val username: String?,
  val address: List<Address>?
)

data class Address(
  val street: String?,
  val city: String?,
  val state: String?,
  val zip: String?
)

data class Category(
  val id: Long?,
  val name: String?
)

data class User(
  val id: Long?,
  val username: String?,
  val firstName: String?,
  val lastName: String?,
  val email: String?,
  val password: String?,
  val phone: String?,
  val userStatus: Long?
)

data class Tag(
  val id: Long?,
  val name: String?
)

data class Pet(
  val id: Long?,
  val name: String,
  val category: Category?,
  val photoUrls: List<String>,
  val tags: List<Tag>?,
  val status: PetStatus?
)

enum class PetStatus (override val label: String): Wirespec.Enum {
  available("available"),
  pending("pending"),
  sold("sold");
  override fun toString(): String {
    return label
  }
}

data class ApiResponse(
  val code: Long?,
  val type: String?,
  val message: String?
)
