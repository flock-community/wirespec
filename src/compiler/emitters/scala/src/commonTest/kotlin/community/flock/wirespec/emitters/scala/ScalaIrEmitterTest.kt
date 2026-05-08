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
            |import community.flock.wirespec.generated.model.Request
            |object RequestGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Request =
            |    new Request(
            |      `type` = generator.generate(path ++ List("type"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      url = generator.generate(path ++ List("url"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      BODY_TYPE = generator.generate(path ++ List("BODY_TYPE"), new Wirespec.GeneratorFieldNullable(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )))),
            |      params = generator.generate(path ++ List("params"), new Wirespec.GeneratorFieldArray(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )))),
            |      headers = generator.generate(path ++ List("headers"), new Wirespec.GeneratorFieldDict(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )))),
            |      body = generator.generate(path ++ List("body"), new Wirespec.GeneratorFieldNullable(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldDict(generate = (p1) => generator.generate(p1, new Wirespec.GeneratorFieldArray(generate = (p2) => generator.generate(p2, new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      ))))))))
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
            |import community.flock.wirespec.generated.model.MyAwesomeEnum
            |object MyAwesomeEnumGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): MyAwesomeEnum =
            |    MyAwesomeEnum.valueOf(generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldEnum(
            |      values = List("ONE", "Two", "THREE_MORE", "UnitedKingdom", "-1", "0", "10", "-999", "88"),
            |      annotations = List.empty[Map[String, Any]],
            |      `type` = scala.reflect.classTag[MyAwesomeEnum]
            |    )))
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
            |import community.flock.wirespec.generated.model.TodoId
            |object TodoIdGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TodoId =
            |    new TodoId(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldString(
            |      regex = Some("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}"),
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TodoNoRegex
            |object TodoNoRegexGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TodoNoRegex =
            |    new TodoNoRegex(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldString(
            |      regex = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TestInt
            |object TestIntGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TestInt =
            |    new TestInt(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldInteger(
            |      min = None,
            |      max = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TestInt0
            |object TestInt0Generator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TestInt0 =
            |    new TestInt0(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldInteger(
            |      min = None,
            |      max = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TestInt1
            |object TestInt1Generator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TestInt1 =
            |    new TestInt1(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldInteger(
            |      min = Some(0),
            |      max = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TestInt2
            |object TestInt2Generator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TestInt2 =
            |    new TestInt2(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldInteger(
            |      min = Some(1),
            |      max = Some(3),
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TestNum
            |object TestNumGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TestNum =
            |    new TestNum(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldNumber(
            |      min = None,
            |      max = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TestNum0
            |object TestNum0Generator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TestNum0 =
            |    new TestNum0(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldNumber(
            |      min = None,
            |      max = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TestNum1
            |object TestNum1Generator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TestNum1 =
            |    new TestNum1(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldNumber(
            |      min = None,
            |      max = Some(0.5),
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TestNum2
            |object TestNum2Generator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TestNum2 =
            |    new TestNum2(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldNumber(
            |      min = Some(-0.2),
            |      max = Some(0.5),
            |      annotations = List.empty[Map[String, Any]]
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
            |import community.flock.wirespec.generated.model.UserAccount
            |object UserAccountGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): UserAccount = {
            |    val variant = generator.generate(path ++ List("variant"), new Wirespec.GeneratorFieldUnion(
            |      variants = List("UserAccountPassword", "UserAccountToken"),
            |      annotations = List.empty[Map[String, Any]],
            |      `type` = scala.reflect.classTag[UserAccount]
            |    ))
            |    variant match {
            |        case "UserAccountPassword" => {
            |          UserAccountPasswordGenerator.generate(generator, path ++ List("UserAccountPassword"))
            |        }
            |        case "UserAccountToken" => {
            |          UserAccountTokenGenerator.generate(generator, path ++ List("UserAccountToken"))
            |        }
            |    }
            |    throw new IllegalStateException("Unknown variant")
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.UserAccountPassword
            |object UserAccountPasswordGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): UserAccountPassword =
            |    new UserAccountPassword(
            |      username = generator.generate(path ++ List("username"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      password = generator.generate(path ++ List("password"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      ))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.UserAccountToken
            |object UserAccountTokenGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): UserAccountToken =
            |    new UserAccountToken(token = generator.generate(path ++ List("token"), new Wirespec.GeneratorFieldString(
            |      regex = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.User
            |import community.flock.wirespec.generated.model.UserAccount
            |object UserGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): User =
            |    new User(
            |      username = generator.generate(path ++ List("username"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      account = generator.generate(path ++ List("account"), new Wirespec.GeneratorFieldShape(
            |        annotations = Map.empty,
            |        generate = (p0) => UserAccountGenerator.generate(generator, p0),
            |        `type` = scala.reflect.classTag[UserAccount]
            |      ))
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
            |import community.flock.wirespec.generated.model.TodoDto
            |object TodoDtoGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TodoDto =
            |    new TodoDto(description = generator.generate(path ++ List("description"), new Wirespec.GeneratorFieldString(
            |      regex = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))
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
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |object PotentialTodoDtoGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): PotentialTodoDto =
            |    new PotentialTodoDto(
            |      name = generator.generate(path ++ List("name"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      done = generator.generate(path ++ List("done"), new Wirespec.GeneratorFieldBoolean(annotations = List.empty[Map[String, Any]]))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Token
            |object TokenGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Token =
            |    new Token(iss = generator.generate(path ++ List("iss"), new Wirespec.GeneratorFieldString(
            |      regex = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.TodoDto
            |object TodoDtoGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): TodoDto =
            |    new TodoDto(
            |      id = generator.generate(path ++ List("id"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      name = generator.generate(path ++ List("name"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      done = generator.generate(path ++ List("done"), new Wirespec.GeneratorFieldBoolean(annotations = List.empty[Map[String, Any]]))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Error
            |object ErrorGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Error =
            |    new Error(
            |      code = generator.generate(path ++ List("code"), new Wirespec.GeneratorFieldInteger(
            |        min = None,
            |        max = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      description = generator.generate(path ++ List("description"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      ))
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
            |    address.validate().map(e => s"address.${'$'}{e}").toList
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.DutchPostalCode
            |object DutchPostalCodeGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): DutchPostalCode =
            |    new DutchPostalCode(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldString(
            |      regex = Some("^([0-9]{4}[A-Z]{2})${'$'}"),
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Address
            |import community.flock.wirespec.generated.model.DutchPostalCode
            |object AddressGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Address =
            |    new Address(
            |      street = generator.generate(path ++ List("street"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      houseNumber = generator.generate(path ++ List("houseNumber"), new Wirespec.GeneratorFieldInteger(
            |        min = None,
            |        max = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      postalCode = generator.generate(path ++ List("postalCode"), new Wirespec.GeneratorFieldShape(
            |        annotations = Map.empty,
            |        generate = (p0) => DutchPostalCodeGenerator.generate(generator, p0),
            |        `type` = scala.reflect.classTag[DutchPostalCode]
            |      ))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Person
            |import community.flock.wirespec.generated.model.Address
            |object PersonGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Person =
            |    new Person(
            |      name = generator.generate(path ++ List("name"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      address = generator.generate(path ++ List("address"), new Wirespec.GeneratorFieldShape(
            |        annotations = Map("street" -> List.empty[Map[String, Any]], "houseNumber" -> List.empty[Map[String, Any]], "postalCode" -> List.empty[Map[String, Any]]),
            |        generate = (p0) => AddressGenerator.generate(generator, p0),
            |        `type` = scala.reflect.classTag[Address]
            |      )),
            |      tags = generator.generate(path ++ List("tags"), new Wirespec.GeneratorFieldArray(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      ))))
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
            |    (if (!age.validate()) List("age") else List.empty[String]) ++ contactInfo.validate().map(e => s"contactInfo.${'$'}{e}").toList ++ tags.zipWithIndex.flatMap { case (el, i) => if (!el.validate()) List(s"tags[${'$'}{i}]") else List.empty[String] }
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
            |    employees.zipWithIndex.flatMap { case (el, i) => el.validate().map(e => s"employees[${'$'}{i}].${'$'}{e}").toList }
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
            |    departments.zipWithIndex.flatMap { case (el, i) => el.validate().map(e => s"departments[${'$'}{i}].${'$'}{e}").toList }
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Email
            |object EmailGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Email =
            |    new Email(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldString(
            |      regex = Some("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}${'$'}"),
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.PhoneNumber
            |object PhoneNumberGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): PhoneNumber =
            |    new PhoneNumber(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldString(
            |      regex = Some("^\\+[1-9]\\d{1,14}${'$'}"),
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Tag
            |object TagGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Tag =
            |    new Tag(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldString(
            |      regex = Some("^[a-z][a-z0-9-]{0,19}${'$'}"),
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.EmployeeAge
            |object EmployeeAgeGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): EmployeeAge =
            |    new EmployeeAge(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldInteger(
            |      min = Some(18),
            |      max = Some(65),
            |      annotations = List.empty[Map[String, Any]]
            |    )))
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.ContactInfo
            |import community.flock.wirespec.generated.model.Email
            |import community.flock.wirespec.generated.model.PhoneNumber
            |object ContactInfoGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): ContactInfo =
            |    new ContactInfo(
            |      email = generator.generate(path ++ List("email"), new Wirespec.GeneratorFieldShape(
            |        annotations = Map.empty,
            |        generate = (p0) => EmailGenerator.generate(generator, p0),
            |        `type` = scala.reflect.classTag[Email]
            |      )),
            |      phone = generator.generate(path ++ List("phone"), new Wirespec.GeneratorFieldNullable(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldShape(
            |        annotations = Map.empty,
            |        generate = (p1) => PhoneNumberGenerator.generate(generator, p1),
            |        `type` = scala.reflect.classTag[PhoneNumber]
            |      ))))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Employee
            |import community.flock.wirespec.generated.model.EmployeeAge
            |import community.flock.wirespec.generated.model.ContactInfo
            |import community.flock.wirespec.generated.model.Tag
            |object EmployeeGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Employee =
            |    new Employee(
            |      name = generator.generate(path ++ List("name"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      age = generator.generate(path ++ List("age"), new Wirespec.GeneratorFieldShape(
            |        annotations = Map.empty,
            |        generate = (p0) => EmployeeAgeGenerator.generate(generator, p0),
            |        `type` = scala.reflect.classTag[EmployeeAge]
            |      )),
            |      contactInfo = generator.generate(path ++ List("contactInfo"), new Wirespec.GeneratorFieldShape(
            |        annotations = Map("email" -> List.empty[Map[String, Any]], "phone" -> List.empty[Map[String, Any]]),
            |        generate = (p0) => ContactInfoGenerator.generate(generator, p0),
            |        `type` = scala.reflect.classTag[ContactInfo]
            |      )),
            |      tags = generator.generate(path ++ List("tags"), new Wirespec.GeneratorFieldArray(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldShape(
            |        annotations = Map.empty,
            |        generate = (p1) => TagGenerator.generate(generator, p1),
            |        `type` = scala.reflect.classTag[Tag]
            |      ))))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Department
            |import community.flock.wirespec.generated.model.Employee
            |object DepartmentGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Department =
            |    new Department(
            |      name = generator.generate(path ++ List("name"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      employees = generator.generate(path ++ List("employees"), new Wirespec.GeneratorFieldArray(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldShape(
            |        annotations = Map("name" -> List.empty[Map[String, Any]], "age" -> List.empty[Map[String, Any]], "contactInfo" -> List.empty[Map[String, Any]], "tags" -> List.empty[Map[String, Any]]),
            |        generate = (p1) => EmployeeGenerator.generate(generator, p1),
            |        `type` = scala.reflect.classTag[Employee]
            |      ))))
            |    )
            |}
            |
            |package community.flock.wirespec.generated.generator
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Company
            |import community.flock.wirespec.generated.model.Department
            |object CompanyGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Company =
            |    new Company(
            |      name = generator.generate(path ++ List("name"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      departments = generator.generate(path ++ List("departments"), new Wirespec.GeneratorFieldArray(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldShape(
            |        annotations = Map("name" -> List.empty[Map[String, Any]], "employees" -> List.empty[Map[String, Any]]),
            |        generate = (p1) => DepartmentGenerator.generate(generator, p1),
            |        `type` = scala.reflect.classTag[Department]
            |      ))))
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
            |  sealed trait GeneratorField[T <: Option[Any]]
            |  case class GeneratorFieldString(
            |      val regex: Option[String],
            |      val annotations: List[Map[String, Any]]
            |    ) extends GeneratorField[String]
            |  case class GeneratorFieldInteger(
            |      val min: Option[Long],
            |      val max: Option[Long],
            |      val annotations: List[Map[String, Any]]
            |    ) extends GeneratorField[Long]
            |  case class GeneratorFieldNumber(
            |      val min: Option[Double],
            |      val max: Option[Double],
            |      val annotations: List[Map[String, Any]]
            |    ) extends GeneratorField[Double]
            |  case class GeneratorFieldBoolean(
            |      val annotations: List[Map[String, Any]]
            |    ) extends GeneratorField[Boolean]
            |  case class GeneratorFieldBytes(
            |      val annotations: List[Map[String, Any]]
            |    ) extends GeneratorField[Array[Byte]]
            |  case class GeneratorFieldEnum(
            |      val values: List[String],
            |      val annotations: List[Map[String, Any]],
            |      val `type`: scala.reflect.ClassTag[?]
            |    ) extends GeneratorField[String]
            |  case class GeneratorFieldUnion(
            |      val variants: List[String],
            |      val annotations: List[Map[String, Any]],
            |      val `type`: scala.reflect.ClassTag[?]
            |    ) extends GeneratorField[String]
            |  case class GeneratorFieldArray[T](
            |      val generate: (List[String]) => T
            |    ) extends GeneratorField[List[T]]
            |  case class GeneratorFieldNullable[T](
            |      val generate: (List[String]) => T
            |    ) extends GeneratorField[Option[T]]
            |  case class GeneratorFieldShape[T](
            |      val annotations: Map[String, List[Map[String, Any]]],
            |      val generate: (List[String]) => T,
            |      val `type`: scala.reflect.ClassTag[?]
            |    ) extends GeneratorField[T]
            |  case class GeneratorFieldDict[V](
            |      val generate: (List[String]) => V
            |    ) extends GeneratorField[Map[String, V]]
            |  trait Generator {
            |      def generate[T <: Option[Any]](path: List[String], field: GeneratorField[T]): T
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
            |import community.flock.wirespec.generated.model.Address
            |object AddressGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Address =
            |    new Address(
            |      street = generator.generate(path ++ List("street"), new Wirespec.GeneratorFieldString(
            |        regex = None,
            |        annotations = List.empty[Map[String, Any]]
            |      )),
            |      number = generator.generate(path ++ List("number"), new Wirespec.GeneratorFieldInteger(
            |        min = None,
            |        max = None,
            |        annotations = List.empty[Map[String, Any]]
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
            |import community.flock.wirespec.generated.model.Color
            |object ColorGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Color =
            |    Color.valueOf(generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldEnum(
            |      values = List("RED", "GREEN", "BLUE"),
            |      annotations = List.empty[Map[String, Any]],
            |      `type` = scala.reflect.classTag[Color]
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Shape
            |object ShapeGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Shape = {
            |    val variant = generator.generate(path ++ List("variant"), new Wirespec.GeneratorFieldUnion(
            |      variants = List("Circle", "Square"),
            |      annotations = List.empty[Map[String, Any]],
            |      `type` = scala.reflect.classTag[Shape]
            |    ))
            |    variant match {
            |        case "Circle" => {
            |          CircleGenerator.generate(generator, path ++ List("Circle"))
            |        }
            |        case "Square" => {
            |          SquareGenerator.generate(generator, path ++ List("Square"))
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
            |import community.flock.wirespec.generated.model.UUID
            |object UUIDGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): UUID =
            |    new UUID(value = generator.generate(path ++ List("value"), new Wirespec.GeneratorFieldString(
            |      regex = Some("^[0-9a-f]{8}${'$'}"),
            |      annotations = List.empty[Map[String, Any]]
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
            |import community.flock.wirespec.scala.Wirespec
            |import scala.reflect.ClassTag
            |import community.flock.wirespec.generated.model.Inventory
            |object InventoryGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Inventory =
            |    new Inventory(items = generator.generate(path ++ List("items"), new Wirespec.GeneratorFieldArray(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldInteger(
            |      min = None,
            |      max = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))))
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
            |import community.flock.wirespec.generated.model.Lookup
            |object LookupGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Lookup =
            |    new Lookup(entries = generator.generate(path ++ List("entries"), new Wirespec.GeneratorFieldDict(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldInteger(
            |      min = None,
            |      max = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))))
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
            |import community.flock.wirespec.generated.model.Person
            |object PersonGenerator {
            |  def generate(generator: Wirespec.Generator, path: List[String]): Person =
            |    new Person(nickname = generator.generate(path ++ List("nickname"), new Wirespec.GeneratorFieldNullable(generate = (p0) => generator.generate(p0, new Wirespec.GeneratorFieldString(
            |      regex = None,
            |      annotations = List.empty[Map[String, Any]]
            |    )))))
            |}
            |
            """.trimMargin()

        emitGeneratorSource(person, "PersonGenerator") shouldBe expected
    }
}
