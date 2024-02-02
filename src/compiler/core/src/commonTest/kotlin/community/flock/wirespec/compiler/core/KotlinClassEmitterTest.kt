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
            |  }
            |}
        """.trimMargin()

        val emitter = KotlinClassEmitter()
        val res = emitter.emit(ClassModelFixture.endpointRequest).values.first()
        println(res)
        assertEquals(expected, res)
    }
}