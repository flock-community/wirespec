package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.KotlinClassEmitter
import community.flock.wirespec.compiler.core.fixture.ClassModelFixture
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinClassEmitterTest {

    @Test
    fun testKotlinEmitter() {
        val expected = """
            |interface AddPetEndpoint : Wirespec.Endpoint {
            |  sealed interface Request<T> : Wirespec.Request<T>
            |  data class RequestApplicationJson(
            |    override val path: String,
            |    override val method: Wirespec.Method,
            |    override val query: Map<String, List<Any?>>,
            |    override val headers: Map<String, List<Any?>>,
            |    override val content: Wirespec.Content<Pet>?
            |  ) : Request<Pet> {
            |    constructor(body: Pet) : this(
            |      path = "/pet",
            |      method = Wirespec.Method.POST,
            |      query = mapOf<String, List<Any?>>(),
            |      headers = mapOf<String, List<Any?>>(),
            |      content = Wirespec.Content("application/json", body)
            |    )
            |  }
            |
            |  sealed interface Response<T> : Wirespec.Response<T>
            |  sealed interface Response2XX<T> : Response<T>
            |  sealed interface Response4XX<T> : Response<T>
            |  sealed interface Response200<T> : Response2XX<T>
            |  sealed interface Response405<T> : Response4XX<T>
            |  data class Response200ApplicationXml(override val headers: Map<String, List<Any?>>, val body: Pet) : Response200<Pet> {
            |    override val status = 200;
            |    override val content = Wirespec.Content("application/xml", body)
            |  }
            |  data class Response200ApplicationJson(override val headers: Map<String, List<Any?>>, val body: Pet) : Response200<Pet> {
            |    override val status = 200;
            |    override val content = Wirespec.Content("application/json", body)
            |  }
            |  data class Response405Unit(override val headers: Map<String, List<Any?>>) : Response405<Unit> {
            |    override val status = 405;
            |    override val content = null
            |  }
            |  companion object {
            |    const val PATH = "/pet"
            |    const val METHOD = "POST"
            |    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
            |      when {
            |        request.content?.type == "application/json" -> contentMapper
            |          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
            |          .let { RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
            |        request.content?.type == "application/xml" -> contentMapper
            |          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
            |          .let { RequestApplicationXml(request.path, request.method, request.query, request.headers, it) }
            |        request.content?.type == "application/x-www-form-urlencoded" -> contentMapper
            |          .read<Pet>(request.content!!, Wirespec.getType(Pet::class.java, false))
            |          .let { RequestApplicationXWwwFormUrlencoded(request.path, request.method, request.query, request.headers, it) }
            |        else -> error("Cannot map request")
            |      }
            |    }
            |    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
            |      when {
            |        response.status == 200 && response.content?.type == "application/xml" -> contentMapper
            |          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
            |          .let { Response200ApplicationXml(response.headers, it.body) }
            |        response.status == 200 && response.content?.type == "application/json" -> contentMapper
            |          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
            |          .let { Response200ApplicationJson(response.headers, it.body) }
            |        response.status == 405 && response.content == null -> Response405Unit(response.headers)
            |        else -> error("Cannot map response with status ${'$'}{response.status}")
            |      }
            |    }
            |  }
            |}
        """.trimMargin()

        val emitter = KotlinClassEmitter()
        val res = emitter.emit(ClassModelFixture.endpointRequest).values.first()
        println(res)
        assertEquals(expected, res)
    }
}