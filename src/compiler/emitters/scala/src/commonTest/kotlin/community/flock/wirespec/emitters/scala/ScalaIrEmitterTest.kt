package community.flock.wirespec.emitters.scala

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
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
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ScalaIrEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(ScalaIrEmitter())
    }

    @Test
    fun testEmitterType() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model
            |case class Todo(
            |  val name: String,
            |  val description: Option[String],
            |  val notes: List[String],
            |  val done: Boolean
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    List.empty[String]
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
            |case class TodoWithoutProperties() extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    List.empty[String]
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class UUID(
            |  override val value: String
            |) extends Wirespec.Refined[String] {
            |  override def validate(): Boolean =
            |    ${"\"\"\""}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}${"\"\"\""}.r.findFirstIn(value).isDefined
            |  override def toString(): String =
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |enum TodoStatus(override val label: String) extends Wirespec.Enum {
            |  case OPEN extends TodoStatus("OPEN"),
            |  case IN_PROGRESS extends TodoStatus("IN_PROGRESS"),
            |  case CLOSE extends TodoStatus("CLOSE")
            |  override def toString(): String = {
            |    label
            |  }
            |}
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.enum)
        res shouldBe expected
    }

    @Test
    fun compileTypeTest() {
        val scala = """
            |package community.flock.wirespec.generated.model
            |case class Request(
            |  val `type`: String,
            |  val url: String,
            |  val BODY_TYPE: Option[String],
            |  val params: List[String],
            |  val headers: Map[String, String],
            |  val body: Option[Map[String, Option[List[Option[String]]]]]
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    List.empty[String]
            |}
            |
        """.trimMargin()

        CompileTypeTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileEnumTest() {
        val scala = """
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |enum MyAwesomeEnum(override val label: String) extends Wirespec.Enum {
            |  case ONE extends MyAwesomeEnum("ONE"),
            |  case Two extends MyAwesomeEnum("Two"),
            |  case THREE_MORE extends MyAwesomeEnum("THREE_MORE"),
            |  case UnitedKingdom extends MyAwesomeEnum("UnitedKingdom"),
            |  case _1 extends MyAwesomeEnum("-1"),
            |  case _0 extends MyAwesomeEnum("0"),
            |  case _10 extends MyAwesomeEnum("10"),
            |  case _999 extends MyAwesomeEnum("-999"),
            |  case _88 extends MyAwesomeEnum("88")
            |  override def toString(): String = {
            |    label
            |  }
            |}
            |
        """.trimMargin()

        CompileEnumTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileRefinedTest() {
        val scala = """
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TodoId(
            |  override val value: String
            |) extends Wirespec.Refined[String] {
            |  override def validate(): Boolean =
            |    ${"\"\"\""}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}${"\"\"\""}.r.findFirstIn(value).isDefined
            |  override def toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TodoNoRegex(
            |  override val value: String
            |) extends Wirespec.Refined[String] {
            |  override def validate(): Boolean =
            |    true
            |  override def toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TestInt(
            |  override val value: Long
            |) extends Wirespec.Refined[Long] {
            |  override def validate(): Boolean =
            |    true
            |  override def toString(): String =
            |    value.toString
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TestInt0(
            |  override val value: Long
            |) extends Wirespec.Refined[Long] {
            |  override def validate(): Boolean =
            |    true
            |  override def toString(): String =
            |    value.toString
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TestInt1(
            |  override val value: Long
            |) extends Wirespec.Refined[Long] {
            |  override def validate(): Boolean =
            |    0 <= value
            |  override def toString(): String =
            |    value.toString
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TestInt2(
            |  override val value: Long
            |) extends Wirespec.Refined[Long] {
            |  override def validate(): Boolean =
            |    1 <= value && value <= 3
            |  override def toString(): String =
            |    value.toString
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TestNum(
            |  override val value: Double
            |) extends Wirespec.Refined[Double] {
            |  override def validate(): Boolean =
            |    true
            |  override def toString(): String =
            |    value.toString
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TestNum0(
            |  override val value: Double
            |) extends Wirespec.Refined[Double] {
            |  override def validate(): Boolean =
            |    true
            |  override def toString(): String =
            |    value.toString
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TestNum1(
            |  override val value: Double
            |) extends Wirespec.Refined[Double] {
            |  override def validate(): Boolean =
            |    value <= 0.5
            |  override def toString(): String =
            |    value.toString
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TestNum2(
            |  override val value: Double
            |) extends Wirespec.Refined[Double] {
            |  override def validate(): Boolean =
            |    -0.2 <= value && value <= 0.5
            |  override def toString(): String =
            |    value.toString
            |}
            |
        """.trimMargin()

        CompileRefinedTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileUnionTest() {
        val scala = """
            |package community.flock.wirespec.generated.model
            |sealed trait UserAccount
            |
            |package community.flock.wirespec.generated.model
            |case class UserAccountPassword(
            |  val username: String,
            |  val password: String
            |) extends Wirespec.Model with UserAccount {
            |  override def validate(): List[String] =
            |    List.empty[String]
            |}
            |
            |package community.flock.wirespec.generated.model
            |case class UserAccountToken(
            |  val token: String
            |) extends Wirespec.Model with UserAccount {
            |  override def validate(): List[String] =
            |    List.empty[String]
            |}
            |
            |package community.flock.wirespec.generated.model
            |case class User(
            |  val username: String,
            |  val account: UserAccount
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    List.empty[String]
            |}
            |
        """.trimMargin()

        CompileUnionTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileChannelTest() {
        val scala = """
            |package community.flock.wirespec.generated.channel
            |trait Queue extends Wirespec.Channel {
            |  def invoke(message: String): Unit
            |}
            |
        """.trimMargin()

        CompileChannelTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileMinimalEndpointTest() {
        val scala = """
            |package community.flock.wirespec.generated.endpoint
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TodoDto
            |object GetTodos extends Wirespec.Endpoint {
            |  object Path extends Wirespec.Path
            |  object Queries extends Wirespec.Queries
            |  object RequestHeaders extends Wirespec.Request.Headers
            |  object Request extends Wirespec.Request[Unit] {
            |      override val path: Path.type = Path
            |      override val method: Wirespec.Method = Wirespec.Method.GET
            |      override val queries: Queries.type = Queries
            |      override val headers: RequestHeaders.type = RequestHeaders
            |      override val body: Unit = ()  }
            |  sealed trait Response[T] extends Wirespec.Response[T]
            |  sealed trait Response2XX[T] extends Response[T]
            |  sealed trait ResponseListTodoDto extends Response[List[TodoDto]]
            |  object Response200Headers extends Wirespec.Response.Headers
            |  case class Response200(
            |      override val status: Int,
            |      override val headers: Response200Headers.type,
            |      override val body: List[TodoDto]
            |    ) extends Response2XX[List[TodoDto]] with ResponseListTodoDto {
            |      def this(body: List[TodoDto]) = this(200, Response200Headers, body)
            |    }
            |  def toRawRequest(serialization: Wirespec.Serializer, request: Request.type): Wirespec.RawRequest =
            |    new Wirespec.RawRequest(
            |      method = request.method.toString,
            |      path = List("todos"),
            |      queries = Map.empty,
            |      headers = Map.empty,
            |      body = None
            |    )
            |  def fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request.type =
            |    Request
            |  def toRawResponse(serialization: Wirespec.Serializer, response: Response[?]): Wirespec.RawResponse = {
            |    response match {
            |        case r: Response200 => {
            |          new Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = Map.empty,
            |            body = Some(serialization.serializeBody(r.body, scala.reflect.classTag[List[TodoDto]]))
            |          )
            |        }
            |        case _ => {
            |          throw new IllegalStateException(("Cannot match response with status: " + response.status))
            |        }
            |    }
            |  }
            |  def fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response[?] = {
            |    response.statusCode match {
            |        case 200 => {
            |          new Response200(body = (response.body.map(it => serialization.deserializeBody[List[TodoDto]](it, scala.reflect.classTag[List[TodoDto]])).getOrElse(throw new IllegalStateException("body is null"))))
            |        }
            |        case _ => {
            |          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode))
            |        }
            |    }
            |  }
            |  trait Handler[F[_]] extends Wirespec.Handler {
            |      def getTodos(request: Request.type): F[Response[?]]
            |  }
            |  trait Call[F[_]] extends Wirespec.Call {
            |      def getTodos(): F[Response[?]]
            |  }
            |  object Client extends Wirespec.Client[Request.type, Response[?]] {
            |    override val pathTemplate: String = "/todos"
            |    override val method: String = "GET"
            |    override val pathSegments: List[Wirespec.PathSegment] = List(Wirespec.Literal("todos"))
            |    override def client(serialization: Wirespec.Serialization): Wirespec.ClientEdge[Request.type, Response[?]] = new Wirespec.ClientEdge[Request.type, Response[?]] {
            |      override def to(request: Request.type): Wirespec.RawRequest = toRawRequest(serialization, request)
            |      override def from(response: Wirespec.RawResponse): Response[?] = fromRawResponse(serialization, response)
            |    }
            |  }
            |  object Server extends Wirespec.Server[Request.type, Response[?]] {
            |    override val pathTemplate: String = "/todos"
            |    override val method: String = "GET"
            |    override val pathSegments: List[Wirespec.PathSegment] = List(Wirespec.Literal("todos"))
            |    override def server(serialization: Wirespec.Serialization): Wirespec.ServerEdge[Request.type, Response[?]] = new Wirespec.ServerEdge[Request.type, Response[?]] {
            |      override def from(request: Wirespec.RawRequest): Request.type = fromRawRequest(serialization, request)
            |      override def to(response: Response[?]): Wirespec.RawResponse = toRawResponse(serialization, response)
            |    }
            |  }
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TodoDto(
            |  val description: String
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    List.empty[String]
            |}
            |
            |package community.flock.wirespec.generated.client
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.endpoint.GetTodos
            |case class GetTodosClient(
            |  val serialization: Wirespec.Serialization,
            |  val transportation: Wirespec.Transportation
            |) extends GetTodos.Call[[A] =>> A] {
            |  override def getTodos(): GetTodos.Response[?] = {
            |    val request = GetTodos.Request
            |    val rawRequest = GetTodos.toRawRequest(serialization, request)
            |    val rawResponse = transportation.transport(rawRequest)
            |    GetTodos.fromRawResponse(serialization, rawResponse)
            |  }
            |}
            |
            |package community.flock.wirespec.generated
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.endpoint.GetTodos
            |import community.flock.wirespec.generated.client.GetTodosClient
            |case class Client(
            |  val serialization: Wirespec.Serialization,
            |  val transportation: Wirespec.Transportation
            |) extends GetTodos.Call[[A] =>> A] {
            |  override def getTodos(): GetTodos.Response[?] =
            |    new GetTodosClient(
            |      serialization = serialization,
            |      transportation = transportation
            |    ).getTodos()
            |}
            |
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileFullEndpointTest() {
        val scala = """
            |package community.flock.wirespec.generated.endpoint
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.model.Error
            |object PutTodo extends Wirespec.Endpoint {
            |  case class Path(
            |      val id: String
            |    ) extends Wirespec.Path
            |  case class Queries(
            |      val done: Boolean,
            |      val name: Option[String]
            |    ) extends Wirespec.Queries
            |  case class RequestHeaders(
            |      val token: Token,
            |      val refreshToken: Option[Token]
            |    ) extends Wirespec.Request.Headers
            |  case class Request(
            |      override val path: Path,
            |      override val method: Wirespec.Method,
            |      override val queries: Queries,
            |      override val headers: RequestHeaders,
            |      override val body: PotentialTodoDto
            |    ) extends Wirespec.Request[PotentialTodoDto] {
            |      def this(id: String, done: Boolean, name: Option[String], token: Token, refreshToken: Option[Token], body: PotentialTodoDto) = this(Path(id = id), Wirespec.Method.PUT, Queries(
            |        done = done,
            |        name = name
            |      ), RequestHeaders(
            |        token = token,
            |        refreshToken = refreshToken
            |      ), body)
            |    }
            |  sealed trait Response[T] extends Wirespec.Response[T]
            |  sealed trait Response2XX[T] extends Response[T]
            |  sealed trait Response5XX[T] extends Response[T]
            |  sealed trait ResponseTodoDto extends Response[TodoDto]
            |  sealed trait ResponseError extends Response[Error]
            |  object Response200Headers extends Wirespec.Response.Headers
            |  case class Response200(
            |      override val status: Int,
            |      override val headers: Response200Headers.type,
            |      override val body: TodoDto
            |    ) extends Response2XX[TodoDto] with ResponseTodoDto {
            |      def this(body: TodoDto) = this(200, Response200Headers, body)
            |    }
            |  case class Response201Headers(
            |      val token: Token,
            |      val refreshToken: Option[Token]
            |    ) extends Wirespec.Response.Headers
            |  case class Response201(
            |      override val status: Int,
            |      override val headers: Response201Headers,
            |      override val body: TodoDto
            |    ) extends Response2XX[TodoDto] with ResponseTodoDto {
            |      def this(token: Token, refreshToken: Option[Token], body: TodoDto) = this(201, Response201Headers(
            |        token = token,
            |        refreshToken = refreshToken
            |      ), body)
            |    }
            |  object Response500Headers extends Wirespec.Response.Headers
            |  case class Response500(
            |      override val status: Int,
            |      override val headers: Response500Headers.type,
            |      override val body: Error
            |    ) extends Response5XX[Error] with ResponseError {
            |      def this(body: Error) = this(500, Response500Headers, body)
            |    }
            |  def toRawRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
            |    new Wirespec.RawRequest(
            |      method = request.method.toString,
            |      path = List("todos", serialization.serializePath[String](request.path.id, scala.reflect.classTag[String])),
            |      queries = Map("done" -> serialization.serializeParam[Boolean](request.queries.done, scala.reflect.classTag[Boolean]), "name" -> (request.queries.name.map(it => serialization.serializeParam[String](it, scala.reflect.classTag[String])).getOrElse(List.empty[String]))),
            |      headers = Map("token" -> serialization.serializeParam[Token](request.headers.token, scala.reflect.classTag[Token]), "Refresh-Token" -> (request.headers.refreshToken.map(it => serialization.serializeParam[Token](it, scala.reflect.classTag[Token])).getOrElse(List.empty[String]))),
            |      body = Some(serialization.serializeBody[PotentialTodoDto](request.body, scala.reflect.classTag[PotentialTodoDto]))
            |    )
            |  def fromRawRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
            |    new Request(
            |      id = serialization.deserializePath[String](request.path(1), scala.reflect.classTag[String]),
            |      done = (request.queries.get("done").map(it => serialization.deserializeParam[Boolean](it, scala.reflect.classTag[Boolean])).getOrElse(throw new IllegalStateException("Param done cannot be null"))),
            |      name = (request.queries.get("name").map(it => serialization.deserializeParam[String](it, scala.reflect.classTag[String]))),
            |      token = (request.headers.find(_._1.equalsIgnoreCase("token")).map(_._2).map(it => serialization.deserializeParam[Token](it, scala.reflect.classTag[Token])).getOrElse(throw new IllegalStateException("Param token cannot be null"))),
            |      refreshToken = (request.headers.find(_._1.equalsIgnoreCase("Refresh-Token")).map(_._2).map(it => serialization.deserializeParam[Token](it, scala.reflect.classTag[Token]))),
            |      body = (request.body.map(it => serialization.deserializeBody[PotentialTodoDto](it, scala.reflect.classTag[PotentialTodoDto])).getOrElse(throw new IllegalStateException("body is null")))
            |    )
            |  def toRawResponse(serialization: Wirespec.Serializer, response: Response[?]): Wirespec.RawResponse = {
            |    response match {
            |        case r: Response200 => {
            |          new Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = Map.empty,
            |            body = Some(serialization.serializeBody(r.body, scala.reflect.classTag[TodoDto]))
            |          )
            |        }
            |        case r: Response201 => {
            |          new Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = Map("token" -> serialization.serializeParam[Token](r.headers.token, scala.reflect.classTag[Token]), "refreshToken" -> (r.headers.refreshToken.map(it => serialization.serializeParam[Token](it, scala.reflect.classTag[Token])).getOrElse(List.empty[String]))),
            |            body = Some(serialization.serializeBody(r.body, scala.reflect.classTag[TodoDto]))
            |          )
            |        }
            |        case r: Response500 => {
            |          new Wirespec.RawResponse(
            |            statusCode = r.status,
            |            headers = Map.empty,
            |            body = Some(serialization.serializeBody(r.body, scala.reflect.classTag[Error]))
            |          )
            |        }
            |        case _ => {
            |          throw new IllegalStateException(("Cannot match response with status: " + response.status))
            |        }
            |    }
            |  }
            |  def fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response[?] = {
            |    response.statusCode match {
            |        case 200 => {
            |          new Response200(body = (response.body.map(it => serialization.deserializeBody[TodoDto](it, scala.reflect.classTag[TodoDto])).getOrElse(throw new IllegalStateException("body is null"))))
            |        }
            |        case 201 => {
            |          new Response201(
            |            token = (response.headers.find(_._1.equalsIgnoreCase("token")).map(_._2).map(it => serialization.deserializeParam[Token](it, scala.reflect.classTag[Token])).getOrElse(throw new IllegalStateException("Param token cannot be null"))),
            |            refreshToken = (response.headers.find(_._1.equalsIgnoreCase("refreshToken")).map(_._2).map(it => serialization.deserializeParam[Token](it, scala.reflect.classTag[Token]))),
            |            body = (response.body.map(it => serialization.deserializeBody[TodoDto](it, scala.reflect.classTag[TodoDto])).getOrElse(throw new IllegalStateException("body is null")))
            |          )
            |        }
            |        case 500 => {
            |          new Response500(body = (response.body.map(it => serialization.deserializeBody[Error](it, scala.reflect.classTag[Error])).getOrElse(throw new IllegalStateException("body is null"))))
            |        }
            |        case _ => {
            |          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode))
            |        }
            |    }
            |  }
            |  trait Handler[F[_]] extends Wirespec.Handler {
            |      def putTodo(request: Request): F[Response[?]]
            |  }
            |  trait Call[F[_]] extends Wirespec.Call {
            |      def putTodo(id: String, done: Boolean, name: Option[String], token: Token, refreshToken: Option[Token], body: PotentialTodoDto): F[Response[?]]
            |  }
            |  object Client extends Wirespec.Client[Request, Response[?]] {
            |    override val pathTemplate: String = "/todos/{id}"
            |    override val method: String = "PUT"
            |    override val pathSegments: List[Wirespec.PathSegment] = List(Wirespec.Literal("todos"), Wirespec.Param("id", scala.reflect.classTag[String]))
            |    override def client(serialization: Wirespec.Serialization): Wirespec.ClientEdge[Request, Response[?]] = new Wirespec.ClientEdge[Request, Response[?]] {
            |      override def to(request: Request): Wirespec.RawRequest = toRawRequest(serialization, request)
            |      override def from(response: Wirespec.RawResponse): Response[?] = fromRawResponse(serialization, response)
            |    }
            |  }
            |  object Server extends Wirespec.Server[Request, Response[?]] {
            |    override val pathTemplate: String = "/todos/{id}"
            |    override val method: String = "PUT"
            |    override val pathSegments: List[Wirespec.PathSegment] = List(Wirespec.Literal("todos"), Wirespec.Param("id", scala.reflect.classTag[String]))
            |    override def server(serialization: Wirespec.Serialization): Wirespec.ServerEdge[Request, Response[?]] = new Wirespec.ServerEdge[Request, Response[?]] {
            |      override def from(request: Wirespec.RawRequest): Request = fromRawRequest(serialization, request)
            |      override def to(response: Response[?]): Wirespec.RawResponse = toRawResponse(serialization, response)
            |    }
            |  }
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class PotentialTodoDto(
            |  val name: String,
            |  val done: Boolean
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    List.empty[String]
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class Token(
            |  val iss: String
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    List.empty[String]
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class TodoDto(
            |  val id: String,
            |  val name: String,
            |  val done: Boolean
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    List.empty[String]
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class Error(
            |  val code: Long,
            |  val description: String
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    List.empty[String]
            |}
            |
            |package community.flock.wirespec.generated.client
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.model.Error
            |import community.flock.wirespec.generated.endpoint.PutTodo
            |case class PutTodoClient(
            |  val serialization: Wirespec.Serialization,
            |  val transportation: Wirespec.Transportation
            |) extends PutTodo.Call[[A] =>> A] {
            |  override def putTodo(id: String, done: Boolean, name: Option[String], token: Token, refreshToken: Option[Token], body: PotentialTodoDto): PutTodo.Response[?] = {
            |    val request = new PutTodo.Request(
            |      id = id,
            |      done = done,
            |      name = name,
            |      token = token,
            |      refreshToken = refreshToken,
            |      body = body
            |    )
            |    val rawRequest = PutTodo.toRawRequest(serialization, request)
            |    val rawResponse = transportation.transport(rawRequest)
            |    PutTodo.fromRawResponse(serialization, rawResponse)
            |  }
            |}
            |
            |package community.flock.wirespec.generated
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.model.Error
            |import community.flock.wirespec.generated.endpoint.PutTodo
            |import community.flock.wirespec.generated.client.PutTodoClient
            |case class Client(
            |  val serialization: Wirespec.Serialization,
            |  val transportation: Wirespec.Transportation
            |) extends PutTodo.Call[[A] =>> A] {
            |  override def putTodo(id: String, done: Boolean, name: Option[String], token: Token, refreshToken: Option[Token], body: PotentialTodoDto): PutTodo.Response[?] =
            |    new PutTodoClient(
            |      serialization = serialization,
            |      transportation = transportation
            |    ).putTodo(id, done, name, token, refreshToken, body)
            |}
            |
        """.trimMargin()

        CompileFullEndpointTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileNestedTypeTest() {
        val scala = """
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class DutchPostalCode(
            |  override val value: String
            |) extends Wirespec.Refined[String] {
            |  override def validate(): Boolean =
            |    ${"\"\"\""}^([0-9]{4}[A-Z]{2})${'$'}${"\"\"\""}.r.findFirstIn(value).isDefined
            |  override def toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class Address(
            |  val street: String,
            |  val houseNumber: Long,
            |  val postalCode: DutchPostalCode
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    if (!postalCode.validate()) List("postalCode") else List.empty[String]
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class Person(
            |  val name: String,
            |  val address: Address,
            |  val tags: List[String]
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    address.validate().map(e => s"address.${'$'}{e}")
            |}
            |
        """.trimMargin()

        CompileNestedTypeTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileComplexModelTest() {
        val scala = """
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class Email(
            |  override val value: String
            |) extends Wirespec.Refined[String] {
            |  override def validate(): Boolean =
            |    ${"\"\"\""}^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}${"\"\"\""}.r.findFirstIn(value).isDefined
            |  override def toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class PhoneNumber(
            |  override val value: String
            |) extends Wirespec.Refined[String] {
            |  override def validate(): Boolean =
            |    ${"\"\"\""}^\+[1-9]\d{1,14}${'$'}${"\"\"\""}.r.findFirstIn(value).isDefined
            |  override def toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class Tag(
            |  override val value: String
            |) extends Wirespec.Refined[String] {
            |  override def validate(): Boolean =
            |    ${"\"\"\""}^[a-z][a-z0-9-]{0,19}${'$'}${"\"\"\""}.r.findFirstIn(value).isDefined
            |  override def toString(): String =
            |    value
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class EmployeeAge(
            |  override val value: Long
            |) extends Wirespec.Refined[Long] {
            |  override def validate(): Boolean =
            |    18 <= value && value <= 65
            |  override def toString(): String =
            |    value.toString
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class ContactInfo(
            |  val email: Email,
            |  val phone: Option[PhoneNumber]
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    (if (!email.validate()) List("email") else List.empty[String]) ++ (phone.map(it => if (!it.validate()) List("phone") else List.empty[String]).getOrElse(List.empty[String]))
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class Employee(
            |  val name: String,
            |  val age: EmployeeAge,
            |  val contactInfo: ContactInfo,
            |  val tags: List[Tag]
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    (if (!age.validate()) List("age") else List.empty[String]) ++ contactInfo.validate().map(e => s"contactInfo.${'$'}{e}") ++ tags.zipWithIndex.flatMap { case (el, i) => if (!el.validate()) List(s"tags[${'$'}{i}]") else List.empty[String] }
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class Department(
            |  val name: String,
            |  val employees: List[Employee]
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    employees.zipWithIndex.flatMap { case (el, i) => el.validate().map(e => s"employees[${'$'}{i}].${'$'}{e}") }
            |}
            |
            |package community.flock.wirespec.generated.model
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |case class Company(
            |  val name: String,
            |  val departments: List[Department]
            |) extends Wirespec.Model {
            |  override def validate(): List[String] =
            |    departments.zipWithIndex.flatMap { case (el, i) => el.validate().map(e => s"departments[${'$'}{i}].${'$'}{e}") }
            |}
            |
        """.trimMargin()

        CompileComplexModelTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun sharedOutputTest() {
        val expected = """
            |package community.flock.wirespec.scala
            |import scala.reflect.ClassTag
            |object Wirespec {
            |  trait Model {
            |      def validate(): List[String]
            |  }
            |  trait Enum {
            |      def label: String
            |  }
            |  trait Endpoint
            |  trait Channel
            |  trait Refined[T] {
            |      def value: T
            |      def validate(): Boolean
            |  }
            |  trait Path
            |  trait Queries
            |  trait Headers
            |  trait Handler
            |  trait Call
            |  enum Method {
            |      case GET
            |      case PUT
            |      case POST
            |      case DELETE
            |      case OPTIONS
            |      case HEAD
            |      case PATCH
            |      case TRACE
            |    }
            |  object Request {
            |      trait Headers
            |  }
            |  trait Request[T] {
            |      def path: Path
            |      def method: Method
            |      def queries: Queries
            |      def headers: Request.Headers
            |      def body: T
            |  }
            |  object Response {
            |      trait Headers
            |  }
            |  trait Response[T] {
            |      def status: Int
            |      def headers: Response.Headers
            |      def body: T
            |  }
            |  trait BodySerializer {
            |      def serializeBody[T](t: T, `type`: scala.reflect.ClassTag[?]): Array[Byte]
            |  }
            |  trait BodyDeserializer {
            |      def deserializeBody[T](raw: Array[Byte], `type`: scala.reflect.ClassTag[?]): T
            |  }
            |  trait BodySerialization extends BodySerializer with BodyDeserializer
            |  trait PathSerializer {
            |      def serializePath[T](t: T, `type`: scala.reflect.ClassTag[?]): String
            |  }
            |  trait PathDeserializer {
            |      def deserializePath[T](raw: String, `type`: scala.reflect.ClassTag[?]): T
            |  }
            |  trait PathSerialization extends PathSerializer with PathDeserializer
            |  trait ParamSerializer {
            |      def serializeParam[T](value: T, `type`: scala.reflect.ClassTag[?]): List[String]
            |  }
            |  trait ParamDeserializer {
            |      def deserializeParam[T](values: List[String], `type`: scala.reflect.ClassTag[?]): T
            |  }
            |  trait ParamSerialization extends ParamSerializer with ParamDeserializer
            |  trait Serializer extends BodySerializer with PathSerializer with ParamSerializer
            |  trait Deserializer extends BodyDeserializer with PathDeserializer with ParamDeserializer
            |  trait Serialization extends Serializer with Deserializer
            |  case class RawRequest(
            |      val method: String,
            |      val path: List[String],
            |      val queries: Map[String, List[String]],
            |      val headers: Map[String, List[String]],
            |      val body: Option[Array[Byte]]
            |    )
            |  case class RawResponse(
            |      val statusCode: Int,
            |      val headers: Map[String, List[String]],
            |      val body: Option[Array[Byte]]
            |    )
            |  trait Transportation {
            |      def transport(request: RawRequest): RawResponse
            |  }
            |  sealed trait PathSegment
            |  case class Literal(val value: String) extends PathSegment
            |  case class Param(val name: String, val `type`: ClassTag[?]) extends PathSegment
            |  trait ServerEdge[Req <: Request[?], Res <: Response[?]] {
            |      def from(request: RawRequest): Req
            |      def to(response: Res): RawResponse
            |  }
            |  trait ClientEdge[Req <: Request[?], Res <: Response[?]] {
            |      def to(request: Req): RawRequest
            |      def from(response: RawResponse): Res
            |  }
            |  trait Client[Req <: Request[?], Res <: Response[?]] {
            |      def pathTemplate: String
            |      def method: String
            |      def pathSegments: List[PathSegment]
            |      def client(serialization: Serialization): ClientEdge[Req, Res]
            |  }
            |  trait Server[Req <: Request[?], Res <: Response[?]] {
            |      def pathTemplate: String
            |      def method: String
            |      def pathSegments: List[PathSegment]
            |      def server(serialization: Serialization): ServerEdge[Req, Res]
            |  }
            |}
            |
        """.trimMargin()

        val emitter = ScalaIrEmitter()
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
}
