package community.flock.wirespec.integration.spring.generated

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

interface AddPet {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestApplicationJson(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Pet>?
    ) : Request<Pet> {
        constructor(body: Pet) : this(
            path = "/pet",
            method = Wirespec.Method.POST,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/json", body)
        )
    }

    class RequestApplicationXml(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Pet>?
    ) : Request<Pet> {
        constructor(body: Pet) : this(
            path = "/pet",
            method = Wirespec.Method.POST,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/xml", body)
        )
    }

    class RequestApplicationXWwwFormUrlencoded(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Pet>?
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
    class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, body: Pet) : Response200<Pet> {
        override val status = 200;
        override val content = Wirespec.Content("application/xml", body)
    }

    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: Pet) : Response200<Pet> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class Response405Unit(override val headers: Map<String, List<Any?>>) : Response405<Unit> {
        override val status = 405;
        override val content = null
    }

    suspend fun addPet(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.POST
        const val PATH = "/pet"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content?.type == "application/json" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { RequestApplicationJson(path, method, query, headers, it) }

                    content?.type == "application/xml" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { RequestApplicationXml(path, method, query, headers, it) }

                    content?.type == "application/x-www-form-urlencoded" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { RequestApplicationXWwwFormUrlencoded(path, method, query, headers, it) }

                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/xml" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { Response200ApplicationXml(headers, it.body) }

                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    status == 405 && content == null -> Response405Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface UpdatePet {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestApplicationJson(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Pet>?
    ) : Request<Pet> {
        constructor(body: Pet) : this(
            path = "/pet",
            method = Wirespec.Method.PUT,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/json", body)
        )
    }

    class RequestApplicationXml(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Pet>?
    ) : Request<Pet> {
        constructor(body: Pet) : this(
            path = "/pet",
            method = Wirespec.Method.PUT,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/xml", body)
        )
    }

    class RequestApplicationXWwwFormUrlencoded(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Pet>?
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
    class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, body: Pet) : Response200<Pet> {
        override val status = 200;
        override val content = Wirespec.Content("application/xml", body)
    }

    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: Pet) : Response200<Pet> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    class Response404Unit(override val headers: Map<String, List<Any?>>) : Response404<Unit> {
        override val status = 404;
        override val content = null
    }

    class Response405Unit(override val headers: Map<String, List<Any?>>) : Response405<Unit> {
        override val status = 405;
        override val content = null
    }

    suspend fun updatePet(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.PUT
        const val PATH = "/pet"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content?.type == "application/json" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { RequestApplicationJson(path, method, query, headers, it) }

                    content?.type == "application/xml" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { RequestApplicationXml(path, method, query, headers, it) }

                    content?.type == "application/x-www-form-urlencoded" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { RequestApplicationXWwwFormUrlencoded(path, method, query, headers, it) }

                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/xml" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { Response200ApplicationXml(headers, it.body) }

                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    status == 400 && content == null -> Response400Unit(headers)
                    status == 404 && content == null -> Response404Unit(headers)
                    status == 405 && content == null -> Response405Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface FindPetsByStatus {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
    ) : Request<Unit> {
        constructor(status: FindPetsByStatusParameterStatus? = null) : this(
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
    class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, body: List<Pet>) :
        Response200<List<Pet>> {
        override val status = 200;
        override val content = Wirespec.Content("application/xml", body)
    }

    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: List<Pet>) :
        Response200<List<Pet>> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    suspend fun findPetsByStatus(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.GET
        const val PATH = "/pet/findByStatus"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/xml" -> contentMapper
                        .read<List<Pet>>(content, typeOf<List<Pet>>().javaType)
                        .let { Response200ApplicationXml(headers, it.body) }

                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<List<Pet>>(content, typeOf<List<Pet>>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    status == 400 && content == null -> Response400Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface FindPetsByTags {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
    ) : Request<Unit> {
        constructor(tags: List<String>? = null) : this(
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
    class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, body: List<Pet>) :
        Response200<List<Pet>> {
        override val status = 200;
        override val content = Wirespec.Content("application/xml", body)
    }

    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: List<Pet>) :
        Response200<List<Pet>> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    suspend fun findPetsByTags(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.GET
        const val PATH = "/pet/findByTags"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/xml" -> contentMapper
                        .read<List<Pet>>(content, typeOf<List<Pet>>().javaType)
                        .let { Response200ApplicationXml(headers, it.body) }

                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<List<Pet>>(content, typeOf<List<Pet>>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    status == 400 && content == null -> Response400Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface GetPetById {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
    ) : Request<Unit> {
        constructor(petId: Int) : this(
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
    class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, body: Pet) : Response200<Pet> {
        override val status = 200;
        override val content = Wirespec.Content("application/xml", body)
    }

    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: Pet) : Response200<Pet> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    class Response404Unit(override val headers: Map<String, List<Any?>>) : Response404<Unit> {
        override val status = 404;
        override val content = null
    }

    suspend fun getPetById(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.GET
        const val PATH = "/pet/{petId}"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/xml" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { Response200ApplicationXml(headers, it.body) }

                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<Pet>(content, typeOf<Pet>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    status == 400 && content == null -> Response400Unit(headers)
                    status == 404 && content == null -> Response404Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface UpdatePetWithForm {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
    ) : Request<Unit> {
        constructor(petId: Int, name: String? = null, status: String? = null) : this(
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
    class Response405Unit(override val headers: Map<String, List<Any?>>) : Response405<Unit> {
        override val status = 405;
        override val content = null
    }

    suspend fun updatePetWithForm(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.POST
        const val PATH = "/pet/{petId}"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 405 && content == null -> Response405Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface DeletePet {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
    ) : Request<Unit> {
        constructor(petId: Int, api_key: String? = null) : this(
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
    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    suspend fun deletePet(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.DELETE
        const val PATH = "/pet/{petId}"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 400 && content == null -> Response400Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface UploadFile {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestApplicationOctetStream(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<String>?
    ) : Request<String> {
        constructor(
            petId: Int,
            additionalMetadata: String? = null,
            body: String
        ) : this(
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
    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: ApiResponse) :
        Response200<ApiResponse> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    suspend fun uploadFile(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.POST
        const val PATH = "/pet/{petId}/uploadImage"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content?.type == "application/octet-stream" -> contentMapper
                        .read<String>(content, typeOf<String>().javaType)
                        .let { RequestApplicationOctetStream(path, method, query, headers, it) }

                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<ApiResponse>(content, typeOf<ApiResponse>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface GetInventory {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
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
    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: Map<String, Int>) :
        Response200<Map<String, Int>> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    suspend fun getInventory(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.GET
        const val PATH = "/store/inventory"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<Map<String, Int>>(content, typeOf<Map<String, Int>>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface PlaceOrder {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestApplicationJson(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Order>?
    ) : Request<Order> {
        constructor(body: Order) : this(
            path = "/store/order",
            method = Wirespec.Method.POST,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/json", body)
        )
    }

    class RequestApplicationXml(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Order>?
    ) : Request<Order> {
        constructor(body: Order) : this(
            path = "/store/order",
            method = Wirespec.Method.POST,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/xml", body)
        )
    }

    class RequestApplicationXWwwFormUrlencoded(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Order>?
    ) : Request<Order> {
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
    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: Order) : Response200<Order> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class Response405Unit(override val headers: Map<String, List<Any?>>) : Response405<Unit> {
        override val status = 405;
        override val content = null
    }

    suspend fun placeOrder(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.POST
        const val PATH = "/store/order"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content?.type == "application/json" -> contentMapper
                        .read<Order>(content, typeOf<Order>().javaType)
                        .let { RequestApplicationJson(path, method, query, headers, it) }

                    content?.type == "application/xml" -> contentMapper
                        .read<Order>(content, typeOf<Order>().javaType)
                        .let { RequestApplicationXml(path, method, query, headers, it) }

                    content?.type == "application/x-www-form-urlencoded" -> contentMapper
                        .read<Order>(content, typeOf<Order>().javaType)
                        .let { RequestApplicationXWwwFormUrlencoded(path, method, query, headers, it) }

                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<Order>(content, typeOf<Order>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    status == 405 && content == null -> Response405Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface GetOrderById {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
    ) : Request<Unit> {
        constructor(orderId: Int) : this(
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
    class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, body: Order) : Response200<Order> {
        override val status = 200;
        override val content = Wirespec.Content("application/xml", body)
    }

    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: Order) : Response200<Order> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    class Response404Unit(override val headers: Map<String, List<Any?>>) : Response404<Unit> {
        override val status = 404;
        override val content = null
    }

    suspend fun getOrderById(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.GET
        const val PATH = "/store/order/{orderId}"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/xml" -> contentMapper
                        .read<Order>(content, typeOf<Order>().javaType)
                        .let { Response200ApplicationXml(headers, it.body) }

                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<Order>(content, typeOf<Order>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    status == 400 && content == null -> Response400Unit(headers)
                    status == 404 && content == null -> Response404Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface DeleteOrder {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
    ) : Request<Unit> {
        constructor(orderId: Int) : this(
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
    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    class Response404Unit(override val headers: Map<String, List<Any?>>) : Response404<Unit> {
        override val status = 404;
        override val content = null
    }

    suspend fun deleteOrder(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.DELETE
        const val PATH = "/store/order/{orderId}"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 400 && content == null -> Response400Unit(headers)
                    status == 404 && content == null -> Response404Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface CreateUser {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestApplicationJson(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<User>?
    ) : Request<User> {
        constructor(body: User) : this(
            path = "/user",
            method = Wirespec.Method.POST,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/json", body)
        )
    }

    class RequestApplicationXml(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<User>?
    ) : Request<User> {
        constructor(body: User) : this(
            path = "/user",
            method = Wirespec.Method.POST,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/xml", body)
        )
    }

    class RequestApplicationXWwwFormUrlencoded(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<User>?
    ) : Request<User> {
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


    class ResponseDefaultApplicationJson(
        override val status: Int,
        override val headers: Map<String, List<Any?>>,
        body: User
    ) : ResponseDefault<User> {
        override val content = Wirespec.Content("application/json", body)
    }

    class ResponseDefaultApplicationXml(
        override val status: Int,
        override val headers: Map<String, List<Any?>>,
        body: User
    ) : ResponseDefault<User> {
        override val content = Wirespec.Content("application/xml", body)
    }

    suspend fun createUser(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.POST
        const val PATH = "/user"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content?.type == "application/json" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { RequestApplicationJson(path, method, query, headers, it) }

                    content?.type == "application/xml" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { RequestApplicationXml(path, method, query, headers, it) }

                    content?.type == "application/x-www-form-urlencoded" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { RequestApplicationXWwwFormUrlencoded(path, method, query, headers, it) }

                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {

                    content?.type == "application/json" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { ResponseDefaultApplicationJson(status, headers, it.body) }

                    content?.type == "application/xml" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { ResponseDefaultApplicationXml(status, headers, it.body) }

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface CreateUsersWithListInput {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestApplicationJson(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<List<User>>?
    ) : Request<List<User>> {
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
    class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, body: User) : Response200<User> {
        override val status = 200;
        override val content = Wirespec.Content("application/xml", body)
    }

    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: User) : Response200<User> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class ResponseDefaultUnit(override val status: Int, override val headers: Map<String, List<Any?>>) :
        ResponseDefault<Unit> {
        override val content = null
    }

    suspend fun createUsersWithListInput(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.POST
        const val PATH = "/user/createWithList"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content?.type == "application/json" -> contentMapper
                        .read<List<User>>(content, typeOf<List<User>>().javaType)
                        .let { RequestApplicationJson(path, method, query, headers, it) }

                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/xml" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { Response200ApplicationXml(headers, it.body) }

                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    content == null -> ResponseDefaultUnit(status, headers)
                    else -> error("Cannot map response with status $status")
                }
    }
}

interface LoginUser {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
    ) : Request<Unit> {
        constructor(username: String? = null, password: String? = null) : this(
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
    class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, body: String) : Response200<String> {
        override val status = 200;
        override val content = Wirespec.Content("application/xml", body)
    }

    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: String) :
        Response200<String> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    suspend fun loginUser(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.GET
        const val PATH = "/user/login"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/xml" -> contentMapper
                        .read<String>(content, typeOf<String>().javaType)
                        .let { Response200ApplicationXml(headers, it.body) }

                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<String>(content, typeOf<String>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    status == 400 && content == null -> Response400Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface LogoutUser {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
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


    class ResponseDefaultUnit(override val status: Int, override val headers: Map<String, List<Any?>>) :
        ResponseDefault<Unit> {
        override val content = null
    }

    suspend fun logoutUser(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.GET
        const val PATH = "/user/logout"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {

                    content == null -> ResponseDefaultUnit(status, headers)
                    else -> error("Cannot map response with status $status")
                }
    }
}

interface GetUserByName {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
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
    class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, body: User) : Response200<User> {
        override val status = 200;
        override val content = Wirespec.Content("application/xml", body)
    }

    class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, body: User) : Response200<User> {
        override val status = 200;
        override val content = Wirespec.Content("application/json", body)
    }

    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    class Response404Unit(override val headers: Map<String, List<Any?>>) : Response404<Unit> {
        override val status = 404;
        override val content = null
    }

    suspend fun getUserByName(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.GET
        const val PATH = "/user/{username}"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 200 && content?.type == "application/xml" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { Response200ApplicationXml(headers, it.body) }

                    status == 200 && content?.type == "application/json" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { Response200ApplicationJson(headers, it.body) }

                    status == 400 && content == null -> Response400Unit(headers)
                    status == 404 && content == null -> Response404Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

interface UpdateUser {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestApplicationJson(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<User>?
    ) : Request<User> {
        constructor(username: String, body: User) : this(
            path = "/user/${username}",
            method = Wirespec.Method.PUT,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/json", body)
        )
    }

    class RequestApplicationXml(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<User>?
    ) : Request<User> {
        constructor(username: String, body: User) : this(
            path = "/user/${username}",
            method = Wirespec.Method.PUT,
            query = mapOf<String, List<Any?>>(),
            headers = mapOf<String, List<Any?>>(),
            content = Wirespec.Content("application/xml", body)
        )
    }

    class RequestApplicationXWwwFormUrlencoded(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<User>?
    ) : Request<User> {
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


    class ResponseDefaultUnit(override val status: Int, override val headers: Map<String, List<Any?>>) :
        ResponseDefault<Unit> {
        override val content = null
    }

    suspend fun updateUser(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.PUT
        const val PATH = "/user/{username}"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content?.type == "application/json" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { RequestApplicationJson(path, method, query, headers, it) }

                    content?.type == "application/xml" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { RequestApplicationXml(path, method, query, headers, it) }

                    content?.type == "application/x-www-form-urlencoded" -> contentMapper
                        .read<User>(content, typeOf<User>().javaType)
                        .let { RequestApplicationXWwwFormUrlencoded(path, method, query, headers, it) }

                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {

                    content == null -> ResponseDefaultUnit(status, headers)
                    else -> error("Cannot map response with status $status")
                }
    }
}

interface DeleteUser {
    sealed interface Request<T> : Wirespec.Request<T>
    class RequestUnit(
        override val path: String,
        override val method: Wirespec.Method,
        override val query: Map<String, List<Any?>>,
        override val headers: Map<String, List<Any?>>,
        override val content: Wirespec.Content<Unit>?
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
    class Response400Unit(override val headers: Map<String, List<Any?>>) : Response400<Unit> {
        override val status = 400;
        override val content = null
    }

    class Response404Unit(override val headers: Map<String, List<Any?>>) : Response404<Unit> {
        override val status = 404;
        override val content = null
    }

    suspend fun deleteUser(request: Request<*>): Response<*>

    companion object {
        val METHOD = Wirespec.Method.DELETE
        const val PATH = "/user/{username}"
        fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(
                path: String,
                method: Wirespec.Method,
                query: Map<String, List<Any?>>,
                headers: Map<String, List<Any?>>,
                content: Wirespec.Content<B>?
            ) =
                when {
                    content == null -> RequestUnit(path, method, query, headers, null)
                    else -> error("Cannot map request")
                }

        fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) =
            fun(status: Int, headers: Map<String, List<Any?>>, content: Wirespec.Content<B>?) =
                when {
                    status == 400 && content == null -> Response400Unit(headers)
                    status == 404 && content == null -> Response404Unit(headers)

                    else -> error("Cannot map response with status $status")
                }
    }
}

enum class FindPetsByStatusParameterStatus(val label: String) {
    available("available"),
    pending("pending"),
    sold("sold");

    override fun toString(): String {
        return label
    }
}

data class Order(
    val id: Int? = null,
    val petId: Int? = null,
    val quantity: Int? = null,
    val shipDate: String? = null,
    val status: OrderStatus? = null,
    val complete: Boolean? = null
)

enum class OrderStatus(val label: String) {
    placed("placed"),
    approved("approved"),
    delivered("delivered");

    override fun toString(): String {
        return label
    }
}

data class Customer(
    val id: Int? = null,
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
    val id: Int? = null,
    val name: String? = null
)

data class User(
    val id: Int? = null,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val password: String? = null,
    val phone: String? = null,
    val userStatus: Int? = null
)

data class Tag(
    val id: Int? = null,
    val name: String? = null
)

data class Pet(
    val id: Int? = null,
    val name: String,
    val category: Category? = null,
    val photoUrls: List<String>,
    val tags: List<Tag>? = null,
    val status: PetStatus? = null
)

enum class PetStatus(val label: String) {
    available("available"),
    pending("pending"),
    sold("sold");

    override fun toString(): String {
        return label
    }
}

data class ApiResponse(
    val code: Int? = null,
    val type: String? = null,
    val message: String? = null
)
