package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.Endpoint.Method.GET
import community.flock.wirespec.compiler.core.parse.Endpoint.Method.POST
import community.flock.wirespec.compiler.core.parse.Endpoint.Segment.Literal
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldHaveSize
import io.kotest.data.headers
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseEndpointTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent("", source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testEndpointParser() {
        val source = """
            |type Todo { name: String }
            |endpoint GetTodos GET /todos -> {
            |    200 -> Todo[]
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Endpoint>()
            .run {
                identifier.value shouldBe "GetTodos"
                method shouldBe GET
                path shouldBe listOf(Literal("todos"))
                requests shouldBe listOf(
                    Endpoint.Request(
                        content = null,
                    ),
                )
            }
    }

    @Test
    fun testEndpointPathParser() {
        val source = """
            |type Todo { name: String }
            |endpoint GetTodos GET /To-Do_List -> {
            |    200 -> Todo[]
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Endpoint>()
            .run {
                identifier.value shouldBe "GetTodos"
                method shouldBe GET
                path shouldBe listOf(Literal("To-Do_List"))
                requests shouldBe listOf(
                    Endpoint.Request(
                        content = null,
                    ),
                )
            }
    }

    @Test
    fun testPathParamsParser() {
        val source = """
            |type Todo { name: String }
            |endpoint PostTodo POST Todo /todos -> {
            |    200 -> Todo
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Endpoint>()
            .run {
                identifier.value shouldBe "PostTodo"
                method shouldBe POST
                requests.shouldNotBeEmpty().shouldHaveSize(1).first().run {
                    content.shouldNotBeNull().run {
                        type shouldBe "application/json"
                        reference.shouldBeInstanceOf<Reference.Custom>().run {
                            value shouldBe "Todo"
                        }
                    }
                }
            }
    }

    @Test
    fun testRequestBodyParser() {
        val source = """
            |type Todo { name: String }
            |endpoint GetTodo GET /todos/{id: String} -> {
            |    200 -> Todo
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Endpoint>()
            .run {
                identifier.value shouldBe "GetTodo"
                method shouldBe GET
                path shouldBe listOf(
                    Literal("todos"),
                    Endpoint.Segment.Param(
                        identifier = FieldIdentifier("id"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(),
                            isNullable = false,
                        ),
                    ),
                )
                requests shouldBe listOf(
                    Endpoint.Request(
                        content = null,
                    ),
                )
            }
    }

    @Test
    fun testQueryParamsParser() {
        val source = """
            |type Todo { name: String }
            |endpoint GetTodos GET /todos?{name: String, date: String} -> {
            |    200 -> Todo[]
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Endpoint>()
            .queries.shouldNotBeEmpty().shouldHaveSize(2).take(2).let {
                val (one, two) = it
                one.run {
                    identifier.value shouldBe "name"
                    reference.shouldBeInstanceOf<Reference.Primitive>().type shouldBe Reference.Primitive.Type.String()
                    reference.isNullable shouldBe false
                }
                two.run {
                    identifier.value shouldBe "date"
                    reference.shouldBeInstanceOf<Reference.Primitive>().type shouldBe Reference.Primitive.Type.String()
                    reference.isNullable shouldBe false
                }
            }
    }

    @Test
    fun testHeadersParser() {
        val source = """
            |type Todo { name: String }
            |endpoint GetTodos GET /todos#{name: String, date: String} -> {
            |    200 -> Todo[]
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Endpoint>()
            .headers.shouldNotBeEmpty().shouldHaveSize(2).take(2).let {
                val (one, two) = it
                one.run {
                    identifier.value shouldBe "name"
                    reference.shouldBeInstanceOf<Reference.Primitive>().type shouldBe Reference.Primitive.Type.String()
                    reference.isNullable shouldBe false
                }
                two.run {
                    identifier.value shouldBe "date"
                    reference.shouldBeInstanceOf<Reference.Primitive>().type shouldBe Reference.Primitive.Type.String()
                    reference.isNullable shouldBe false
                }
            }
    }

    @Test
    fun testResponseHeaderParser() {
        val source = """
            |type Todo { name: String }
            |endpoint GetTodo GET /todo/{id: Integer} -> {
            |   200 -> Todo #{token: String}
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Endpoint>()
            .responses.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .first()
            .headers.shouldHaveSize(1).first().run {
                identifier.value shouldBe "token"
                reference.shouldBeInstanceOf<Reference.Primitive>().type shouldBe Reference.Primitive.Type.String()
            }
    }

    @Test
    fun testDictionaryResponse() {
        val source = """
            |endpoint GetTodos GET /todos ? {done:{String}} # {token:{String}} -> {
            |    200 -> {String}
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Endpoint>().run {
                responses.shouldNotBeEmpty()
                responses.first().content?.reference?.let { it is Reference.Dict }?.shouldBeTrue()
                queries.first().reference.let { it is Reference.Dict }.shouldBeTrue()
                headers.first().reference.let { it is Reference.Dict }.shouldBeTrue()
            }
    }
}
