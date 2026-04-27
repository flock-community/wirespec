package community.flock.wirespec.emitters.scala

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
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
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object RequestGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Request =
            |    new Request(
            |      `type` = generator.generate((path + "type"), scala.reflect.classTag[Request], new Wirespec.GeneratorFieldString(regex = null)),
            |      url = generator.generate((path + "url"), scala.reflect.classTag[Request], new Wirespec.GeneratorFieldString(regex = null)),
            |      BODY_TYPE = if (generator.generate((path + "BODY_TYPE"), scala.reflect.classTag[Request], new Wirespec.GeneratorFieldNullable(inner = new Wirespec.GeneratorFieldString(regex = null)))) null else generator.generate((path + "BODY_TYPE"), scala.reflect.classTag[Request], new Wirespec.GeneratorFieldString(regex = null)),
            |      params = (0 until generator.generate((path + "params"), scala.reflect.classTag[Request], new Wirespec.GeneratorFieldArray(inner = new Wirespec.GeneratorFieldString(regex = null)))).map(i => generator.generate(((path + "params") + i.toString()), scala.reflect.classTag[Request], new Wirespec.GeneratorFieldString(regex = null))),
            |      headers = generator.generate((path + "headers"), scala.reflect.classTag[Request], new Wirespec.GeneratorFieldDict(
            |        key = null,
            |        value = new Wirespec.GeneratorFieldString(regex = null)
            |      )),
            |      body = if (generator.generate((path + "body"), scala.reflect.classTag[Request], new Wirespec.GeneratorFieldNullable(inner = new Wirespec.GeneratorFieldDict(
            |        key = null,
            |        value = null
            |      )))) null else generator.generate((path + "body"), scala.reflect.classTag[Request], new Wirespec.GeneratorFieldDict(
            |        key = null,
            |        value = null
            |      ))
            |    )
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
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object MyAwesomeEnumGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): MyAwesomeEnum =
            |    MyAwesomeEnum.valueOf(generator.generate((path + "value"), scala.reflect.classTag[MyAwesomeEnum], new Wirespec.GeneratorFieldEnum(values = List("ONE", "Two", "THREE_MORE", "UnitedKingdom", "-1", "0", "10", "-999", "88"))))
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
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TodoIdGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TodoId =
            |    new TodoId(value = generator.generate((path + "value"), scala.reflect.classTag[TodoId], new Wirespec.GeneratorFieldString(regex = "^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}")))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TodoNoRegexGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TodoNoRegex =
            |    new TodoNoRegex(value = generator.generate((path + "value"), scala.reflect.classTag[TodoNoRegex], new Wirespec.GeneratorFieldString(regex = null)))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TestIntGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TestInt =
            |    new TestInt(value = generator.generate((path + "value"), scala.reflect.classTag[TestInt], new Wirespec.GeneratorFieldInteger(
            |      min = null,
            |      max = null
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TestInt0Generator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TestInt0 =
            |    new TestInt0(value = generator.generate((path + "value"), scala.reflect.classTag[TestInt0], new Wirespec.GeneratorFieldInteger(
            |      min = null,
            |      max = null
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TestInt1Generator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TestInt1 =
            |    new TestInt1(value = generator.generate((path + "value"), scala.reflect.classTag[TestInt1], new Wirespec.GeneratorFieldInteger(
            |      min = 0,
            |      max = null
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TestInt2Generator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TestInt2 =
            |    new TestInt2(value = generator.generate((path + "value"), scala.reflect.classTag[TestInt2], new Wirespec.GeneratorFieldInteger(
            |      min = 1,
            |      max = 3
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TestNumGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TestNum =
            |    new TestNum(value = generator.generate((path + "value"), scala.reflect.classTag[TestNum], new Wirespec.GeneratorFieldNumber(
            |      min = null,
            |      max = null
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TestNum0Generator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TestNum0 =
            |    new TestNum0(value = generator.generate((path + "value"), scala.reflect.classTag[TestNum0], new Wirespec.GeneratorFieldNumber(
            |      min = null,
            |      max = null
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TestNum1Generator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TestNum1 =
            |    new TestNum1(value = generator.generate((path + "value"), scala.reflect.classTag[TestNum1], new Wirespec.GeneratorFieldNumber(
            |      min = null,
            |      max = 0.5
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TestNum2Generator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TestNum2 =
            |    new TestNum2(value = generator.generate((path + "value"), scala.reflect.classTag[TestNum2], new Wirespec.GeneratorFieldNumber(
            |      min = -0.2,
            |      max = 0.5
            |    )))
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
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object UserAccountGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): UserAccount = {
            |    val variant = generator.generate((path + "variant"), scala.reflect.classTag[UserAccount], new Wirespec.GeneratorFieldUnion(variants = List("UserAccountPassword", "UserAccountToken")))
            |    variant match {
            |        case "UserAccountPassword" => {
            |          UserAccountPasswordGenerator.generate((path + "UserAccountPassword"), generator)
            |        }
            |        case "UserAccountToken" => {
            |          UserAccountTokenGenerator.generate((path + "UserAccountToken"), generator)
            |        }
            |    }
            |    throw new IllegalStateException("Unknown variant")
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object UserAccountPasswordGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): UserAccountPassword =
            |    new UserAccountPassword(
            |      username = generator.generate((path + "username"), scala.reflect.classTag[UserAccountPassword], new Wirespec.GeneratorFieldString(regex = null)),
            |      password = generator.generate((path + "password"), scala.reflect.classTag[UserAccountPassword], new Wirespec.GeneratorFieldString(regex = null))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object UserAccountTokenGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): UserAccountToken =
            |    new UserAccountToken(token = generator.generate((path + "token"), scala.reflect.classTag[UserAccountToken], new Wirespec.GeneratorFieldString(regex = null)))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object UserGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): User =
            |    new User(
            |      username = generator.generate((path + "username"), scala.reflect.classTag[User], new Wirespec.GeneratorFieldString(regex = null)),
            |      account = UserAccountGenerator.generate((path + "account"), generator)
            |    )
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
            |    override def client(serialization: Wirespec.Serialization): Wirespec.ClientEdge[Request.type, Response[?]] = new Wirespec.ClientEdge[Request.type, Response[?]] {
            |      override def to(request: Request.type): Wirespec.RawRequest = toRawRequest(serialization, request)
            |      override def from(response: Wirespec.RawResponse): Response[?] = fromRawResponse(serialization, response)
            |    }
            |  }
            |  object Server extends Wirespec.Server[Request.type, Response[?]] {
            |    override val pathTemplate: String = "/todos"
            |    override val method: String = "GET"
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
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TodoDtoGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TodoDto =
            |    new TodoDto(description = generator.generate((path + "description"), scala.reflect.classTag[TodoDto], new Wirespec.GeneratorFieldString(regex = null)))
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
            |    override def client(serialization: Wirespec.Serialization): Wirespec.ClientEdge[Request, Response[?]] = new Wirespec.ClientEdge[Request, Response[?]] {
            |      override def to(request: Request): Wirespec.RawRequest = toRawRequest(serialization, request)
            |      override def from(response: Wirespec.RawResponse): Response[?] = fromRawResponse(serialization, response)
            |    }
            |  }
            |  object Server extends Wirespec.Server[Request, Response[?]] {
            |    override val pathTemplate: String = "/todos/{id}"
            |    override val method: String = "PUT"
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
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object PotentialTodoDtoGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): PotentialTodoDto =
            |    new PotentialTodoDto(
            |      name = generator.generate((path + "name"), scala.reflect.classTag[PotentialTodoDto], new Wirespec.GeneratorFieldString(regex = null)),
            |      done = generator.generate((path + "done"), scala.reflect.classTag[PotentialTodoDto], Wirespec.GeneratorFieldBoolean)
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TokenGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Token =
            |    new Token(iss = generator.generate((path + "iss"), scala.reflect.classTag[Token], new Wirespec.GeneratorFieldString(regex = null)))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TodoDtoGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): TodoDto =
            |    new TodoDto(
            |      id = generator.generate((path + "id"), scala.reflect.classTag[TodoDto], new Wirespec.GeneratorFieldString(regex = null)),
            |      name = generator.generate((path + "name"), scala.reflect.classTag[TodoDto], new Wirespec.GeneratorFieldString(regex = null)),
            |      done = generator.generate((path + "done"), scala.reflect.classTag[TodoDto], Wirespec.GeneratorFieldBoolean)
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object ErrorGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Error =
            |    new Error(
            |      code = generator.generate((path + "code"), scala.reflect.classTag[Error], new Wirespec.GeneratorFieldInteger(
            |        min = null,
            |        max = null
            |      )),
            |      description = generator.generate((path + "description"), scala.reflect.classTag[Error], new Wirespec.GeneratorFieldString(regex = null))
            |    )
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
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object DutchPostalCodeGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): DutchPostalCode =
            |    new DutchPostalCode(value = generator.generate((path + "value"), scala.reflect.classTag[DutchPostalCode], new Wirespec.GeneratorFieldString(regex = "^([0-9]{4}[A-Z]{2})${'$'}")))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object AddressGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Address =
            |    new Address(
            |      street = generator.generate((path + "street"), scala.reflect.classTag[Address], new Wirespec.GeneratorFieldString(regex = null)),
            |      houseNumber = generator.generate((path + "houseNumber"), scala.reflect.classTag[Address], new Wirespec.GeneratorFieldInteger(
            |        min = null,
            |        max = null
            |      )),
            |      postalCode = DutchPostalCodeGenerator.generate((path + "postalCode"), generator)
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object PersonGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Person =
            |    new Person(
            |      name = generator.generate((path + "name"), scala.reflect.classTag[Person], new Wirespec.GeneratorFieldString(regex = null)),
            |      address = AddressGenerator.generate((path + "address"), generator),
            |      tags = (0 until generator.generate((path + "tags"), scala.reflect.classTag[Person], new Wirespec.GeneratorFieldArray(inner = new Wirespec.GeneratorFieldString(regex = null)))).map(i => generator.generate(((path + "tags") + i.toString()), scala.reflect.classTag[Person], new Wirespec.GeneratorFieldString(regex = null)))
            |    )
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
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object EmailGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Email =
            |    new Email(value = generator.generate((path + "value"), scala.reflect.classTag[Email], new Wirespec.GeneratorFieldString(regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}")))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object PhoneNumberGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): PhoneNumber =
            |    new PhoneNumber(value = generator.generate((path + "value"), scala.reflect.classTag[PhoneNumber], new Wirespec.GeneratorFieldString(regex = "^\+[1-9]\d{1,14}${'$'}")))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object TagGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Tag =
            |    new Tag(value = generator.generate((path + "value"), scala.reflect.classTag[Tag], new Wirespec.GeneratorFieldString(regex = "^[a-z][a-z0-9-]{0,19}${'$'}")))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object EmployeeAgeGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): EmployeeAge =
            |    new EmployeeAge(value = generator.generate((path + "value"), scala.reflect.classTag[EmployeeAge], new Wirespec.GeneratorFieldInteger(
            |      min = 18,
            |      max = 65
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object ContactInfoGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): ContactInfo =
            |    new ContactInfo(
            |      email = EmailGenerator.generate((path + "email"), generator),
            |      phone = if (generator.generate((path + "phone"), scala.reflect.classTag[ContactInfo], new Wirespec.GeneratorFieldNullable(inner = null))) null else PhoneNumberGenerator.generate((path + "phone"), generator)
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object EmployeeGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Employee =
            |    new Employee(
            |      name = generator.generate((path + "name"), scala.reflect.classTag[Employee], new Wirespec.GeneratorFieldString(regex = null)),
            |      age = EmployeeAgeGenerator.generate((path + "age"), generator),
            |      contactInfo = ContactInfoGenerator.generate((path + "contactInfo"), generator),
            |      tags = (0 until generator.generate((path + "tags"), scala.reflect.classTag[Employee], new Wirespec.GeneratorFieldArray(inner = null))).map(i => TagGenerator.generate(((path + "tags") + i.toString()), generator))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object DepartmentGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Department =
            |    new Department(
            |      name = generator.generate((path + "name"), scala.reflect.classTag[Department], new Wirespec.GeneratorFieldString(regex = null)),
            |      employees = (0 until generator.generate((path + "employees"), scala.reflect.classTag[Department], new Wirespec.GeneratorFieldArray(inner = null))).map(i => EmployeeGenerator.generate(((path + "employees") + i.toString()), generator))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object CompanyGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Company =
            |    new Company(
            |      name = generator.generate((path + "name"), scala.reflect.classTag[Company], new Wirespec.GeneratorFieldString(regex = null)),
            |      departments = (0 until generator.generate((path + "departments"), scala.reflect.classTag[Company], new Wirespec.GeneratorFieldArray(inner = null))).map(i => DepartmentGenerator.generate(((path + "departments") + i.toString()), generator))
            |    )
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
            |  sealed trait GeneratorField[T]
            |  case class GeneratorFieldString(
            |      val regex: Option[String]
            |    ) extends GeneratorField[String]
            |  case class GeneratorFieldInteger(
            |      val min: Option[Long],
            |      val max: Option[Long]
            |    ) extends GeneratorField[Long]
            |  case class GeneratorFieldNumber(
            |      val min: Option[Double],
            |      val max: Option[Double]
            |    ) extends GeneratorField[Double]
            |  object GeneratorFieldBoolean extends GeneratorField[Boolean]
            |  object GeneratorFieldBytes extends GeneratorField[Array[Byte]]
            |  case class GeneratorFieldEnum(
            |      val values: List[String]
            |    ) extends GeneratorField[String]
            |  case class GeneratorFieldUnion(
            |      val variants: List[String]
            |    ) extends GeneratorField[String]
            |  case class GeneratorFieldArray(
            |      val inner: Option[GeneratorField[?]]
            |    ) extends GeneratorField[Int]
            |  case class GeneratorFieldNullable(
            |      val inner: Option[GeneratorField[?]]
            |    ) extends GeneratorField[Boolean]
            |  case class GeneratorFieldDict(
            |      val key: Option[GeneratorField[?]],
            |      val value: Option[GeneratorField[?]]
            |    ) extends GeneratorField[Int]
            |  trait Generator {
            |      def generate[T](path: List[String], `type`: scala.reflect.ClassTag[?], field: GeneratorField[T]): T
            |  }
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
            |      def client(serialization: Serialization): ClientEdge[Req, Res]
            |  }
            |  trait Server[Req <: Request[?], Res <: Response[?]] {
            |      def pathTemplate: String
            |      def method: String
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

    private fun emitGeneratorSource(node: Definition, fileNameSubstring: String): String {
        val emitter = ScalaIrEmitter()
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object AddressGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Address =
            |    new Address(
            |      street = generator.generate((path + "street"), scala.reflect.classTag[Address], new Wirespec.GeneratorFieldString(regex = null)),
            |      number = generator.generate((path + "number"), scala.reflect.classTag[Address], new Wirespec.GeneratorFieldInteger(
            |        min = null,
            |        max = null
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object ColorGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Color =
            |    Color.valueOf(generator.generate((path + "value"), scala.reflect.classTag[Color], new Wirespec.GeneratorFieldEnum(values = List("RED", "GREEN", "BLUE"))))
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object ShapeGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Shape = {
            |    val variant = generator.generate((path + "variant"), scala.reflect.classTag[Shape], new Wirespec.GeneratorFieldUnion(variants = List("Circle", "Square")))
            |    variant match {
            |        case "Circle" => {
            |          CircleGenerator.generate((path + "Circle"), generator)
            |        }
            |        case "Square" => {
            |          SquareGenerator.generate((path + "Square"), generator)
            |        }
            |    }
            |    throw new IllegalStateException("Unknown variant")
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object UUIDGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): UUID =
            |    new UUID(value = generator.generate((path + "value"), scala.reflect.classTag[UUID], new Wirespec.GeneratorFieldString(regex = "^[0-9a-f]{8}${'$'}")))
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object InventoryGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Inventory =
            |    new Inventory(items = (0 until generator.generate((path + "items"), scala.reflect.classTag[Inventory], new Wirespec.GeneratorFieldArray(inner = new Wirespec.GeneratorFieldInteger(
            |      min = null,
            |      max = null
            |    )))).map(i => generator.generate(((path + "items") + i.toString()), scala.reflect.classTag[Inventory], new Wirespec.GeneratorFieldInteger(
            |      min = null,
            |      max = null
            |    ))))
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object LookupGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Lookup =
            |    new Lookup(entries = generator.generate((path + "entries"), scala.reflect.classTag[Lookup], new Wirespec.GeneratorFieldDict(
            |      key = null,
            |      value = new Wirespec.GeneratorFieldInteger(
            |        min = null,
            |        max = null
            |      )
            |    )))
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |object PersonGenerator {
            |  def generate(path: List[String], generator: Wirespec.Generator): Person =
            |    new Person(nickname = if (generator.generate((path + "nickname"), scala.reflect.classTag[Person], new Wirespec.GeneratorFieldNullable(inner = new Wirespec.GeneratorFieldString(regex = null)))) null else generator.generate((path + "nickname"), scala.reflect.classTag[Person], new Wirespec.GeneratorFieldString(regex = null)))
            |}
            |
        """.trimMargin()

        emitGeneratorSource(person, "PersonGenerator") shouldBe expected
    }
}
