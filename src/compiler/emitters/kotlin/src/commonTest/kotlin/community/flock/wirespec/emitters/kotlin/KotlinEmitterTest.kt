package community.flock.wirespec.emitters.kotlin

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class KotlinEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(KotlinEmitter())
    }

    @Test
    fun testEmitterType() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model
            |
            |data class Todo(
            |  val name: String,
            |  val description: String?,
            |  val notes: List<String>,
            |  val done: Boolean
            |)
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.type)
        res shouldBe expected
    }

    @Test
    fun testEmitterEmptyType() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model
            |
            |data object TodoWithoutProperties
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.emptyType)
        res shouldBe expected
    }

    @Test
    fun testEmitterRefined() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class UUID(override val value: String): Wirespec.Refined {
            |  override fun toString() = value
            |}
            |
            |fun UUID.validate() = Regex(${"\"\"\""}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}${"\"\"\""}).matches(value)
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.refined)
        res shouldBe expected
    }

    @Test
    fun testEmitterEnum() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |enum class TodoStatus (override val label: String): Wirespec.Enum {
            |  OPEN("OPEN"),
            |  IN_PROGRESS("IN_PROGRESS"),
            |  CLOSE("CLOSE");
            |  override fun toString(): String {
            |    return label
            |  }
            |}
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.enum)
        res shouldBe expected
    }

    @Test
    fun compileChannel() {
        val kotlin = """
            |package community.flock.wirespec.generated.channel
            |
            |
            |
            |interface QueueChannel {
            |   operator fun invoke(message: String)
            |}
            |
        """.trimMargin()

        CompileChannelTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileEnum() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |enum class MyAwesomeEnum (override val label: String): Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE"),
            |  UnitedKingdom("UnitedKingdom");
            |  override fun toString(): String {
            |    return label
            |  }
            |}
            |
        """.trimMargin()

        CompileEnumTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileFullEndpointTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.endpoint
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.model.Error
            |
            |object PutTodo : Wirespec.Endpoint {
            |  data class Path(
            |    val id: String,
            |  ) : Wirespec.Path
            |
            |  data class Queries(
            |    val done: Boolean,
            |    val name: String?,
            |  ) : Wirespec.Queries
            |
            |  data class Headers(
            |    val token: Token,
            |    val refreshToken: Token?,
            |  ) : Wirespec.Request.Headers
            |
            |  class Request(
            |    id: String,
            |    done: Boolean,     name: String?,
            |    token: Token,     refreshToken: Token?,
            |    override val body: PotentialTodoDto,
            |  ) : Wirespec.Request<PotentialTodoDto> {
            |    override val path = Path(id)
            |    override val method = Wirespec.Method.PUT
            |    override val queries = Queries(done, name)
            |    override val headers = Headers(token, refreshToken)
            |  }
            |
            |  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      path = listOf("todos", request.path.id.let{serialization.serialize(it, typeOf<String>())}),
            |      method = request.method.name,
            |      queries = (mapOf("done" to (request.queries.done?.let{ serialization.serializeParam(it, typeOf<Boolean>()) } ?: emptyList()))) + (mapOf("name" to (request.queries.name?.let{ serialization.serializeParam(it, typeOf<String?>()) } ?: emptyList()))),
            |      headers = (mapOf("token" to (request.headers.token?.let{ serialization.serializeParam(it, typeOf<Token>()) } ?: emptyList()))) + (mapOf("refreshToken" to (request.headers.refreshToken?.let{ serialization.serializeParam(it, typeOf<Token?>()) } ?: emptyList()))),
            |      body = serialization.serialize(request.body, typeOf<PotentialTodoDto>()),
            |    )
            |
            |  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
            |    Request(
            |      id = serialization.deserialize(request.path[1], typeOf<String>()),
            |      done = serialization.deserializeParam(requireNotNull(request.queries["done"]) { "done is null" }, typeOf<Boolean>()),       name = request.queries["name"]?.let{ serialization.deserializeParam(it, typeOf<String?>()) },
            |      token = serialization.deserializeParam(requireNotNull(request.headers["token"]) { "token is null" }, typeOf<Token>()),       refreshToken = request.headers["refreshToken"]?.let{ serialization.deserializeParam(it, typeOf<Token?>()) },
            |      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<PotentialTodoDto>()),
            |    )
            |
            |  sealed interface Response<T: Any> : Wirespec.Response<T>
            |
            |  sealed interface Response2XX<T: Any> : Response<T>
            |  sealed interface Response5XX<T: Any> : Response<T>
            |
            |  sealed interface ResponseTodoDto : Response<TodoDto>
            |  sealed interface ResponseError : Response<Error>
            |
            |  data class Response200(override val body: TodoDto) : Response2XX<TodoDto>, ResponseTodoDto {
            |    override val status = 200
            |    override val headers = ResponseHeaders
            |    data object ResponseHeaders : Wirespec.Response.Headers
            |  }
            |
            |  data class Response201(override val body: TodoDto, val token: Token, val refreshToken: Token?) : Response2XX<TodoDto>, ResponseTodoDto {
            |    override val status = 201
            |    override val headers = ResponseHeaders(token, refreshToken)
            |    data class ResponseHeaders(
            |      val token: Token,
            |      val refreshToken: Token?,
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
            |        headers = emptyMap(),
            |        body = serialization.serialize(response.body, typeOf<TodoDto>()),
            |      )
            |      is Response201 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = (mapOf("token" to (response.headers.token?.let{ serialization.serializeParam(it, typeOf<Token>()) } ?: emptyList()))) + (mapOf("refreshToken" to (response.headers.refreshToken?.let{ serialization.serializeParam(it, typeOf<Token?>()) } ?: emptyList()))),
            |        body = serialization.serialize(response.body, typeOf<TodoDto>()),
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
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<TodoDto>()),
            |      )
            |      201 -> Response201(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<TodoDto>()),
            |        token = serialization.deserializeParam(requireNotNull(response.headers["token"]) { "token is null" }, typeOf<Token>()),
            |        refreshToken = response.headers["refreshToken"]?.let{ serialization.deserializeParam(it, typeOf<Token?>()) }
            |      )
            |      500 -> Response500(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Error>()),
            |      )
            |      else -> error("Cannot match response with status: ${'$'}{response.statusCode}")
            |    }
            |
            |  interface Handler: Wirespec.Handler {
            |    suspend fun putTodo(request: Request): Response<*>
            |    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |      override val pathTemplate = "/todos/{id}"
            |      override val method = "PUT"
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
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class PotentialTodoDto(
            |  val name: String,
            |  val done: Boolean
            |)
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class Token(
            |  val iss: String
            |)
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoDto(
            |  val id: String,
            |  val name: String,
            |  val done: Boolean
            |)
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class Error(
            |  val code: Long,
            |  val description: String
            |)
            |
            |package community.flock.wirespec.generated.client
            |
            |import community.flock.wirespec.generated.endpoint.PutTodo
            |
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |
            |interface PutTodoClient {
            |  suspend fun putTodo(id: String, done: Boolean, name: String?, token: Token, refreshToken: Token?, body: PotentialTodoDto): PutTodo.Response<*>
            |}
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |
            |import community.flock.wirespec.generated.client.PutTodoClient
            |
            |import community.flock.wirespec.generated.endpoint.PutTodo
            |
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.model.Error
            |
            |interface AModule: 
            |  PutTodoClient
            |
            |interface All: 
            |  AModule
            |
            |open class Client(val serialization: Wirespec.Serialization<String>, val handler: (Wirespec.RawRequest) -> Wirespec.RawResponse ): All {
            |  override suspend fun putTodo(id: String, done: Boolean, name: String?, token: Token, refreshToken: Token?, body: PotentialTodoDto) = 
            |     PutTodo.Request(id, done, name, token, refreshToken, body)
            |       .let { req -> PutTodo.toRequest(serialization, req) }
            |       .let { rawReq -> handler(rawReq) }
            |       .let { rawRes -> PutTodo.fromResponse(serialization, rawRes) }
            |}
            |
        """.trimMargin()

        CompileFullEndpointTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileMinimalEndpoint() {
        val kotlin = """
            |package community.flock.wirespec.generated.endpoint
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |import community.flock.wirespec.generated.model.TodoDto
            |
            |object GetTodos : Wirespec.Endpoint {
            |  data object Path : Wirespec.Path
            |
            |  data object Queries : Wirespec.Queries
            |
            |  data object Headers : Wirespec.Request.Headers
            |
            |  object Request : Wirespec.Request<Unit> {
            |    override val path = Path
            |    override val method = Wirespec.Method.GET
            |    override val queries = Queries
            |    override val headers = Headers
            |    override val body = Unit
            |  }
            |
            |  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      path = listOf("todos"),
            |      method = request.method.name,
            |      queries = emptyMap(),
            |      headers = emptyMap(),
            |      body = null,
            |    )
            |
            |  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
            |    Request
            |
            |  sealed interface Response<T: Any> : Wirespec.Response<T>
            |
            |  sealed interface Response2XX<T: Any> : Response<T>
            |
            |  sealed interface ResponseListTodoDto : Response<List<TodoDto>>
            |
            |  data class Response200(override val body: List<TodoDto>) : Response2XX<List<TodoDto>>, ResponseListTodoDto {
            |    override val status = 200
            |    override val headers = ResponseHeaders
            |    data object ResponseHeaders : Wirespec.Response.Headers
            |  }
            |
            |  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
            |    when(response) {
            |      is Response200 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = emptyMap(),
            |        body = serialization.serialize(response.body, typeOf<List<TodoDto>>()),
            |      )
            |    }
            |
            |  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
            |    when (response.statusCode) {
            |      200 -> Response200(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<List<TodoDto>>()),
            |      )
            |      else -> error("Cannot match response with status: ${'$'}{response.statusCode}")
            |    }
            |
            |  interface Handler: Wirespec.Handler {
            |    suspend fun getTodos(request: Request): Response<*>
            |    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |      override val pathTemplate = "/todos"
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
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoDto(
            |  val description: String
            |)
            |
            |package community.flock.wirespec.generated.client
            |
            |import community.flock.wirespec.generated.endpoint.GetTodos
            |
            |
            |
            |interface GetTodosClient {
            |  suspend fun getTodos(): GetTodos.Response<*>
            |}
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |
            |import community.flock.wirespec.generated.client.GetTodosClient
            |
            |import community.flock.wirespec.generated.endpoint.GetTodos
            |
            |import community.flock.wirespec.generated.model.TodoDto
            |
            |interface AModule: 
            |  GetTodosClient
            |
            |interface All: 
            |  AModule
            |
            |open class Client(val serialization: Wirespec.Serialization<String>, val handler: (Wirespec.RawRequest) -> Wirespec.RawResponse ): All {
            |  override suspend fun getTodos() = 
            |     GetTodos.Request
            |       .let { req -> GetTodos.toRequest(serialization, req) }
            |       .let { rawReq -> handler(rawReq) }
            |       .let { rawRes -> GetTodos.fromResponse(serialization, rawRes) }
            |}
            |
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileRefinedTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoId(override val value: String): Wirespec.Refined {
            |  override fun toString() = value
            |}
            |
            |fun TodoId.validate() = Regex(""${'"'}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$""${'"'}).matches(value)
            |
        """.trimMargin()

        CompileRefinedTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileTypeTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |
            |data class Request(
            |  val type: String,
            |  val url: String,
            |  val BODY_TYPE: String?,
            |  val params: List<String>,
            |  val headers: Map<String, String>,
            |  val body: Map<String, List<String?>?>?
            |)
            |
        """.trimMargin()

        CompileTypeTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileUnionTest() {
        val expected = """
            |package community.flock.wirespec.generated.model
            |
            |sealed interface UserAccount
            |
            |package community.flock.wirespec.generated.model
            |
            |data class UserAccountPassword(
            |  val username: String,
            |  val password: String
            |) : UserAccount
            |
            |package community.flock.wirespec.generated.model
            |
            |data class UserAccountToken(
            |  val token: String
            |) : UserAccount
            |
            |package community.flock.wirespec.generated.model
            |
            |data class User(
            |  val username: String,
            |  val account: UserAccount
            |)
            |
        """.trimMargin()

        CompileUnionTest.compiler { KotlinEmitter() } shouldBeRight expected
    }

    private fun EmitContext.emitFirst(node: Definition) = emitters.map {
        val ast = AST(
            nonEmptyListOf(
                Module(
                    FileUri(""),
                    nonEmptyListOf(node),
                ),
            ),
        )
        it.emit(ast, logger).first().result
    }
}
