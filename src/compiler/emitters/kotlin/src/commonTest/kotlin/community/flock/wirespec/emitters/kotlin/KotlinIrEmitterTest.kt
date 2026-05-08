package community.flock.wirespec.emitters.kotlin

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class KotlinIrEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(KotlinIrEmitter())
    }

    @Test
    fun testEmitterType() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model
            |data class Todo(
            |  val name: String,
            |  val description: String?,
            |  val notes: List<String>,
            |  val done: Boolean
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
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
            |data object TodoWithoutProperties : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
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
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class UUID(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun validate(): Boolean =
            |    Regex(${"\"\"\""}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}${"\"\"\""}).matches(value)
            |  override fun toString(): String =
            |    value
            |}
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
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
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
    fun compileFullEndpointTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.endpoint
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.model.Error
            |object PutTodo : Wirespec.Endpoint {
            |  data class Path(
            |      val id: String
            |    ) : Wirespec.Path
            |  data class Queries(
            |      val done: Boolean,
            |      val name: String?
            |    ) : Wirespec.Queries
            |  data class RequestHeaders(
            |      val token: Token,
            |      val refreshToken: Token?
            |    ) : Wirespec.Request.Headers
            |  data class Request(
            |      override val path: Path,
            |      override val method: Wirespec.Method,
            |      override val queries: Queries,
            |      override val headers: RequestHeaders,
            |      override val body: PotentialTodoDto
            |    ) : Wirespec.Request<PotentialTodoDto> {
            |      constructor(id: String, done: Boolean, name: String?, token: Token, refreshToken: Token?, body: PotentialTodoDto) : this(Path(id = id), Wirespec.Method.PUT, Queries(
            |        done = done,
            |        name = name
            |      ), RequestHeaders(
            |        token = token,
            |        refreshToken = refreshToken
            |      ), body)
            |    }
            |  sealed interface Response<T: Any> : Wirespec.Response<T>
            |  sealed interface Response2XX<T: Any> : Response<T>
            |  sealed interface Response5XX<T: Any> : Response<T>
            |  sealed interface ResponseTodoDto : Response<TodoDto>
            |  sealed interface ResponseError : Response<Error>
            |  data class Response200(
            |      override val status: Int,
            |      override val headers: Headers,
            |      override val body: TodoDto
            |    ) : Response2XX<TodoDto>, ResponseTodoDto {
            |      constructor(body: TodoDto) : this(200, Headers, body)
            |      object Headers : Wirespec.Response.Headers
            |    }
            |  data class Response201(
            |      override val status: Int,
            |      override val headers: Headers,
            |      override val body: TodoDto
            |    ) : Response2XX<TodoDto>, ResponseTodoDto {
            |      constructor(token: Token, refreshToken: Token?, body: TodoDto) : this(201, Headers(
            |        token = token,
            |        refreshToken = refreshToken
            |      ), body)
            |      data class Headers(
            |            val token: Token,
            |            val refreshToken: Token?
            |          ) : Wirespec.Response.Headers
            |    }
            |  data class Response500(
            |      override val status: Int,
            |      override val headers: Headers,
            |      override val body: Error
            |    ) : Response5XX<Error>, ResponseError {
            |      constructor(body: Error) : this(500, Headers, body)
            |      object Headers : Wirespec.Response.Headers
            |    }
            |  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      method = request.method.name,
            |      path = listOf("todos", serialization.serializePath<String>(request.path.id, typeOf<String>())),
            |      queries = mapOf("done" to serialization.serializeParam<Boolean>(request.queries.done, typeOf<Boolean>()), "name" to (request.queries.name?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: emptyList<String>())),
            |      headers = mapOf("token" to serialization.serializeParam<Token>(request.headers.token, typeOf<Token>()), "Refresh-Token" to (request.headers.refreshToken?.let { serialization.serializeParam<Token>(it, typeOf<Token>()) } ?: emptyList<String>())),
            |      body = serialization.serializeBody<PotentialTodoDto>(request.body, typeOf<PotentialTodoDto>())
            |    )
            |  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
            |    Request(
            |      id = serialization.deserializePath<String>(request.path[1], typeOf<String>()),
            |      done = (request.queries["done"]?.let { serialization.deserializeParam<Boolean>(it, typeOf<Boolean>()) } ?: error("Param done cannot be null")),
            |      name = (request.queries["name"]?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
            |      token = (request.headers.entries.find { it.key.equals("token", ignoreCase = true) }?.value?.let { serialization.deserializeParam<Token>(it, typeOf<Token>()) } ?: error("Param token cannot be null")),
            |      refreshToken = (request.headers.entries.find { it.key.equals("Refresh-Token", ignoreCase = true) }?.value?.let { serialization.deserializeParam<Token>(it, typeOf<Token>()) }),
            |      body = (request.body?.let { serialization.deserializeBody<PotentialTodoDto>(it, typeOf<PotentialTodoDto>()) } ?: error("body is null"))
            |    )
            |  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
            |    when(val r = response) {
            |        is Response200 -> {
            |          return Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = emptyMap<String, List<String>>(),
            |            body = serialization.serializeBody(r.body, typeOf<TodoDto>())
            |          )
            |        }
            |        is Response201 -> {
            |          return Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = mapOf("token" to serialization.serializeParam<Token>(r.headers.token, typeOf<Token>()), "refreshToken" to (r.headers.refreshToken?.let { serialization.serializeParam<Token>(it, typeOf<Token>()) } ?: emptyList<String>())),
            |            body = serialization.serializeBody(r.body, typeOf<TodoDto>())
            |          )
            |        }
            |        is Response500 -> {
            |          return Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = emptyMap<String, List<String>>(),
            |            body = serialization.serializeBody(r.body, typeOf<Error>())
            |          )
            |        }
            |        else -> {
            |          error(("Cannot match response with status: " + response.status))
            |        }
            |    }
            |  }
            |  fun fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> {
            |    when (response.statusCode) {
            |        200 -> {
            |          return Response200(body = (response.body?.let { serialization.deserializeBody<TodoDto>(it, typeOf<TodoDto>()) } ?: error("body is null")))
            |        }
            |        201 -> {
            |          return Response201(
            |            token = (response.headers.entries.find { it.key.equals("token", ignoreCase = true) }?.value?.let { serialization.deserializeParam<Token>(it, typeOf<Token>()) } ?: error("Param token cannot be null")),
            |            refreshToken = (response.headers.entries.find { it.key.equals("refreshToken", ignoreCase = true) }?.value?.let { serialization.deserializeParam<Token>(it, typeOf<Token>()) }),
            |            body = (response.body?.let { serialization.deserializeBody<TodoDto>(it, typeOf<TodoDto>()) } ?: error("body is null"))
            |          )
            |        }
            |        500 -> {
            |          return Response500(body = (response.body?.let { serialization.deserializeBody<Error>(it, typeOf<Error>()) } ?: error("body is null")))
            |        }
            |        else -> {
            |          error(("Cannot match response with status: " + response.statusCode))
            |        }
            |    }
            |  }
            |  interface Handler : Wirespec.Handler {
            |      suspend fun putTodo(request: Request): Response<*>
            |      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |        override val pathTemplate = "/todos/{id}"
            |        override val method = "PUT"
            |        override fun server(serialization: Wirespec.Serialization) = object : Wirespec.ServerEdge<Request, Response<*>> {
            |          override fun from(request: Wirespec.RawRequest) = fromRawRequest(serialization, request)
            |          override fun to(response: Response<*>) = toRawResponse(serialization, response)
            |        }
            |        override fun client(serialization: Wirespec.Serialization) = object : Wirespec.ClientEdge<Request, Response<*>> {
            |          override fun to(request: Request) = toRawRequest(serialization, request)
            |          override fun from(response: Wirespec.RawResponse) = fromRawResponse(serialization, response)
            |        }
            |      }
            |  }
            |  interface Call : Wirespec.Call {
            |      suspend fun putTodo(id: String, done: Boolean, name: String?, token: Token, refreshToken: Token?, body: PotentialTodoDto): Response<*>
            |  }
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class PotentialTodoDto(
            |  val name: String,
            |  val done: Boolean
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class Token(
            |  val iss: String
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TodoDto(
            |  val id: String,
            |  val name: String,
            |  val done: Boolean
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class Error(
            |  val code: Long,
            |  val description: String
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.client
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.model.Error
            |import community.flock.wirespec.generated.endpoint.PutTodo
            |data class PutTodoClient(
            |  val serialization: Wirespec.Serialization,
            |  val transportation: Wirespec.Transportation
            |) : PutTodo.Call {
            |  override suspend fun putTodo(id: String, done: Boolean, name: String?, token: Token, refreshToken: Token?, body: PotentialTodoDto): PutTodo.Response<*> {
            |    val request = PutTodo.Request(
            |      id = id,
            |      done = done,
            |      name = name,
            |      token = token,
            |      refreshToken = refreshToken,
            |      body = body
            |    )
            |    val rawRequest = PutTodo.toRawRequest(serialization, request)
            |    val rawResponse = transportation.transport(rawRequest)
            |    return PutTodo.fromRawResponse(serialization, rawResponse)
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |object PotentialTodoDtoGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): PotentialTodoDto =
            |    PotentialTodoDto(
            |      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      done = generator.generate(path + listOf("done"), Wirespec.GeneratorFieldBoolean(annotations = emptyList<Map<String, Any>>()))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Token
            |object TokenGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Token =
            |    Token(iss = generator.generate(path + listOf("iss"), Wirespec.GeneratorFieldString(
            |      regex = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TodoDto
            |object TodoDtoGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TodoDto =
            |    TodoDto(
            |      id = generator.generate(path + listOf("id"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      done = generator.generate(path + listOf("done"), Wirespec.GeneratorFieldBoolean(annotations = emptyList<Map<String, Any>>()))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Error
            |object ErrorGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Error =
            |    Error(
            |      code = generator.generate(path + listOf("code"), Wirespec.GeneratorFieldInteger(
            |        min = null,
            |        max = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      description = generator.generate(path + listOf("description"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      ))
            |    )
            |}
            |
            |package community.flock.wirespec.generated
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.model.Error
            |import community.flock.wirespec.generated.endpoint.PutTodo
            |import community.flock.wirespec.generated.client.PutTodoClient
            |data class Client(
            |  val serialization: Wirespec.Serialization,
            |  val transportation: Wirespec.Transportation
            |) : PutTodo.Call {
            |  override suspend fun putTodo(id: String, done: Boolean, name: String?, token: Token, refreshToken: Token?, body: PotentialTodoDto): PutTodo.Response<*> =
            |    PutTodoClient(
            |      serialization = serialization,
            |      transportation = transportation
            |    ).putTodo(id, done, name, token, refreshToken, body)
            |}
            |
            """.trimMargin()

        CompileFullEndpointTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileChannelTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.channel
            |interface Queue : Wirespec.Channel {
            |  fun invoke(message: String)
            |}
            |
        """.trimMargin()

        CompileChannelTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileEnumTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |enum class MyAwesomeEnum (override val label: String): Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE"),
            |  UnitedKingdom("UnitedKingdom"),
            |  _1("-1"),
            |  _0("0"),
            |  _10("10"),
            |  _999("-999"),
            |  _88("88");
            |  override fun toString(): String {
            |    return label
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.MyAwesomeEnum
            |object MyAwesomeEnumGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): MyAwesomeEnum =
            |    MyAwesomeEnum.valueOf(generator.generate(path + listOf("value"), Wirespec.GeneratorFieldEnum(
            |      values = listOf("ONE", "Two", "THREE_MORE", "UnitedKingdom", "-1", "0", "10", "-999", "88"),
            |      annotations = emptyList<Map<String, Any>>(),
            |      type = typeOf<MyAwesomeEnum>()
            |    )))
            |}
            |
            """.trimMargin()

        CompileEnumTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileMinimalEndpointTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.endpoint
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TodoDto
            |object GetTodos : Wirespec.Endpoint {
            |  object Path : Wirespec.Path
            |  object Queries : Wirespec.Queries
            |  object RequestHeaders : Wirespec.Request.Headers
            |  data object Request : Wirespec.Request<Unit> {
            |      override val path: Path = Path
            |      override val method: Wirespec.Method = Wirespec.Method.GET
            |      override val queries: Queries = Queries
            |      override val headers: RequestHeaders = RequestHeaders
            |      override val body: Unit = Unit  }
            |  sealed interface Response<T: Any> : Wirespec.Response<T>
            |  sealed interface Response2XX<T: Any> : Response<T>
            |  sealed interface ResponseListTodoDto : Response<List<TodoDto>>
            |  data class Response200(
            |      override val status: Int,
            |      override val headers: Headers,
            |      override val body: List<TodoDto>
            |    ) : Response2XX<List<TodoDto>>, ResponseListTodoDto {
            |      constructor(body: List<TodoDto>) : this(200, Headers, body)
            |      object Headers : Wirespec.Response.Headers
            |    }
            |  fun toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      method = request.method.name,
            |      path = listOf("todos"),
            |      queries = emptyMap<String, List<String>>(),
            |      headers = emptyMap<String, List<String>>(),
            |      body = null
            |    )
            |  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
            |    Request
            |  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
            |    when(val r = response) {
            |        is Response200 -> {
            |          return Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = emptyMap<String, List<String>>(),
            |            body = serialization.serializeBody(r.body, typeOf<List<TodoDto>>())
            |          )
            |        }
            |        else -> {
            |          error(("Cannot match response with status: " + response.status))
            |        }
            |    }
            |  }
            |  fun fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> {
            |    when (response.statusCode) {
            |        200 -> {
            |          return Response200(body = (response.body?.let { serialization.deserializeBody<List<TodoDto>>(it, typeOf<List<TodoDto>>()) } ?: error("body is null")))
            |        }
            |        else -> {
            |          error(("Cannot match response with status: " + response.statusCode))
            |        }
            |    }
            |  }
            |  interface Handler : Wirespec.Handler {
            |      suspend fun getTodos(request: Request): Response<*>
            |      companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |        override val pathTemplate = "/todos"
            |        override val method = "GET"
            |        override fun server(serialization: Wirespec.Serialization) = object : Wirespec.ServerEdge<Request, Response<*>> {
            |          override fun from(request: Wirespec.RawRequest) = fromRawRequest(serialization, request)
            |          override fun to(response: Response<*>) = toRawResponse(serialization, response)
            |        }
            |        override fun client(serialization: Wirespec.Serialization) = object : Wirespec.ClientEdge<Request, Response<*>> {
            |          override fun to(request: Request) = toRawRequest(serialization, request)
            |          override fun from(response: Wirespec.RawResponse) = fromRawResponse(serialization, response)
            |        }
            |      }
            |  }
            |  interface Call : Wirespec.Call {
            |      suspend fun getTodos(): Response<*>
            |  }
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TodoDto(
            |  val description: String
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.client
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.endpoint.GetTodos
            |data class GetTodosClient(
            |  val serialization: Wirespec.Serialization,
            |  val transportation: Wirespec.Transportation
            |) : GetTodos.Call {
            |  override suspend fun getTodos(): GetTodos.Response<*> {
            |    val request = GetTodos.Request
            |    val rawRequest = GetTodos.toRawRequest(serialization, request)
            |    val rawResponse = transportation.transport(rawRequest)
            |    return GetTodos.fromRawResponse(serialization, rawResponse)
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TodoDto
            |object TodoDtoGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TodoDto =
            |    TodoDto(description = generator.generate(path + listOf("description"), Wirespec.GeneratorFieldString(
            |      regex = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.endpoint.GetTodos
            |import community.flock.wirespec.generated.client.GetTodosClient
            |data class Client(
            |  val serialization: Wirespec.Serialization,
            |  val transportation: Wirespec.Transportation
            |) : GetTodos.Call {
            |  override suspend fun getTodos(): GetTodos.Response<*> =
            |    GetTodosClient(
            |      serialization = serialization,
            |      transportation = transportation
            |    ).getTodos()
            |}
            |
            """.trimMargin()

        CompileMinimalEndpointTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileRefinedTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TodoId(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun validate(): Boolean =
            |    Regex(${"\"\"\""}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}${"\"\"\""}).matches(value)
            |  override fun toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TodoNoRegex(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun validate(): Boolean =
            |    true
            |  override fun toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TestInt(
            |  override val value: Long
            |) : Wirespec.Refined<Long> {
            |  override fun validate(): Boolean =
            |    true
            |  override fun toString(): String =
            |    value.toString()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TestInt0(
            |  override val value: Long
            |) : Wirespec.Refined<Long> {
            |  override fun validate(): Boolean =
            |    true
            |  override fun toString(): String =
            |    value.toString()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TestInt1(
            |  override val value: Long
            |) : Wirespec.Refined<Long> {
            |  override fun validate(): Boolean =
            |    0 <= value
            |  override fun toString(): String =
            |    value.toString()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TestInt2(
            |  override val value: Long
            |) : Wirespec.Refined<Long> {
            |  override fun validate(): Boolean =
            |    1 <= value && value <= 3
            |  override fun toString(): String =
            |    value.toString()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TestNum(
            |  override val value: Double
            |) : Wirespec.Refined<Double> {
            |  override fun validate(): Boolean =
            |    true
            |  override fun toString(): String =
            |    value.toString()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TestNum0(
            |  override val value: Double
            |) : Wirespec.Refined<Double> {
            |  override fun validate(): Boolean =
            |    true
            |  override fun toString(): String =
            |    value.toString()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TestNum1(
            |  override val value: Double
            |) : Wirespec.Refined<Double> {
            |  override fun validate(): Boolean =
            |    value <= 0.5
            |  override fun toString(): String =
            |    value.toString()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class TestNum2(
            |  override val value: Double
            |) : Wirespec.Refined<Double> {
            |  override fun validate(): Boolean =
            |    -0.2 <= value && value <= 0.5
            |  override fun toString(): String =
            |    value.toString()
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TodoId
            |object TodoIdGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TodoId =
            |    TodoId(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldString(
            |      regex = "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\${'$'}",
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TodoNoRegex
            |object TodoNoRegexGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TodoNoRegex =
            |    TodoNoRegex(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldString(
            |      regex = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TestInt
            |object TestIntGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TestInt =
            |    TestInt(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldInteger(
            |      min = null,
            |      max = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TestInt0
            |object TestInt0Generator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TestInt0 =
            |    TestInt0(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldInteger(
            |      min = null,
            |      max = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TestInt1
            |object TestInt1Generator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TestInt1 =
            |    TestInt1(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldInteger(
            |      min = 0,
            |      max = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TestInt2
            |object TestInt2Generator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TestInt2 =
            |    TestInt2(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldInteger(
            |      min = 1,
            |      max = 3,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TestNum
            |object TestNumGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TestNum =
            |    TestNum(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldNumber(
            |      min = null,
            |      max = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TestNum0
            |object TestNum0Generator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TestNum0 =
            |    TestNum0(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldNumber(
            |      min = null,
            |      max = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TestNum1
            |object TestNum1Generator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TestNum1 =
            |    TestNum1(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldNumber(
            |      min = null,
            |      max = 0.5,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.TestNum2
            |object TestNum2Generator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): TestNum2 =
            |    TestNum2(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldNumber(
            |      min = -0.2,
            |      max = 0.5,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            """.trimMargin()

        CompileRefinedTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileUnionTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |sealed interface UserAccount
            |
            |package community.flock.wirespec.generated.model
            |data class UserAccountPassword(
            |  val username: String,
            |  val password: String
            |) : Wirespec.Model, UserAccount {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.model
            |data class UserAccountToken(
            |  val token: String
            |) : Wirespec.Model, UserAccount {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.model
            |data class User(
            |  val username: String,
            |  val account: UserAccount
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.UserAccount
            |object UserAccountGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): UserAccount {
            |    val variant = generator.generate(path + listOf("variant"), Wirespec.GeneratorFieldUnion(
            |      variants = listOf("UserAccountPassword", "UserAccountToken"),
            |      annotations = emptyList<Map<String, Any>>(),
            |      type = typeOf<UserAccount>()
            |    ))
            |    when (variant) {
            |        "UserAccountPassword" -> {
            |          return UserAccountPasswordGenerator.generate(generator, path + listOf("UserAccountPassword"))
            |        }
            |        "UserAccountToken" -> {
            |          return UserAccountTokenGenerator.generate(generator, path + listOf("UserAccountToken"))
            |        }
            |    }
            |    error("Unknown variant")
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.UserAccountPassword
            |object UserAccountPasswordGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): UserAccountPassword =
            |    UserAccountPassword(
            |      username = generator.generate(path + listOf("username"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      password = generator.generate(path + listOf("password"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      ))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.UserAccountToken
            |object UserAccountTokenGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): UserAccountToken =
            |    UserAccountToken(token = generator.generate(path + listOf("token"), Wirespec.GeneratorFieldString(
            |      regex = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.User
            |import community.flock.wirespec.generated.model.UserAccount
            |object UserGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): User =
            |    User(
            |      username = generator.generate(path + listOf("username"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      account = generator.generate(path + listOf("account"), Wirespec.GeneratorFieldShape(
            |        annotations = emptyMap<String, List<Map<String, Any>>>(),
            |        generate = { p0 -> UserAccountGenerator.generate(generator, p0) },
            |        type = typeOf<UserAccount>()
            |      ))
            |    )
            |}
            |
            """.trimMargin()

        CompileUnionTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileTypeTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |data class Request(
            |  val type: String,
            |  val url: String,
            |  val BODY_TYPE: String?,
            |  val params: List<String>,
            |  val headers: Map<String, String>,
            |  val body: Map<String, List<String?>?>?
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Request
            |object RequestGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Request =
            |    Request(
            |      type = generator.generate(path + listOf("type"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      url = generator.generate(path + listOf("url"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      BODY_TYPE = generator.generate(path + listOf("BODY_TYPE"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )) })),
            |      params = generator.generate(path + listOf("params"), Wirespec.GeneratorFieldArray(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )) })),
            |      headers = generator.generate(path + listOf("headers"), Wirespec.GeneratorFieldDict(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )) })),
            |      body = generator.generate(path + listOf("body"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldDict(generate = { p1 -> generator.generate(p1, Wirespec.GeneratorFieldArray(generate = { p2 -> generator.generate(p2, Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )) })) })) }))
            |    )
            |}
            |
            """.trimMargin()

        CompileTypeTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileNestedTypeTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class DutchPostalCode(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun validate(): Boolean =
            |    Regex(${"\"\"\""}^([0-9]{4}[A-Z]{2})${'$'}${"\"\"\""}).matches(value)
            |  override fun toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class Address(
            |  val street: String,
            |  val houseNumber: Long,
            |  val postalCode: DutchPostalCode
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    if (!postalCode.validate()) listOf("postalCode") else emptyList<String>()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class Person(
            |  val name: String,
            |  val address: Address,
            |  val tags: List<String>
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    address.validate().map { e -> "address.${'$'}{e}" }
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.DutchPostalCode
            |object DutchPostalCodeGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): DutchPostalCode =
            |    DutchPostalCode(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldString(
            |      regex = "^([0-9]{4}[A-Z]{2})\${'$'}",
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Address
            |import community.flock.wirespec.generated.model.DutchPostalCode
            |object AddressGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Address =
            |    Address(
            |      street = generator.generate(path + listOf("street"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      houseNumber = generator.generate(path + listOf("houseNumber"), Wirespec.GeneratorFieldInteger(
            |        min = null,
            |        max = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      postalCode = generator.generate(path + listOf("postalCode"), Wirespec.GeneratorFieldShape(
            |        annotations = emptyMap<String, List<Map<String, Any>>>(),
            |        generate = { p0 -> DutchPostalCodeGenerator.generate(generator, p0) },
            |        type = typeOf<DutchPostalCode>()
            |      ))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Person
            |import community.flock.wirespec.generated.model.Address
            |object PersonGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Person =
            |    Person(
            |      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      address = generator.generate(path + listOf("address"), Wirespec.GeneratorFieldShape(
            |        annotations = mapOf("street" to emptyList<Map<String, Any>>(), "houseNumber" to emptyList<Map<String, Any>>(), "postalCode" to emptyList<Map<String, Any>>()),
            |        generate = { p0 -> AddressGenerator.generate(generator, p0) },
            |        type = typeOf<Address>()
            |      )),
            |      tags = generator.generate(path + listOf("tags"), Wirespec.GeneratorFieldArray(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )) }))
            |    )
            |}
            |
            """.trimMargin()

        CompileNestedTypeTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileComplexModelTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class Email(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun validate(): Boolean =
            |    Regex(${"\"\"\""}^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}${"\"\"\""}).matches(value)
            |  override fun toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class PhoneNumber(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun validate(): Boolean =
            |    Regex(${"\"\"\""}^\+[1-9]\d{1,14}${'$'}${"\"\"\""}).matches(value)
            |  override fun toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class Tag(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun validate(): Boolean =
            |    Regex(${"\"\"\""}^[a-z][a-z0-9-]{0,19}${'$'}${"\"\"\""}).matches(value)
            |  override fun toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class EmployeeAge(
            |  override val value: Long
            |) : Wirespec.Refined<Long> {
            |  override fun validate(): Boolean =
            |    18 <= value && value <= 65
            |  override fun toString(): String =
            |    value.toString()
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class ContactInfo(
            |  val email: Email,
            |  val phone: PhoneNumber?
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    (if (!email.validate()) listOf("email") else emptyList<String>()) + (phone?.let { if (!it.validate()) listOf("phone") else emptyList<String>() } ?: emptyList<String>())
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class Employee(
            |  val name: String,
            |  val age: EmployeeAge,
            |  val contactInfo: ContactInfo,
            |  val tags: List<Tag>
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    (if (!age.validate()) listOf("age") else emptyList<String>()) + contactInfo.validate().map { e -> "contactInfo.${'$'}{e}" } + tags.flatMapIndexed { i, el -> if (!el.validate()) listOf("tags[${'$'}{i}]") else emptyList<String>() }
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class Department(
            |  val name: String,
            |  val employees: List<Employee>
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    employees.flatMapIndexed { i, el -> el.validate().map { e -> "employees[${'$'}{i}].${'$'}{e}" } }
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |data class Company(
            |  val name: String,
            |  val departments: List<Department>
            |) : Wirespec.Model {
            |  override fun validate(): List<String> =
            |    departments.flatMapIndexed { i, el -> el.validate().map { e -> "departments[${'$'}{i}].${'$'}{e}" } }
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Email
            |object EmailGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Email =
            |    Email(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldString(
            |      regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\${'$'}",
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.PhoneNumber
            |object PhoneNumberGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): PhoneNumber =
            |    PhoneNumber(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldString(
            |      regex = "^\\+[1-9]\\d{1,14}\${'$'}",
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Tag
            |object TagGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Tag =
            |    Tag(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldString(
            |      regex = "^[a-z][a-z0-9-]{0,19}\${'$'}",
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.EmployeeAge
            |object EmployeeAgeGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): EmployeeAge =
            |    EmployeeAge(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldInteger(
            |      min = 18,
            |      max = 65,
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.ContactInfo
            |import community.flock.wirespec.generated.model.Email
            |import community.flock.wirespec.generated.model.PhoneNumber
            |object ContactInfoGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): ContactInfo =
            |    ContactInfo(
            |      email = generator.generate(path + listOf("email"), Wirespec.GeneratorFieldShape(
            |        annotations = emptyMap<String, List<Map<String, Any>>>(),
            |        generate = { p0 -> EmailGenerator.generate(generator, p0) },
            |        type = typeOf<Email>()
            |      )),
            |      phone = generator.generate(path + listOf("phone"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldShape(
            |        annotations = emptyMap<String, List<Map<String, Any>>>(),
            |        generate = { p1 -> PhoneNumberGenerator.generate(generator, p1) },
            |        type = typeOf<PhoneNumber>()
            |      )) }))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Employee
            |import community.flock.wirespec.generated.model.EmployeeAge
            |import community.flock.wirespec.generated.model.ContactInfo
            |import community.flock.wirespec.generated.model.Tag
            |object EmployeeGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Employee =
            |    Employee(
            |      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      age = generator.generate(path + listOf("age"), Wirespec.GeneratorFieldShape(
            |        annotations = emptyMap<String, List<Map<String, Any>>>(),
            |        generate = { p0 -> EmployeeAgeGenerator.generate(generator, p0) },
            |        type = typeOf<EmployeeAge>()
            |      )),
            |      contactInfo = generator.generate(path + listOf("contactInfo"), Wirespec.GeneratorFieldShape(
            |        annotations = mapOf("email" to emptyList<Map<String, Any>>(), "phone" to emptyList<Map<String, Any>>()),
            |        generate = { p0 -> ContactInfoGenerator.generate(generator, p0) },
            |        type = typeOf<ContactInfo>()
            |      )),
            |      tags = generator.generate(path + listOf("tags"), Wirespec.GeneratorFieldArray(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldShape(
            |        annotations = emptyMap<String, List<Map<String, Any>>>(),
            |        generate = { p1 -> TagGenerator.generate(generator, p1) },
            |        type = typeOf<Tag>()
            |      )) }))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Department
            |import community.flock.wirespec.generated.model.Employee
            |object DepartmentGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Department =
            |    Department(
            |      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      employees = generator.generate(path + listOf("employees"), Wirespec.GeneratorFieldArray(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldShape(
            |        annotations = mapOf("name" to emptyList<Map<String, Any>>(), "age" to emptyList<Map<String, Any>>(), "contactInfo" to emptyList<Map<String, Any>>(), "tags" to emptyList<Map<String, Any>>()),
            |        generate = { p1 -> EmployeeGenerator.generate(generator, p1) },
            |        type = typeOf<Employee>()
            |      )) }))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Company
            |import community.flock.wirespec.generated.model.Department
            |object CompanyGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Company =
            |    Company(
            |      name = generator.generate(path + listOf("name"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      departments = generator.generate(path + listOf("departments"), Wirespec.GeneratorFieldArray(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldShape(
            |        annotations = mapOf("name" to emptyList<Map<String, Any>>(), "employees" to emptyList<Map<String, Any>>()),
            |        generate = { p1 -> DepartmentGenerator.generate(generator, p1) },
            |        type = typeOf<Department>()
            |      )) }))
            |    )
            |}
            |
            """.trimMargin()

        CompileComplexModelTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun sharedOutputTest() {
        val expected = """
            |package community.flock.wirespec.kotlin
            |import kotlin.reflect.KType
            |object Wirespec {
            |  interface Model {
            |      fun validate(): List<String>
            |  }
            |  interface Enum {
            |      val label: String
            |  }
            |  interface Endpoint
            |  interface Channel
            |  interface Refined<T: Any> {
            |      val value: T
            |      fun validate(): Boolean
            |  }
            |  interface Path
            |  interface Queries
            |  interface Headers
            |  interface Handler
            |  interface Call
            |  enum class Method {
            |      GET,
            |      PUT,
            |      POST,
            |      DELETE,
            |      OPTIONS,
            |      HEAD,
            |      PATCH,
            |      TRACE
            |    }  interface Request<T: Any> {
            |      val path: Path
            |      val method: Method
            |      val queries: Queries
            |      val headers: Headers
            |      val body: T
            |      interface Headers
            |  }
            |  interface Response<T: Any> {
            |      val status: Int
            |      val headers: Headers
            |      val body: T
            |      interface Headers
            |  }
            |  interface BodySerializer {
            |      fun <T: Any> serializeBody(t: T, type: KType): ByteArray
            |  }
            |  interface BodyDeserializer {
            |      fun <T: Any> deserializeBody(raw: ByteArray, type: KType): T
            |  }
            |  interface BodySerialization : BodySerializer, BodyDeserializer
            |  interface PathSerializer {
            |      fun <T: Any> serializePath(t: T, type: KType): String
            |  }
            |  interface PathDeserializer {
            |      fun <T: Any> deserializePath(raw: String, type: KType): T
            |  }
            |  interface PathSerialization : PathSerializer, PathDeserializer
            |  interface ParamSerializer {
            |      fun <T: Any> serializeParam(value: T, type: KType): List<String>
            |  }
            |  interface ParamDeserializer {
            |      fun <T: Any> deserializeParam(values: List<String>, type: KType): T
            |  }
            |  interface ParamSerialization : ParamSerializer, ParamDeserializer
            |  interface Serializer : BodySerializer, PathSerializer, ParamSerializer
            |  interface Deserializer : BodyDeserializer, PathDeserializer, ParamDeserializer
            |  interface Serialization : Serializer, Deserializer
            |  data class RawRequest(
            |      val method: String,
            |      val path: List<String>,
            |      val queries: Map<String, List<String>>,
            |      val headers: Map<String, List<String>>,
            |      val body: ByteArray?
            |    )
            |  data class RawResponse(
            |      val statusCode: Int,
            |      val headers: Map<String, List<String>>,
            |      val body: ByteArray?
            |    )
            |  interface Transportation {
            |      suspend fun transport(request: RawRequest): RawResponse
            |  }
            |  sealed interface GeneratorField<T: Any?>
            |  data class GeneratorFieldString(
            |      val regex: String?,
            |      val annotations: List<Map<String, Any>>
            |    ) : GeneratorField<String>
            |  data class GeneratorFieldInteger(
            |      val min: Long?,
            |      val max: Long?,
            |      val annotations: List<Map<String, Any>>
            |    ) : GeneratorField<Long>
            |  data class GeneratorFieldNumber(
            |      val min: Double?,
            |      val max: Double?,
            |      val annotations: List<Map<String, Any>>
            |    ) : GeneratorField<Double>
            |  data class GeneratorFieldBoolean(
            |      val annotations: List<Map<String, Any>>
            |    ) : GeneratorField<Boolean>
            |  data class GeneratorFieldBytes(
            |      val annotations: List<Map<String, Any>>
            |    ) : GeneratorField<ByteArray>
            |  data class GeneratorFieldEnum(
            |      val values: List<String>,
            |      val annotations: List<Map<String, Any>>,
            |      val type: KType
            |    ) : GeneratorField<String>
            |  data class GeneratorFieldUnion(
            |      val variants: List<String>,
            |      val annotations: List<Map<String, Any>>,
            |      val type: KType
            |    ) : GeneratorField<String>
            |  data class GeneratorFieldArray<T: Any>(
            |      val generate: (List<String>) -> T
            |    ) : GeneratorField<List<T>>
            |  data class GeneratorFieldNullable<T: Any>(
            |      val generate: (List<String>) -> T
            |    ) : GeneratorField<T?>
            |  data class GeneratorFieldShape<T: Any>(
            |      val annotations: Map<String, List<Map<String, Any>>>,
            |      val generate: (List<String>) -> T,
            |      val type: KType
            |    ) : GeneratorField<T>
            |  data class GeneratorFieldDict<V: Any>(
            |      val generate: (List<String>) -> V
            |    ) : GeneratorField<Map<String, V>>
            |  interface Generator {
            |      fun <T: Any?> generate(path: List<String>, field: GeneratorField<T>): T
            |  }
            |  interface ServerEdge<Req: Request<*>, Res: Response<*>> {
            |      fun from(request: RawRequest): Req
            |      fun to(response: Res): RawResponse
            |  }
            |  interface ClientEdge<Req: Request<*>, Res: Response<*>> {
            |      fun to(request: Req): RawRequest
            |      fun from(response: RawResponse): Res
            |  }
            |  interface Client<Req: Request<*>, Res: Response<*>> {
            |      val pathTemplate: String
            |      val method: String
            |      fun client(serialization: Serialization): ClientEdge<Req, Res>
            |  }
            |  interface Server<Req: Request<*>, Res: Response<*>> {
            |      val pathTemplate: String
            |      val method: String
            |      fun server(serialization: Serialization): ServerEdge<Req, Res>
            |  }
            |}
            |
            """.trimMargin()

        val emitter = KotlinIrEmitter()
        emitter.shared.source shouldBe expected
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

    private fun emitGeneratorSource(node: Definition, fileNameSubstring: String): String {
        val emitter = KotlinIrEmitter()
        val ast = AST(
            nonEmptyListOf(
                Module(
                    FileUri(""),
                    nonEmptyListOf(node),
                ),
            ),
        )
        val emitted = emitter.emit(ast, noLogger)
        val match = emitted.toList().first { it.file.contains(fileNameSubstring) }
        return match.result
    }

    @Test
    fun testEmitGeneratorForType() {
        val address = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Address"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("street"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("number"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer(constraint = null),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Address
            |object AddressGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Address =
            |    Address(
            |      street = generator.generate(path + listOf("street"), Wirespec.GeneratorFieldString(
            |        regex = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      )),
            |      number = generator.generate(path + listOf("number"), Wirespec.GeneratorFieldInteger(
            |        min = null,
            |        max = null,
            |        annotations = emptyList<Map<String, Any>>()
            |      ))
            |    )
            |}
            |
            """.trimMargin()

        emitGeneratorSource(address, "AddressGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForEnum() {
        val color = Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Color"),
            entries = setOf("RED", "GREEN", "BLUE"),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Color
            |object ColorGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Color =
            |    Color.valueOf(generator.generate(path + listOf("value"), Wirespec.GeneratorFieldEnum(
            |      values = listOf("RED", "GREEN", "BLUE"),
            |      annotations = emptyList<Map<String, Any>>(),
            |      type = typeOf<Color>()
            |    )))
            |}
            |
            """.trimMargin()

        emitGeneratorSource(color, "ColorGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForUnion() {
        val shape = Union(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Shape"),
            entries = setOf(
                Reference.Custom(value = "Circle", isNullable = false),
                Reference.Custom(value = "Square", isNullable = false),
            ),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Shape
            |object ShapeGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Shape {
            |    val variant = generator.generate(path + listOf("variant"), Wirespec.GeneratorFieldUnion(
            |      variants = listOf("Circle", "Square"),
            |      annotations = emptyList<Map<String, Any>>(),
            |      type = typeOf<Shape>()
            |    ))
            |    when (variant) {
            |        "Circle" -> {
            |          return CircleGenerator.generate(generator, path + listOf("Circle"))
            |        }
            |        "Square" -> {
            |          return SquareGenerator.generate(generator, path + listOf("Square"))
            |        }
            |    }
            |    error("Unknown variant")
            |  }
            |}
            |
            """.trimMargin()

        emitGeneratorSource(shape, "ShapeGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForRefined() {
        val uuid = Refined(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("UUID"),
            reference = Reference.Primitive(
                type = Reference.Primitive.Type.String(
                    Reference.Primitive.Type.Constraint.RegExp("/^[0-9a-f]{8}${'$'}/g"),
                ),
                isNullable = false,
            ),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.UUID
            |object UUIDGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): UUID =
            |    UUID(value = generator.generate(path + listOf("value"), Wirespec.GeneratorFieldString(
            |      regex = "^[0-9a-f]{8}\${'$'}",
            |      annotations = emptyList<Map<String, Any>>()
            |    )))
            |}
            |
            """.trimMargin()

        emitGeneratorSource(uuid, "UUIDGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForArrayField() {
        val inventory = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Inventory"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("items"),
                        annotations = emptyList(),
                        reference = Reference.Iterable(
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(constraint = null),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Inventory
            |object InventoryGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Inventory =
            |    Inventory(items = generator.generate(path + listOf("items"), Wirespec.GeneratorFieldArray(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldInteger(
            |      min = null,
            |      max = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )) })))
            |}
            |
            """.trimMargin()

        emitGeneratorSource(inventory, "InventoryGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForDictField() {
        val lookup = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Lookup"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("entries"),
                        annotations = emptyList(),
                        reference = Reference.Dict(
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(constraint = null),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Lookup
            |object LookupGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Lookup =
            |    Lookup(entries = generator.generate(path + listOf("entries"), Wirespec.GeneratorFieldDict(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldInteger(
            |      min = null,
            |      max = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )) })))
            |}
            |
            """.trimMargin()

        emitGeneratorSource(lookup, "LookupGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForNullableField() {
        val person = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Person"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("nickname"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import community.flock.wirespec.generated.model.Person
            |object PersonGenerator {
            |  fun generate(generator: Wirespec.Generator, path: List<String>): Person =
            |    Person(nickname = generator.generate(path + listOf("nickname"), Wirespec.GeneratorFieldNullable(generate = { p0 -> generator.generate(p0, Wirespec.GeneratorFieldString(
            |      regex = null,
            |      annotations = emptyList<Map<String, Any>>()
            |    )) })))
            |}
            |
            """.trimMargin()

        emitGeneratorSource(person, "PersonGenerator") shouldBe expected
    }
}
