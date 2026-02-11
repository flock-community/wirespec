package community.flock.wirespec.emitters.kotlin

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
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

class KotlinIrEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(KotlinIrEmitter())
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
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class UUID(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun toString(): String =
            |    value
            |}
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
            |      queries = mapOf("done" to serialization.serializeParam<Boolean>(request.queries.done, typeOf<Boolean>()), "name" to (request.queries.name?.let { serialization.serializeParam<String>(it, typeOf<String>()) } ?: listOf())),
            |      headers = mapOf("token" to serialization.serializeParam<Token>(request.headers.token, typeOf<Token>()), "refreshToken" to (request.headers.refreshToken?.let { serialization.serializeParam<Token>(it, typeOf<Token>()) } ?: listOf())),
            |      body = serialization.serializeBody<PotentialTodoDto>(request.body, typeOf<PotentialTodoDto>())
            |    )
            |  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
            |    Request(
            |      id = serialization.deserializePath<String>(request.path[1], typeOf<String>()),
            |      done = (request.queries["done"]?.let { serialization.deserializeParam<Boolean>(it, typeOf<Boolean>()) } ?: error("Param done cannot be null")),
            |      name = (request.queries["name"]?.let { serialization.deserializeParam<String>(it, typeOf<String>()) }),
            |      token = (request.headers["token"]?.let { serialization.deserializeParam<Token>(it, typeOf<Token>()) } ?: error("Param token cannot be null")),
            |      refreshToken = (request.headers["refreshToken"]?.let { serialization.deserializeParam<Token>(it, typeOf<Token>()) }),
            |      body = (request.body?.let { serialization.deserializeBody<PotentialTodoDto>(it, typeOf<PotentialTodoDto>()) } ?: error("body is null"))
            |    )
            |  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
            |    when(val r = response) {
            |        is Response200 -> {
            |          return Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = emptyMap(),
            |            body = serialization.serializeBody(r.body, typeOf<TodoDto>())
            |          )
            |        }
            |        is Response201 -> {
            |          return Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = mapOf("token" to serialization.serializeParam<Token>(r.headers.token, typeOf<Token>()), "refreshToken" to (r.headers.refreshToken?.let { serialization.serializeParam<Token>(it, typeOf<Token>()) } ?: listOf())),
            |            body = serialization.serializeBody(r.body, typeOf<TodoDto>())
            |          )
            |        }
            |        is Response500 -> {
            |          return Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = emptyMap(),
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
            |            token = (response.headers["token"]?.let { serialization.deserializeParam<Token>(it, typeOf<Token>()) } ?: error("Param token cannot be null")),
            |            refreshToken = (response.headers["refreshToken"]?.let { serialization.deserializeParam<Token>(it, typeOf<Token>()) }),
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
        """.trimMargin()

        CompileFullEndpointTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileChannelTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.channel
            |
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
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
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
        """.trimMargin()

        CompileEnumTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileMinimalEndpointTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.endpoint
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
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
            |      queries = emptyMap(),
            |      headers = emptyMap(),
            |      body = null
            |    )
            |  fun fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
            |    Request
            |  fun toRawResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse {
            |    when(val r = response) {
            |        is Response200 -> {
            |          return Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = emptyMap(),
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
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileRefinedTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoId(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TodoId.validate() = Regex(${"\"\"\""}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}${"\"\"\""}).matches(value)
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoNoRegex(
            |  override val value: String
            |) : Wirespec.Refined<String> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TodoNoRegex.validate() = true
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestInt(
            |  override val value: Long
            |) : Wirespec.Refined<Long> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TestInt.validate() = true
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestInt0(
            |  override val value: Long
            |) : Wirespec.Refined<Long> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TestInt0.validate() =   null < record.value < null;
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestInt1(
            |  override val value: Long
            |) : Wirespec.Refined<Long> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TestInt1.validate() =   0 < record.value < null;
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestInt2(
            |  override val value: Long
            |) : Wirespec.Refined<Long> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TestInt2.validate() =   3 < record.value < 1;
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestNum(
            |  override val value: Double
            |) : Wirespec.Refined<Double> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TestNum.validate() = true
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestNum0(
            |  override val value: Double
            |) : Wirespec.Refined<Double> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TestNum0.validate() =   null < record.value < null;
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestNum1(
            |  override val value: Double
            |) : Wirespec.Refined<Double> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TestNum1.validate() =   null < record.value < 0.5;
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestNum2(
            |  override val value: Double
            |) : Wirespec.Refined<Double> {
            |  override fun toString(): String =
            |    value
            |}
            |fun TestNum2.validate() =   -0.2 < record.value < 0.5;
            |
        """.trimMargin()

        CompileRefinedTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileUnionTest() {
        val kotlin = """
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

        CompileUnionTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileTypeTest() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |
            |data class Request(
            |  val type: String,
            |  val url: String,
            |  val bODY_TYPE: String?,
            |  val params: List<String>,
            |  val headers: Map<String, String>,
            |  val body: Map<String, List<String?>?>?
            |)
            |
        """.trimMargin()

        CompileTypeTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun sharedOutputTest() {
        val expected = """
            |package community.flock.wirespec.kotlin
            |import kotlin.reflect.KType
            |object Wirespec {
            |  interface Enum {
            |      val label: String
            |  }
            |  interface Endpoint
            |  interface Channel
            |  interface Refined<T: Any> {
            |      val value: T
            |  }
            |  interface Path
            |  interface Queries
            |  interface Headers
            |  interface Handler
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
            |  interface ServerEdge<Req : Request<*>, Res : Response<*>> {
            |      fun from(request: RawRequest): Req
            |      fun to(response: Res): RawResponse
            |  }
            |  interface ClientEdge<Req : Request<*>, Res : Response<*>> {
            |      fun to(request: Req): RawRequest
            |      fun from(response: RawResponse): Res
            |  }
            |  interface Client<Req : Request<*>, Res : Response<*>> {
            |      val pathTemplate: String
            |      val method: String
            |      fun client(serialization: Serialization): ClientEdge<Req, Res>
            |  }
            |  interface Server<Req : Request<*>, Res : Response<*>> {
            |      val pathTemplate: String
            |      val method: String
            |      fun server(serialization: Serialization): ServerEdge<Req, Res>
            |  }
            |}
            |
        """.trimMargin()

        val emitter = KotlinIrEmitter()
        emitter.shared!!.source shouldBe expected
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

