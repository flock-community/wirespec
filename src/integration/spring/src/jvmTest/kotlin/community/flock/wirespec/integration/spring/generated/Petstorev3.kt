package community.flock.wirespec.integration.spring.generated

import community.flock.wirespec.Wirespec

interface AddPetEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestApplicationJson(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Pet>?) : Request<Pet> { constructor(body: Pet): this(path = "/pet", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/json", body))}
  data class RequestApplicationXml(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Pet>?) : Request<Pet> { constructor(body: Pet): this(path = "/pet", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/xml", body))}
  data class RequestApplicationXWwwFormUrlencoded(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Pet>?) : Request<Pet> { constructor(body: Pet): this(path = "/pet", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/x-www-form-urlencoded", body))}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  sealed interface Response405<T>: Response4XX<T>
  data class Response200ApplicationXml (override val headers: Map<String, List<Any?>>, val body: Pet ): Response200<Pet> { override val status = 200; override val content = Wirespec.Content("application/xml", body)}
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: Pet ): Response200<Pet> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class Response405Unit (override val headers: Map<String, List<Any?>> ): Response405<Unit> { override val status = 405; override val content = null}

  suspend fun addPet(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/pet"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content?.type == "application/json" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ Response200ApplicationXml(response.headers, it.body) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.status == 405 && response.content == null -> Response405Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface UpdatePetEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestApplicationJson(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Pet>?) : Request<Pet> { constructor(body: Pet): this(path = "/pet", method = Wirespec.Method.PUT, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/json", body))}
  data class RequestApplicationXml(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Pet>?) : Request<Pet> { constructor(body: Pet): this(path = "/pet", method = Wirespec.Method.PUT, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/xml", body))}
  data class RequestApplicationXWwwFormUrlencoded(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Pet>?) : Request<Pet> { constructor(body: Pet): this(path = "/pet", method = Wirespec.Method.PUT, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/x-www-form-urlencoded", body))}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  sealed interface Response400<T>: Response4XX<T>
  sealed interface Response404<T>: Response4XX<T>
  sealed interface Response405<T>: Response4XX<T>
  data class Response200ApplicationXml (override val headers: Map<String, List<Any?>>, val body: Pet ): Response200<Pet> { override val status = 200; override val content = Wirespec.Content("application/xml", body)}
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: Pet ): Response200<Pet> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}
  data class Response404Unit (override val headers: Map<String, List<Any?>> ): Response404<Unit> { override val status = 404; override val content = null}
  data class Response405Unit (override val headers: Map<String, List<Any?>> ): Response405<Unit> { override val status = 405; override val content = null}

  suspend fun updatePet(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/pet"
    const val METHOD = "PUT"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content?.type == "application/json" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ Response200ApplicationXml(response.headers, it.body) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.status == 400 && response.content == null -> Response400Unit(response.headers)
        response.status == 404 && response.content == null -> Response404Unit(response.headers)
        response.status == 405 && response.content == null -> Response405Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface FindPetsByStatusEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(status: FindPetsByStatusParameterStatus? = null): this(path = "/pet/findByStatus", method = Wirespec.Method.GET, query = mapOf<String, List<Any?>>("status" to listOf(status)), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  sealed interface Response400<T>: Response4XX<T>
  data class Response200ApplicationXml (override val headers: Map<String, List<Any?>>, val body: List<Pet> ): Response200<List<Pet>> { override val status = 200; override val content = Wirespec.Content("application/xml", body)}
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: List<Pet> ): Response200<List<Pet>> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}

  suspend fun findPetsByStatus(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/pet/findByStatus"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<List<Pet>>(response.content!!, Wirespec.getType(Pet::class.java, true))
          .let{ Response200ApplicationXml(response.headers, it.body) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<List<Pet>>(response.content!!, Wirespec.getType(Pet::class.java, true))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.status == 400 && response.content == null -> Response400Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface FindPetsByTagsEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(tags: List<String>? = null): this(path = "/pet/findByTags", method = Wirespec.Method.GET, query = mapOf<String, List<Any?>>("tags" to listOf(tags)), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  sealed interface Response400<T>: Response4XX<T>
  data class Response200ApplicationXml (override val headers: Map<String, List<Any?>>, val body: List<Pet> ): Response200<List<Pet>> { override val status = 200; override val content = Wirespec.Content("application/xml", body)}
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: List<Pet> ): Response200<List<Pet>> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}

  suspend fun findPetsByTags(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/pet/findByTags"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<List<Pet>>(response.content!!, Wirespec.getType(Pet::class.java, true))
          .let{ Response200ApplicationXml(response.headers, it.body) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<List<Pet>>(response.content!!, Wirespec.getType(Pet::class.java, true))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.status == 400 && response.content == null -> Response400Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface GetPetByIdEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(petId: Long): this(path = "/pet/${petId}", method = Wirespec.Method.GET, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  sealed interface Response400<T>: Response4XX<T>
  sealed interface Response404<T>: Response4XX<T>
  data class Response200ApplicationXml (override val headers: Map<String, List<Any?>>, val body: Pet ): Response200<Pet> { override val status = 200; override val content = Wirespec.Content("application/xml", body)}
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: Pet ): Response200<Pet> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}
  data class Response404Unit (override val headers: Map<String, List<Any?>> ): Response404<Unit> { override val status = 404; override val content = null}

  suspend fun getPetById(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/pet/{petId}"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ Response200ApplicationXml(response.headers, it.body) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.status == 400 && response.content == null -> Response400Unit(response.headers)
        response.status == 404 && response.content == null -> Response404Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface UpdatePetWithFormEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(petId: Long, name: String? = null, status: String? = null): this(path = "/pet/${petId}", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>("name" to listOf(name), "status" to listOf(status)), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response405<T>: Response4XX<T>
  data class Response405Unit (override val headers: Map<String, List<Any?>> ): Response405<Unit> { override val status = 405; override val content = null}

  suspend fun updatePetWithForm(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/pet/{petId}"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 405 && response.content == null -> Response405Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface DeletePetEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(petId: Long, api_key: String? = null): this(path = "/pet/${petId}", method = Wirespec.Method.DELETE, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>("api_key" to listOf(api_key)), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response400<T>: Response4XX<T>
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}

  suspend fun deletePet(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/pet/{petId}"
    const val METHOD = "DELETE"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 400 && response.content == null -> Response400Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface UploadFileEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestApplicationOctetStream(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<String>?) : Request<String> { constructor(petId: Long, additionalMetadata: String? = null, body: String): this(path = "/pet/${petId}/uploadImage", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>("additionalMetadata" to listOf(additionalMetadata)), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/octet-stream", body))}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: ApiResponse ): Response200<ApiResponse> { override val status = 200; override val content = Wirespec.Content("application/json", body)}

  suspend fun uploadFile(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/pet/{petId}/uploadImage"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content?.type == "application/octet-stream" -> contentMapper
          .read<String>(request.content!!, Wirespec.getType(String::class.java, false))
          .let{ RequestApplicationOctetStream(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<ApiResponse>(response.content!!, Wirespec.getType(ApiResponse::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface GetInventoryEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(): this(path = "/store/inventory", method = Wirespec.Method.GET, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: Map<String, Long> ): Response200<Map<String, Long>> { override val status = 200; override val content = Wirespec.Content("application/json", body)}

  suspend fun getInventory(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/store/inventory"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Map<String, Long>>(response.content!!, Wirespec.getType(Long::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface PlaceOrderEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestApplicationJson(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Order>?) : Request<Order> { constructor(body: Order): this(path = "/store/order", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/json", body))}
  data class RequestApplicationXml(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Order>?) : Request<Order> { constructor(body: Order): this(path = "/store/order", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/xml", body))}
  data class RequestApplicationXWwwFormUrlencoded(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Order>?) : Request<Order> { constructor(body: Order): this(path = "/store/order", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/x-www-form-urlencoded", body))}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  sealed interface Response405<T>: Response4XX<T>
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: Order ): Response200<Order> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class Response405Unit (override val headers: Map<String, List<Any?>> ): Response405<Unit> { override val status = 405; override val content = null}

  suspend fun placeOrder(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/store/order"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content?.type == "application/json" -> contentMapper
          .read<Order>(request.content!!, Wirespec.getType(Order::class.java, false))
          .let{ RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<Order>(request.content!!, Wirespec.getType(Order::class.java, false))
          .let{ RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<Order>(request.content!!, Wirespec.getType(Order::class.java, false))
          .let{ RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Order>(response.content!!, Wirespec.getType(Order::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.status == 405 && response.content == null -> Response405Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface GetOrderByIdEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(orderId: Long): this(path = "/store/order/${orderId}", method = Wirespec.Method.GET, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  sealed interface Response400<T>: Response4XX<T>
  sealed interface Response404<T>: Response4XX<T>
  data class Response200ApplicationXml (override val headers: Map<String, List<Any?>>, val body: Order ): Response200<Order> { override val status = 200; override val content = Wirespec.Content("application/xml", body)}
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: Order ): Response200<Order> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}
  data class Response404Unit (override val headers: Map<String, List<Any?>> ): Response404<Unit> { override val status = 404; override val content = null}

  suspend fun getOrderById(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/store/order/{orderId}"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<Order>(response.content!!, Wirespec.getType(Order::class.java, false))
          .let{ Response200ApplicationXml(response.headers, it.body) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<Order>(response.content!!, Wirespec.getType(Order::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.status == 400 && response.content == null -> Response400Unit(response.headers)
        response.status == 404 && response.content == null -> Response404Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface DeleteOrderEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(orderId: Long): this(path = "/store/order/${orderId}", method = Wirespec.Method.DELETE, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response400<T>: Response4XX<T>
  sealed interface Response404<T>: Response4XX<T>
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}
  data class Response404Unit (override val headers: Map<String, List<Any?>> ): Response404<Unit> { override val status = 404; override val content = null}

  suspend fun deleteOrder(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/store/order/{orderId}"
    const val METHOD = "DELETE"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 400 && response.content == null -> Response400Unit(response.headers)
        response.status == 404 && response.content == null -> Response404Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface CreateUserEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestApplicationJson(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<User>?) : Request<User> { constructor(body: User): this(path = "/user", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/json", body))}
  data class RequestApplicationXml(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<User>?) : Request<User> { constructor(body: User): this(path = "/user", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/xml", body))}
  data class RequestApplicationXWwwFormUrlencoded(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<User>?) : Request<User> { constructor(body: User): this(path = "/user", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/x-www-form-urlencoded", body))}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface ResponseDefault<T>: Response<T>


  data class ResponseDefaultApplicationJson (override val status: Int, override val headers: Map<String, List<Any?>>, val body: User ): ResponseDefault<User> { override val content = Wirespec.Content("application/json", body)}
  data class ResponseDefaultApplicationXml (override val status: Int, override val headers: Map<String, List<Any?>>, val body: User ): ResponseDefault<User> { override val content = Wirespec.Content("application/xml", body)}
  suspend fun createUser(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/user"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content?.type == "application/json" -> contentMapper
          .read<User>(request.content!!, Wirespec.getType(User::class.java, false))
          .let{ RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<User>(request.content!!, Wirespec.getType(User::class.java, false))
          .let{ RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<User>(request.content!!, Wirespec.getType(User::class.java, false))
          .let{ RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {

        response.content?.type == "application/json" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let{ ResponseDefaultApplicationJson(response.status, response.headers, it.body) }
        response.content?.type == "application/xml" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let{ ResponseDefaultApplicationXml(response.status, response.headers, it.body) }
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface CreateUsersWithListInputEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestApplicationJson(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<List<User>>?) : Request<List<User>> { constructor(body: List<User>): this(path = "/user/createWithList", method = Wirespec.Method.POST, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/json", body))}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface ResponseDefault<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  data class Response200ApplicationXml (override val headers: Map<String, List<Any?>>, val body: User ): Response200<User> { override val status = 200; override val content = Wirespec.Content("application/xml", body)}
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: User ): Response200<User> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class ResponseDefaultUnit (override val status: Int, override val headers: Map<String, List<Any?>> ): ResponseDefault<Unit> { override val content = null}
  suspend fun createUsersWithListInput(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/user/createWithList"
    const val METHOD = "POST"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content?.type == "application/json" -> contentMapper
          .read<List<User>>(request.content!!, Wirespec.getType(User::class.java, true))
          .let{ RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let{ Response200ApplicationXml(response.headers, it.body) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.content == null -> ResponseDefaultUnit(response.status, response.headers)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface LoginUserEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(username: String? = null, password: String? = null): this(path = "/user/login", method = Wirespec.Method.GET, query = mapOf<String, List<Any?>>("username" to listOf(username), "password" to listOf(password)), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  sealed interface Response400<T>: Response4XX<T>
  data class Response200ApplicationXml (override val headers: Map<String, List<Any?>>, val body: String ): Response200<String> { override val status = 200; override val content = Wirespec.Content("application/xml", body)}
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: String ): Response200<String> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}

  suspend fun loginUser(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/user/login"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<String>(response.content!!, Wirespec.getType(String::class.java, false))
          .let{ Response200ApplicationXml(response.headers, it.body) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<String>(response.content!!, Wirespec.getType(String::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.status == 400 && response.content == null -> Response400Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface LogoutUserEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(): this(path = "/user/logout", method = Wirespec.Method.GET, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface ResponseDefault<T>: Response<T>


  data class ResponseDefaultUnit (override val status: Int, override val headers: Map<String, List<Any?>> ): ResponseDefault<Unit> { override val content = null}
  suspend fun logoutUser(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/user/logout"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {

        response.content == null -> ResponseDefaultUnit(response.status, response.headers)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface GetUserByNameEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(username: String): this(path = "/user/${username}", method = Wirespec.Method.GET, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response2XX<T>: Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response200<T>: Response2XX<T>
  sealed interface Response400<T>: Response4XX<T>
  sealed interface Response404<T>: Response4XX<T>
  data class Response200ApplicationXml (override val headers: Map<String, List<Any?>>, val body: User ): Response200<User> { override val status = 200; override val content = Wirespec.Content("application/xml", body)}
  data class Response200ApplicationJson (override val headers: Map<String, List<Any?>>, val body: User ): Response200<User> { override val status = 200; override val content = Wirespec.Content("application/json", body)}
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}
  data class Response404Unit (override val headers: Map<String, List<Any?>> ): Response404<Unit> { override val status = 404; override val content = null}

  suspend fun getUserByName(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/user/{username}"
    const val METHOD = "GET"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let{ Response200ApplicationXml(response.headers, it.body) }
        response.status == 200 && response.content?.type == "application/json" -> contentMapper
          .read<User>(response.content!!, Wirespec.getType(User::class.java, false))
          .let{ Response200ApplicationJson(response.headers, it.body) }
        response.status == 400 && response.content == null -> Response400Unit(response.headers)
        response.status == 404 && response.content == null -> Response404Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface UpdateUserEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestApplicationJson(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<User>?) : Request<User> { constructor(username: String, body: User): this(path = "/user/${username}", method = Wirespec.Method.PUT, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/json", body))}
  data class RequestApplicationXml(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<User>?) : Request<User> { constructor(username: String, body: User): this(path = "/user/${username}", method = Wirespec.Method.PUT, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/xml", body))}
  data class RequestApplicationXWwwFormUrlencoded(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<User>?) : Request<User> { constructor(username: String, body: User): this(path = "/user/${username}", method = Wirespec.Method.PUT, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = Wirespec.Content("application/x-www-form-urlencoded", body))}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface ResponseDefault<T>: Response<T>


  data class ResponseDefaultUnit (override val status: Int, override val headers: Map<String, List<Any?>> ): ResponseDefault<Unit> { override val content = null}
  suspend fun updateUser(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/user/{username}"
    const val METHOD = "PUT"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content?.type == "application/json" -> contentMapper
          .read<User>(request.content!!, Wirespec.getType(User::class.java, false))
          .let{ RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/xml" -> contentMapper
          .read<User>(request.content!!, Wirespec.getType(User::class.java, false))
          .let{ RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
          .read<User>(request.content!!, Wirespec.getType(User::class.java, false))
          .let{ RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {

        response.content == null -> ResponseDefaultUnit(response.status, response.headers)
        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
}

interface DeleteUserEndpoint: Wirespec.Endpoint {
  sealed interface Request<T>: Wirespec.Request<T>
  data class RequestUnit(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<Unit>?) : Request<Unit> { constructor(username: String): this(path = "/user/${username}", method = Wirespec.Method.DELETE, query = mapOf<String, List<Any?>>(), headers = mapOf<String, List<Any?>>(), content = null)}
  sealed interface Response<T>: Wirespec.Response<T>
  sealed interface Response4XX<T>: Response<T>
  sealed interface Response400<T>: Response4XX<T>
  sealed interface Response404<T>: Response4XX<T>
  data class Response400Unit (override val headers: Map<String, List<Any?>> ): Response400<Unit> { override val status = 400; override val content = null}
  data class Response404Unit (override val headers: Map<String, List<Any?>> ): Response404<Unit> { override val status = 404; override val content = null}

  suspend fun deleteUser(request: Request<*>): Response<*>
  companion object{
    const val PATH = "/user/{username}"
    const val METHOD = "DELETE"
    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      request: Wirespec.Request<B> -> when {
        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
        else -> error("Cannot map request")
      }
    }
    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
      response: Wirespec.Response<B> -> when {
        response.status == 400 && response.content == null -> Response400Unit(response.headers)
        response.status == 404 && response.content == null -> Response404Unit(response.headers)

        else -> error("Cannot map response with status ${response.status}")
      }
    }
  }
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
