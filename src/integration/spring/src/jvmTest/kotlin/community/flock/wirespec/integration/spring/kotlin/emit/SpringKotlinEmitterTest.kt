package community.flock.wirespec.integration.spring.kotlin.emit

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals

class SpringKotlinEmitterTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    @Test
    fun `Should emit the full wirespec, and add annotation to the handler method`() {
        val path = Path("src/jvmTest/resources/todo.ws")
        val text = SystemFileSystem.source(path).buffered().readString()

        val ast = parse(text)
        val actual = SpringKotlinEmitter(PackageName("community.flock.wirespec.spring.test"))
            .emit(ast, noLogger)
            .joinToString("\n") { it.result }
        val expected = """
            |package community.flock.wirespec.spring.test.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoId(override val value: String): Wirespec.Refined {
            |  override fun toString() = value
            |}
            |
            |fun TodoId.validate() = Regex(""${'"'}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}""${'"'}).matches(value)
            |
            |package community.flock.wirespec.spring.test.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoDto(
            |  val id: TodoId,
            |  val name: String,
            |  val done: Boolean
            |)
            |
            |package community.flock.wirespec.spring.test.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class Error(
            |  val code: Long,
            |  val description: String
            |)
            |
            |package community.flock.wirespec.spring.test.endpoint
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |import community.flock.wirespec.spring.test.model.TodoDto
            |import community.flock.wirespec.spring.test.model.Error
            |
            |object GetTodos : Wirespec.Endpoint {
            |  data object Path : Wirespec.Path
            |
            |  data class Queries(
            |    val done: Boolean?,
            |  ) : Wirespec.Queries
            |
            |  data object Headers : Wirespec.Request.Headers
            |
            |  class Request(
            |    done: Boolean?
            |  ) : Wirespec.Request<Unit> {
            |    override val path = Path
            |    override val method = Wirespec.Method.GET
            |    override val queries = Queries(done)
            |    override val headers = Headers
            |    override val body = Unit
            |  }
            |
            |  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      path = listOf("api", "todos"),
            |      method = request.method.name,
            |      queries = (mapOf("done" to (request.queries.done?.let{ serialization.serializeParam(it, typeOf<Boolean?>()) } ?: emptyList()))),
            |      headers = emptyMap(),
            |      body = null,
            |    )
            |
            |  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
            |    Request(
            |      done = request.queries["done"]?.let{ serialization.deserializeParam(it, typeOf<Boolean?>()) }
            |    )
            |
            |  sealed interface Response<T: Any> : Wirespec.Response<T>
            |
            |  sealed interface Response2XX<T: Any> : Response<T>
            |  sealed interface Response5XX<T: Any> : Response<T>
            |
            |  sealed interface ResponseListTodoDto : Response<List<TodoDto>>
            |  sealed interface ResponseError : Response<Error>
            |
            |  data class Response200(override val body: List<TodoDto>, val total: Long) : Response2XX<List<TodoDto>>, ResponseListTodoDto {
            |    override val status = 200
            |    override val headers = ResponseHeaders(total)
            |    data class ResponseHeaders(
            |      val total: Long,
            |    ) : Wirespec.Response.Headers
            |  }
            |
            |  data class Response500(override val body: Error) : Response5XX<Error>, ResponseError {
            |    override val status = 500
            |    override val headers = ResponseHeaders
            |    data object ResponseHeaders : Wirespec.Response.Headers
            |  }
            |
            |  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
            |    when(response) {
            |      is Response200 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = (mapOf("total" to (response.headers.total?.let{ serialization.serializeParam(it, typeOf<Long>()) } ?: emptyList()))),
            |        body = serialization.serialize(response.body, typeOf<List<TodoDto>>()),
            |      )
            |      is Response500 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = emptyMap(),
            |        body = serialization.serialize(response.body, typeOf<Error>()),
            |      )
            |    }
            |
            |  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
            |    when (response.statusCode) {
            |      200 -> Response200(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<List<TodoDto>>()),
            |        total = serialization.deserializeParam(requireNotNull(response.headers["total"]) { "total is null" }, typeOf<Long>())
            |      )
            |      500 -> Response500(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Error>()),
            |      )
            |      else -> error("Cannot match response with status: ${"$"}{response.statusCode}")
            |    }
            |
            |  interface Handler: Wirespec.Handler {
            |    @org.springframework.web.bind.annotation.GetMapping("/api/todos")
            |    suspend fun getTodos(request: Request): Response<*>
            |
            |    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |      override val pathTemplate = "/api/todos"
            |      override val method = "GET"
            |      override fun server(serialization: Wirespec.Serialization<String>) = object : Wirespec.ServerEdge<Request, Response<*>> {
            |        override fun from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
            |        override fun to(response: Response<*>) = toResponse(serialization, response)
            |      }
            |      override fun client(serialization: Wirespec.Serialization<String>) = object : Wirespec.ClientEdge<Request, Response<*>> {
            |        override fun to(request: Request) = toRequest(serialization, request)
            |        override fun from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
            |      }
            |    }
            |  }
            |}
            |
            |package community.flock.wirespec.spring.test
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |
            |import community.flock.wirespec.spring.test.endpoint.GetTodos
            |
            |import community.flock.wirespec.spring.test.model.TodoId
            |import community.flock.wirespec.spring.test.model.TodoDto
            |import community.flock.wirespec.spring.test.model.Error
            |
            |class Client(val handler: (Wirespec.Request<*>) -> Wirespec.Response<*> ){
            |  suspend fun GetTodos(done: Boolean?) = 
            |     GetTodos.Request(done)
            |       .let{req -> handler(req) as GetTodos.Response<*> }
            |}
            |
        """.trimMargin()

        assertEquals(expected, actual)
    }
}
