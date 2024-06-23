package community.flock.wirespec.integration.spring.generated

import community.flock.wirespec.Wirespec

interface AddPetEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestApplicationJson(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Pet>
  ) : Request<Pet> {
    constructor(body: Pet) : this(
      path = "/pet",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class RequestApplicationXml(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Pet>
  ) : Request<Pet> {
    constructor(body: Pet) : this(
      path = "/pet",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class RequestApplicationXWwwFormUrlencoded(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Pet>
  ) : Request<Pet> {
    constructor(body: Pet) : this(
      path = "/pet",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/x-www-form-urlencoded", body)
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  sealed interface Response405<T> : Response4XX<T>
  data class Response200ApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Pet>) : Response200<Pet> {
    constructor(body: Pet) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Pet>) : Response200<Pet> {
    constructor(body: Pet) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class Response405Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response405<Unit> {
    constructor() : this(
      status = 405,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/pet"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content?.type == "application/json" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let { RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let { RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let { RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationXml(response.status, response.headers, it) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.status == 405 && response.content == null -> Response405Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.PostMapping("/pet")
suspend fun addPet(request: Request<*>): Response<*>
}
interface UpdatePetEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestApplicationJson(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Pet>
  ) : Request<Pet> {
    constructor(body: Pet) : this(
      path = "/pet",
      method = Wirespec.Method.PUT,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class RequestApplicationXml(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Pet>
  ) : Request<Pet> {
    constructor(body: Pet) : this(
      path = "/pet",
      method = Wirespec.Method.PUT,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class RequestApplicationXWwwFormUrlencoded(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Pet>
  ) : Request<Pet> {
    constructor(body: Pet) : this(
      path = "/pet",
      method = Wirespec.Method.PUT,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/x-www-form-urlencoded", body)
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  sealed interface Response400<T> : Response4XX<T>
  sealed interface Response404<T> : Response4XX<T>
  sealed interface Response405<T> : Response4XX<T>
  data class Response200ApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Pet>) : Response200<Pet> {
    constructor(body: Pet) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Pet>) : Response200<Pet> {
    constructor(body: Pet) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  data class Response404Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response404<Unit> {
    constructor() : this(
      status = 404,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  data class Response405Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response405<Unit> {
    constructor() : this(
      status = 405,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/pet"
    const val METHOD = "PUT"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content?.type == "application/json" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let { RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let { RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let { RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationXml(response.status, response.headers, it) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        response.status == 404 && response.content == null -> Response404Unit(response.status, response.headers, null)
        response.status == 405 && response.content == null -> Response405Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.PutMapping("/pet")
suspend fun updatePet(request: Request<*>): Response<*>
}
interface FindPetsByStatusEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(status: FindPetsByStatusParameterStatus?) : this(
      path = "/pet/findByStatus",
      method = Wirespec.Method.GET,
      query = mapOf<String, List<Any?>>("status" to listOf(status)),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  sealed interface Response400<T> : Response4XX<T>
  data class Response200ApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<List<Pet>>) : Response200<List<Pet>> {
    constructor(body: List<Pet>) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<List<Pet>>) : Response200<List<Pet>> {
    constructor(body: List<Pet>) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/pet/findByStatus"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<List<Pet>>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationXml(response.status, response.headers, it) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<List<Pet>>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.GetMapping("/pet/findByStatus")
suspend fun findPetsByStatus(request: Request<*>): Response<*>
}
interface FindPetsByTagsEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(tags: List<String>?) : this(
      path = "/pet/findByTags",
      method = Wirespec.Method.GET,
      query = mapOf<String, List<Any?>>("tags" to listOf(tags)),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  sealed interface Response400<T> : Response4XX<T>
  data class Response200ApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<List<Pet>>) : Response200<List<Pet>> {
    constructor(body: List<Pet>) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<List<Pet>>) : Response200<List<Pet>> {
    constructor(body: List<Pet>) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/pet/findByTags"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<List<Pet>>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationXml(response.status, response.headers, it) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<List<Pet>>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.GetMapping("/pet/findByTags")
suspend fun findPetsByTags(request: Request<*>): Response<*>
}
interface GetPetByIdEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(petId: Long) : this(
      path = "/pet/${petId}",
      method = Wirespec.Method.GET,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  sealed interface Response400<T> : Response4XX<T>
  sealed interface Response404<T> : Response4XX<T>
  data class Response200ApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Pet>) : Response200<Pet> {
    constructor(body: Pet) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Pet>) : Response200<Pet> {
    constructor(body: Pet) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  data class Response404Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response404<Unit> {
    constructor() : this(
      status = 404,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/pet/{petId}"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationXml(response.status, response.headers, it) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        response.status == 404 && response.content == null -> Response404Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.GetMapping("/pet/{petId}")
suspend fun getPetById(request: Request<*>): Response<*>
}
interface UpdatePetWithFormEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(petId: Long, name: String?, status: String?) : this(
      path = "/pet/${petId}",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>("name" to listOf(name), "status" to listOf(status)),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response405<T> : Response4XX<T>
  data class Response405Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response405<Unit> {
    constructor() : this(
      status = 405,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/pet/{petId}"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 405 && response.content == null -> Response405Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}")
suspend fun updatePetWithForm(request: Request<*>): Response<*>
}
interface DeletePetEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(petId: Long, api_key: String?) : this(
      path = "/pet/${petId}",
      method = Wirespec.Method.DELETE,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>("api_key" to listOf(api_key)),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response400<T> : Response4XX<T>
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/pet/{petId}"
    const val METHOD = "DELETE"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.DeleteMapping("/pet/{petId}")
suspend fun deletePet(request: Request<*>): Response<*>
}
interface UploadFileEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestApplicationOctetStream(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<String?>
  ) : Request<String?> {
    constructor(petId: Long, additionalMetadata: String?, body: String) : this(
      path = "/pet/${petId}/uploadImage",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>("additionalMetadata" to listOf(additionalMetadata)),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/octet-stream", body)
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<ApiResponse>) : Response200<ApiResponse> {
    constructor(body: ApiResponse) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  companion object {
    const val PATH = "/pet/{petId}/uploadImage"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content?.type == "application/octet-stream" -> contentMapper
          .read<String?>(request.content!!, Wirespec.getType(String::class.java, false))
          .let { RequestApplicationOctetStream(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<ApiResponse>(response.content!!, Wirespec.getType(ApiResponse::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}/uploadImage")
suspend fun uploadFile(request: Request<*>): Response<*>
}
interface GetInventoryEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor() : this(
      path = "/store/inventory",
      method = Wirespec.Method.GET,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Long>) : Response200<Long> {
    constructor(body: Long) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  companion object {
    const val PATH = "/store/inventory"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Long>(response.content!!, Wirespec.getType(Long::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.GetMapping("/store/inventory")
suspend fun getInventory(request: Request<*>): Response<*>
}
interface PlaceOrderEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestApplicationJson(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Order?>
  ) : Request<Order?> {
    constructor(body: Order) : this(
      path = "/store/order",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class RequestApplicationXml(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Order?>
  ) : Request<Order?> {
    constructor(body: Order) : this(
      path = "/store/order",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class RequestApplicationXWwwFormUrlencoded(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Order?>
  ) : Request<Order?> {
    constructor(body: Order) : this(
      path = "/store/order",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/x-www-form-urlencoded", body)
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  sealed interface Response405<T> : Response4XX<T>
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Order>) : Response200<Order> {
    constructor(body: Order) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class Response405Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response405<Unit> {
    constructor() : this(
      status = 405,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/store/order"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content?.type == "application/json" -> contentMapper
          .read<Order?>(request.content!!, Wirespec.getType(Order::class.java, false))
          .let { RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<Order?>(request.content!!, Wirespec.getType(Order::class.java, false))
          .let { RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<Order?>(request.content!!, Wirespec.getType(Order::class.java, false))
          .let { RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Order>(response.content!!, Wirespec.getType(Order::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.status == 405 && response.content == null -> Response405Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.PostMapping("/store/order")
suspend fun placeOrder(request: Request<*>): Response<*>
}
interface GetOrderByIdEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(orderId: Long) : this(
      path = "/store/order/${orderId}",
      method = Wirespec.Method.GET,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  sealed interface Response400<T> : Response4XX<T>
  sealed interface Response404<T> : Response4XX<T>
  data class Response200ApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Order>) : Response200<Order> {
    constructor(body: Order) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Order>) : Response200<Order> {
    constructor(body: Order) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  data class Response404Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response404<Unit> {
    constructor() : this(
      status = 404,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/store/order/{orderId}"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<Order>(response.content!!, Wirespec.getType(Order::class.java, false))
          .let { Response200ApplicationXml(response.status, response.headers, it) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Order>(response.content!!, Wirespec.getType(Order::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        response.status == 404 && response.content == null -> Response404Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.GetMapping("/store/order/{orderId}")
suspend fun getOrderById(request: Request<*>): Response<*>
}
interface DeleteOrderEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(orderId: Long) : this(
      path = "/store/order/${orderId}",
      method = Wirespec.Method.DELETE,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response400<T> : Response4XX<T>
  sealed interface Response404<T> : Response4XX<T>
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  data class Response404Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response404<Unit> {
    constructor() : this(
      status = 404,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/store/order/{orderId}"
    const val METHOD = "DELETE"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        response.status == 404 && response.content == null -> Response404Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.DeleteMapping("/store/order/{orderId}")
suspend fun deleteOrder(request: Request<*>): Response<*>
}
interface CreateUserEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestApplicationJson(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<User?>
  ) : Request<User?> {
    constructor(body: User) : this(
      path = "/user",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class RequestApplicationXml(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<User?>
  ) : Request<User?> {
    constructor(body: User) : this(
      path = "/user",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class RequestApplicationXWwwFormUrlencoded(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<User?>
  ) : Request<User?> {
    constructor(body: User) : this(
      path = "/user",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/x-www-form-urlencoded", body)
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface ResponseDefault<T> : Response<T>
  data class ResponseDefaultApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<User>) : ResponseDefault<User> {
    constructor(status: Int, body: User) : this(
      status = status,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class ResponseDefaultApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<User>) : ResponseDefault<User> {
    constructor(status: Int, body: User) : this(
      status = status,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  companion object {
    const val PATH = "/user"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content?.type == "application/json" -> contentMapper
          .read<User?>(request.content!!, Wirespec.getType(User::class.java, false))
          .let { RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<User?>(request.content!!, Wirespec.getType(User::class.java, false))
          .let { RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<User?>(request.content!!, Wirespec.getType(User::class.java, false))
          .let { RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.content?.type == "application/json" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let { ResponseDefaultApplicationJson(response.status, response.headers, it) }
        response.content?.type == "application/xml" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let { ResponseDefaultApplicationXml(response.status, response.headers, it) }
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.PostMapping("/user")
suspend fun createUser(request: Request<*>): Response<*>
}
interface CreateUsersWithListInputEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestApplicationJson(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<List<User>?>
  ) : Request<List<User>?> {
    constructor(body: List<User>) : this(
      path = "/user/createWithList",
      method = Wirespec.Method.POST,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface ResponseDefault<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  data class Response200ApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<User>) : Response200<User> {
    constructor(body: User) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<User>) : Response200<User> {
    constructor(body: User) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class ResponseDefaultUnit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : ResponseDefault<Unit> {
    constructor(status: Int) : this(
      status = status,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/user/createWithList"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content?.type == "application/json" -> contentMapper
          .read<List<User>?>(request.content!!, Wirespec.getType(User::class.java, true))
          .let { RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let { Response200ApplicationXml(response.status, response.headers, it) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.content == null -> ResponseDefaultUnit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.PostMapping("/user/createWithList")
suspend fun createUsersWithListInput(request: Request<*>): Response<*>
}
interface LoginUserEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(username: String?, password: String?) : this(
      path = "/user/login",
      method = Wirespec.Method.GET,
      query = mapOf<String, List<Any?>>("username" to listOf(username), "password" to listOf(password)),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  sealed interface Response400<T> : Response4XX<T>
  data class Response200ApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<String>) : Response200<String> {
    constructor(xRateLimit: Long?, xExpiresAfter: String?, body: String) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>("X-Rate-Limit" to listOf(xRateLimit), "X-Expires-After" to listOf(xExpiresAfter)),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<String>) : Response200<String> {
    constructor(xRateLimit: Long?, xExpiresAfter: String?, body: String) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>("X-Rate-Limit" to listOf(xRateLimit), "X-Expires-After" to listOf(xExpiresAfter)),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/user/login"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<String>(response.content!!, Wirespec.getType(String::class.java, false))
          .let { Response200ApplicationXml(response.status, response.headers, it) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<String>(response.content!!, Wirespec.getType(String::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.GetMapping("/user/login")
suspend fun loginUser(request: Request<*>): Response<*>
}
interface LogoutUserEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor() : this(
      path = "/user/logout",
      method = Wirespec.Method.GET,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface ResponseDefault<T> : Response<T>
  data class ResponseDefaultUnit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : ResponseDefault<Unit> {
    constructor(status: Int) : this(
      status = status,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/user/logout"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.content == null -> ResponseDefaultUnit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.GetMapping("/user/logout")
suspend fun logoutUser(request: Request<*>): Response<*>
}
interface GetUserByNameEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(username: String) : this(
      path = "/user/${username}",
      method = Wirespec.Method.GET,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response2XX<T> : Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response200<T> : Response2XX<T>
  sealed interface Response400<T> : Response4XX<T>
  sealed interface Response404<T> : Response4XX<T>
  data class Response200ApplicationXml(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<User>) : Response200<User> {
    constructor(body: User) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<User>) : Response200<User> {
    constructor(body: User) : this(
      status = 200,
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  data class Response404Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response404<Unit> {
    constructor() : this(
      status = 404,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/user/{username}"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let { Response200ApplicationXml(response.status, response.headers, it) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let { Response200ApplicationJson(response.status, response.headers, it) }
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        response.status == 404 && response.content == null -> Response404Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.GetMapping("/user/{username}")
suspend fun getUserByName(request: Request<*>): Response<*>
}
interface UpdateUserEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestApplicationJson(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<User?>
  ) : Request<User?> {
    constructor(username: String, body: User) : this(
      path = "/user/${username}",
      method = Wirespec.Method.PUT,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/json", body)
    )
  }
  data class RequestApplicationXml(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<User?>
  ) : Request<User?> {
    constructor(username: String, body: User) : this(
      path = "/user/${username}",
      method = Wirespec.Method.PUT,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/xml", body)
    )
  }
  data class RequestApplicationXWwwFormUrlencoded(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<User?>
  ) : Request<User?> {
    constructor(username: String, body: User) : this(
      path = "/user/${username}",
      method = Wirespec.Method.PUT,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = Wirespec.Content("application/x-www-form-urlencoded", body)
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface ResponseDefault<T> : Response<T>
  data class ResponseDefaultUnit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : ResponseDefault<Unit> {
    constructor(status: Int) : this(
      status = status,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/user/{username}"
    const val METHOD = "PUT"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content?.type == "application/json" -> contentMapper
          .read<User?>(request.content!!, Wirespec.getType(User::class.java, false))
          .let { RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<User?>(request.content!!, Wirespec.getType(User::class.java, false))
          .let { RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<User?>(request.content!!, Wirespec.getType(User::class.java, false))
          .let { RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.content == null -> ResponseDefaultUnit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.PutMapping("/user/{username}")
suspend fun updateUser(request: Request<*>): Response<*>
}
interface DeleteUserEndpoint : Wirespec.Endpoint {
  sealed interface Request<T> : Wirespec.Request<T>
  data class RequestUnit(
    override val path: String,
    override val method: Wirespec.Method,
    override val query: Map<String, List<Any?>>,
    override val headers: Map<String, List<Any?>>,
    override val content: Wirespec.Content<Unit>? = null
  ) : Request<Unit> {
    constructor(username: String) : this(
      path = "/user/${username}",
      method = Wirespec.Method.DELETE,
      query = mapOf<String, List<Any?>>(),
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }

  sealed interface Response<T> : Wirespec.Response<T>
  sealed interface Response4XX<T> : Response<T>
  sealed interface Response400<T> : Response4XX<T>
  sealed interface Response404<T> : Response4XX<T>
  data class Response400Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response400<Unit> {
    constructor() : this(
      status = 400,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  data class Response404Unit(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Unit>? = null) : Response404<Unit> {
    constructor() : this(
      status = 404,
      headers = mapOf<String, List<Any?>>(),
      content = null
    )
  }
  companion object {
    const val PATH = "/user/{username}"
    const val METHOD = "DELETE"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
      when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
      when {
        response.status == 400 && response.content == null -> Response400Unit(response.status, response.headers, null)
        response.status == 404 && response.content == null -> Response404Unit(response.status, response.headers, null)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
  @org.springframework.web.bind.annotation.DeleteMapping("/user/{username}")
suspend fun deleteUser(request: Request<*>): Response<*>
}
enum class FindPetsByStatusParameterStatus (val label: String): Wirespec.Enum {
  available("available"),
  pending("pending"),
  sold("sold");
  override fun toString(): String {
    return label
  }
}
data class Order(
  val id: Long? = null,
  val petId: Long? = null,
  val quantity: Long? = null,
  val shipDate: String? = null,
  val status: OrderStatus? = null,
  val complete: Boolean? = null
)
enum class OrderStatus (val label: String): Wirespec.Enum {
  placed("placed"),
  approved("approved"),
  delivered("delivered");
  override fun toString(): String {
    return label
  }
}
data class Customer(
  val id: Long? = null,
  val username: String? = null,
  val address: List<Address>? = null
)
data class Address(
  val street: String? = null,
  val city: String? = null,
  val state: String? = null,
  val zip: String? = null
)
data class Category(
  val id: Long? = null,
  val name: String? = null
)
data class User(
  val id: Long? = null,
  val username: String? = null,
  val firstName: String? = null,
  val lastName: String? = null,
  val email: String? = null,
  val password: String? = null,
  val phone: String? = null,
  val userStatus: Long? = null
)
data class Tag(
  val id: Long? = null,
  val name: String? = null
)
data class Pet(
  val id: Long? = null,
  val name: String,
  val category: Category? = null,
  val photoUrls: List<String>,
  val tags: List<Tag>? = null,
  val status: PetStatus? = null
)
enum class PetStatus (val label: String): Wirespec.Enum {
  available("available"),
  pending("pending"),
  sold("sold");
  override fun toString(): String {
    return label
  }
}
data class ApiResponse(
  val code: Long? = null,
  val type: String? = null,
  val message: String? = null
)
