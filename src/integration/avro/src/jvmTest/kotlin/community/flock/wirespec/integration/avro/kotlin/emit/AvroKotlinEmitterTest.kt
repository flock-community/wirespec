package community.flock.wirespec.integration.avro.kotlin.emit

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrowWithMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvroKotlinEmitterTest {

    private val emitter = AvroEmitter(PackageName("packageName"), EmitShared(true))

    @Test
    fun emitTypeFunctionBodyTest() {
        val type = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Identifier"),
            shape = Type.Shape(
                listOf(
                    Field(
                        identifier = FieldIdentifier(name = "name"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(type))))
        val expected = """
            |package packageName.avro
            |
            |import packageName.model.Identifier
            |
            |object IdentifierAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): Identifier {
            |    return Identifier(
            |      record.get(0).toString() as String
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: Identifier ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.name)
            |    return record
            |  }
            |
            |}
            |
        """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        println(actual.first().result)
        assertEquals(expected, actual.find { it.file == "packageName/avro/IdentifierAvro.kt" }?.result)
    }

    @Test
    fun emitEnumFunctionBodyTest() {
        val enum = Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Identifier"),
            entries = setOf("ONE", "TWO", "THREE"),
        )
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(enum))))
        val expected = """
            |package packageName.avro
            |
            |import packageName.model.Identifier
            |
            |object IdentifierAvro {
            |
            |  val SCHEMA: org.apache.avro.Schema = org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.EnumSymbol): Identifier {
            |    return Identifier.valueOf(record.toString())
            |  }
            |
            |  @JvmStatic
            |  fun to(model: Identifier): org.apache.avro.generic.GenericData.EnumSymbol {
            |    return org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, model.name)
            |  }
            |
            |}
            |
        """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        println(actual)
        assertEquals(expected, actual.find { it.file == "packageName/avro/IdentifierAvro.kt" }?.result)
    }

    @Test
    fun compileFullEndpointTest() {
        val result = CompileFullEndpointTest.compiler { emitter }
        val expect =
            """
            |package packageName.endpoint
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |import packageName.model.Token
            |import packageName.model.Token
            |import packageName.model.PotentialTodoDto
            |import packageName.model.TodoDto
            |import packageName.model.Error
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
            |    val RefreshToken: Token?,
            |  ) : Wirespec.Request.Headers
            |
            |  class Request(
            |    id: String,
            |    done: Boolean,     name: String?,
            |    token: Token,     RefreshToken: Token?,
            |    override val body: PotentialTodoDto,
            |  ) : Wirespec.Request<PotentialTodoDto> {
            |    override val path = Path(id)
            |    override val method = Wirespec.Method.PUT
            |    override val queries = Queries(done, name)
            |    override val headers = Headers(token, RefreshToken)
            |  }
            |
            |  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      path = listOf("todos", request.path.id.let{serialization.serializePath(it, typeOf<String>())}),
            |      method = request.method.name,
            |      queries = mapOf(
            |          "done" to request.queries.done?.let{ serialization.serializeParam(it, typeOf<Boolean>()) }.orEmpty(),
            |          "name" to request.queries.name?.let{ serialization.serializeParam(it, typeOf<String?>()) }.orEmpty()
            |        ),
            |      headers = mapOf(
            |          "token" to request.headers.token?.let{ serialization.serializeParam(it, typeOf<Token>()) }.orEmpty(),
            |          "Refresh-Token" to request.headers.RefreshToken?.let{ serialization.serializeParam(it, typeOf<Token?>()) }.orEmpty()
            |        ),
            |      body = serialization.serializeBody(request.body, typeOf<PotentialTodoDto>()),
            |    )
            |
            |  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
            |    Request(
            |      id = serialization.deserializePath(request.path[1], typeOf<String>()),
            |      done =
            |        request.queries
            |          .entries
            |          .find { it.key.equals("done", ignoreCase = false) }
            |          ?.let { serialization.deserializeParam(it.value, typeOf<Boolean>()) }
            |          ?: throw IllegalArgumentException("done is null"),
            |      name =
            |        request.queries
            |          .entries
            |          .find { it.key.equals("name", ignoreCase = false) }
            |          ?.let { serialization.deserializeParam(it.value, typeOf<String?>()) },
            |      token =
            |        request.headers
            |          .entries
            |          .find { it.key.equals("token", ignoreCase = true) }
            |          ?.let { serialization.deserializeParam(it.value, typeOf<Token>()) }
            |          ?: throw IllegalArgumentException("token is null"),
            |      RefreshToken =
            |        request.headers
            |          .entries
            |          .find { it.key.equals("Refresh-Token", ignoreCase = true) }
            |          ?.let { serialization.deserializeParam(it.value, typeOf<Token?>()) },
            |      body = serialization.deserializeBody(requireNotNull(request.body) { "body is null" }, typeOf<PotentialTodoDto>()),
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
            |  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
            |    when(response) {
            |      is Response200 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = emptyMap(),
            |        body = serialization.serializeBody(response.body, typeOf<TodoDto>()),
            |      )
            |      is Response201 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = mapOf(
            |          "token" to response.headers.token?.let{ serialization.serializeParam(it, typeOf<Token>()) }.orEmpty(),
            |          "refreshToken" to response.headers.refreshToken?.let{ serialization.serializeParam(it, typeOf<Token?>()) }.orEmpty()
            |        ),
            |        body = serialization.serializeBody(response.body, typeOf<TodoDto>()),
            |      )
            |      is Response500 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = emptyMap(),
            |        body = serialization.serializeBody(response.body, typeOf<Error>()),
            |      )
            |    }
            |
            |  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
            |    when (response.statusCode) {
            |      200 -> Response200(
            |        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<TodoDto>()),
            |      )
            |      201 -> Response201(
            |        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<TodoDto>()),
            |        token =
            |          response.headers
            |            .entries
            |            .find { it.key.equals("token", ignoreCase = true) }
            |            ?.let { serialization.deserializeParam(it.value, typeOf<Token>()) }
            |            ?: throw IllegalArgumentException("token is null"),
            |        refreshToken =
            |          response.headers
            |            .entries
            |            .find { it.key.equals("refreshToken", ignoreCase = true) }
            |            ?.let { serialization.deserializeParam(it.value, typeOf<Token?>()) }
            |      )
            |      500 -> Response500(
            |        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<Error>()),
            |      )
            |      else -> error("Cannot match response with status: ${'$'}{response.statusCode}")
            |    }
            |
            |  interface Handler: Wirespec.Handler {
            |    suspend fun putTodo(request: Request): Response<*>
            |    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |      override val pathTemplate = "/todos/{id}"
            |      override val method = "PUT"
            |      override fun server(serialization: Wirespec.Serialization) = object : Wirespec.ServerEdge<Request, Response<*>> {
            |        override fun from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
            |        override fun to(response: Response<*>) = toResponse(serialization, response)
            |      }
            |      override fun client(serialization: Wirespec.Serialization) = object : Wirespec.ClientEdge<Request, Response<*>> {
            |        override fun to(request: Request) = toRequest(serialization, request)
            |        override fun from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
            |      }
            |    }
            |  }
            |}
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class PotentialTodoDto(
            |  val name: String,
            |  val done: Boolean
            |)
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class Token(
            |  val iss: String
            |)
            |
            |package packageName.model
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
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class Error(
            |  val code: Long,
            |  val description: String
            |)
            |
            |package packageName.avro
            |
            |import packageName.model.PotentialTodoDto
            |
            |object PotentialTodoDtoAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"PotentialTodoDto\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"done\",\"type\":\"boolean\"}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): PotentialTodoDto {
            |    return PotentialTodoDto(
            |      record.get(0).toString() as String,
            |          record.get(1) as Boolean
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: PotentialTodoDto ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.name)
            |        record.put(1, model.done)
            |    return record
            |  }
            |
            |}
            |
            |package packageName.avro
            |
            |import packageName.model.Token
            |
            |object TokenAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Token\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"iss\",\"type\":\"string\"}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): Token {
            |    return Token(
            |      record.get(0).toString() as String
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: Token ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.iss)
            |    return record
            |  }
            |
            |}
            |
            |package packageName.avro
            |
            |import packageName.model.TodoDto
            |
            |object TodoDtoAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"TodoDto\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"done\",\"type\":\"boolean\"}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): TodoDto {
            |    return TodoDto(
            |      record.get(0).toString() as String,
            |          record.get(1).toString() as String,
            |          record.get(2) as Boolean
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: TodoDto ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.id)
            |        record.put(1, model.name)
            |        record.put(2, model.done)
            |    return record
            |  }
            |
            |}
            |
            |package packageName.avro
            |
            |import packageName.model.Error
            |
            |object ErrorAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Error\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"code\",\"type\":\"long\"},{\"name\":\"description\",\"type\":\"string\"}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): Error {
            |    return Error(
            |      record.get(0) as Long,
            |          record.get(1).toString() as String
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: Error ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.code)
            |        record.put(1, model.description)
            |    return record
            |  }
            |
            |}
            |
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileMinimalEndpointTest() {
        val result = CompileMinimalEndpointTest.compiler { emitter }
        val expect =
            """
            |package packageName.endpoint
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |import packageName.model.TodoDto
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
            |  fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      path = listOf("todos"),
            |      method = request.method.name,
            |      queries = emptyMap(),
            |      headers = emptyMap(),
            |      body = null,
            |    )
            |
            |  fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
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
            |  fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
            |    when(response) {
            |      is Response200 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = emptyMap(),
            |        body = serialization.serializeBody(response.body, typeOf<List<TodoDto>>()),
            |      )
            |    }
            |
            |  fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
            |    when (response.statusCode) {
            |      200 -> Response200(
            |        body = serialization.deserializeBody(requireNotNull(response.body) { "body is null" }, typeOf<List<TodoDto>>()),
            |      )
            |      else -> error("Cannot match response with status: ${'$'}{response.statusCode}")
            |    }
            |
            |  interface Handler: Wirespec.Handler {
            |    suspend fun getTodos(request: Request): Response<*>
            |    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |      override val pathTemplate = "/todos"
            |      override val method = "GET"
            |      override fun server(serialization: Wirespec.Serialization) = object : Wirespec.ServerEdge<Request, Response<*>> {
            |        override fun from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
            |        override fun to(response: Response<*>) = toResponse(serialization, response)
            |      }
            |      override fun client(serialization: Wirespec.Serialization) = object : Wirespec.ClientEdge<Request, Response<*>> {
            |        override fun to(request: Request) = toRequest(serialization, request)
            |        override fun from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
            |      }
            |    }
            |  }
            |}
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoDto(
            |  val description: String
            |)
            |
            |package packageName.avro
            |
            |import packageName.model.TodoDto
            |
            |object TodoDtoAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"TodoDto\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"description\",\"type\":\"string\"}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): TodoDto {
            |    return TodoDto(
            |      record.get(0).toString() as String
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: TodoDto ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.description)
            |    return record
            |  }
            |
            |}
            |
            """.trimMargin()

        result.shouldBeRight(expect)
    }

    @Test
    fun compileChannelTest() {
        val result = CompileChannelTest.compiler { emitter }
        val expect =
            """
            |package packageName.channel
            |
            |
            |
            |fun interface Queue {
            |   operator fun invoke(message: String)
            |}
            |
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileEnumTest() {
        val result = CompileEnumTest.compiler { emitter }
        val expect =
            """
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |enum class MyAwesomeEnum (override val label: String): Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE"),
            |  UnitedKingdom("UnitedKingdom"),
            |  __1("-1"),
            |  _0("0"),
            |  _10("10"),
            |  __999("-999"),
            |  _88("88");
            |  override fun toString(): String {
            |    return label
            |  }
            |}
            |
            |package packageName.avro
            |
            |import packageName.model.MyAwesomeEnum
            |
            |object MyAwesomeEnumAvro {
            |
            |  val SCHEMA: org.apache.avro.Schema = org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"MyAwesomeEnum\",\"symbols\":[\"ONE\",\"Two\",\"THREE_MORE\",\"UnitedKingdom\",\"-1\",\"0\",\"10\",\"-999\",\"88\"]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.EnumSymbol): MyAwesomeEnum {
            |    return MyAwesomeEnum.valueOf(record.toString())
            |  }
            |
            |  @JvmStatic
            |  fun to(model: MyAwesomeEnum): org.apache.avro.generic.GenericData.EnumSymbol {
            |    return org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, model.name)
            |  }
            |
            |}
            |
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileRefinedTest() {
        val result = CompileRefinedTest.compiler { emitter }
        val expect =
            """
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoId(override val value: String): Wirespec.Refined<String> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TodoId.validate() = Regex(""${'"'}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$""${'"'}).matches(value)
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoNoRegex(override val value: String): Wirespec.Refined<String> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TodoNoRegex.validate() = true
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestInt(override val value: Long): Wirespec.Refined<Long> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TestInt.validate() = true
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestInt0(override val value: Long): Wirespec.Refined<Long> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TestInt0.validate() = true
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestInt1(override val value: Long): Wirespec.Refined<Long> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TestInt1.validate() = 0 < value
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestInt2(override val value: Long): Wirespec.Refined<Long> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TestInt2.validate() = 3 < value && value < 1
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestNum(override val value: Double): Wirespec.Refined<Double> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TestNum.validate() = true
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestNum0(override val value: Double): Wirespec.Refined<Double> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TestNum0.validate() = true
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestNum1(override val value: Double): Wirespec.Refined<Double> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TestNum1.validate() = value < 0.5
            |
            |package packageName.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TestNum2(override val value: Double): Wirespec.Refined<Double> {
            |  override fun toString() = value.toString()
            |}
            |
            |fun TestNum2.validate() = -0.2 < value && value < 0.5
            |
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileUnionTest() {
        val result = CompileUnionTest.compiler { emitter }
        val expect =
            """
            |package packageName.model
            |
            |sealed interface UserAccount
            |
            |package packageName.model
            |
            |data class UserAccountPassword(
            |  val username: String,
            |  val password: String
            |) : UserAccount
            |
            |package packageName.model
            |
            |data class UserAccountToken(
            |  val token: String
            |) : UserAccount
            |
            |package packageName.model
            |
            |data class User(
            |  val username: String,
            |  val account: UserAccount
            |)
            |
            |package packageName.avro
            |
            |import packageName.model.UserAccountPassword
            |
            |object UserAccountPasswordAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"UserAccountPassword\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"username\",\"type\":\"string\"},{\"name\":\"password\",\"type\":\"string\"}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): UserAccountPassword {
            |    return UserAccountPassword(
            |      record.get(0).toString() as String,
            |          record.get(1).toString() as String
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: UserAccountPassword ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.username)
            |        record.put(1, model.password)
            |    return record
            |  }
            |
            |}
            |
            |package packageName.avro
            |
            |import packageName.model.UserAccountToken
            |
            |object UserAccountTokenAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"UserAccountToken\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"token\",\"type\":\"string\"}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): UserAccountToken {
            |    return UserAccountToken(
            |      record.get(0).toString() as String
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: UserAccountToken ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.token)
            |    return record
            |  }
            |
            |}
            |
            |package packageName.avro
            |
            |import packageName.model.User
            |
            |object UserAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"username\",\"type\":\"string\"},{\"name\":\"account\",\"type\":" + UserAccountAvro.SCHEMA + "}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): User {
            |    return User(
            |      record.get(0).toString() as String,
            |          UserAccountAvro.from(record.get(1) as org.apache.avro.generic.GenericData.Record)
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: User ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.username)
            |        record.put(1, UserAccountAvro.to(model.account))
            |    return record
            |  }
            |
            |}
            |
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileTypeTest() {
        shouldThrowWithMessage<NotImplementedError>("An operation is not implemented.") { CompileTypeTest.compiler { emitter } }
    }
}
