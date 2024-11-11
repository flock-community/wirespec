package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse.Endpoint.Method.GET
import community.flock.wirespec.compiler.core.parse.Endpoint.Method.POST
import community.flock.wirespec.compiler.core.parse.Endpoint.Segment.Literal
import community.flock.wirespec.compiler.core.parse.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Reference.Primitive.Type.String
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseEndpointTest {

    private fun parser() = Parser(noLogger)

    @Test
    fun testEndpointParser() {
        val source = """
            |type Todo { name: String }
            |endpoint GetTodos GET /todos -> {
            |    200 -> Todo[]
            |}
        """.trimMargin()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[1]
            .shouldBeInstanceOf<Endpoint>()
            .run {
                identifier.value shouldBe "GetTodos"
                method shouldBe GET
                path shouldBe listOf(Literal("todos"))
                requests shouldBe listOf(
                    Endpoint.Request(
                        content = null
                    )
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

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[1]
            .shouldBeInstanceOf<Endpoint>()
            .run {
                identifier.value shouldBe "GetTodos"
                method shouldBe GET
                path shouldBe listOf(Literal("To-Do_List"))
                requests shouldBe listOf(
                    Endpoint.Request(
                        content = null
                    )
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

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[1]
            .shouldBeInstanceOf<Endpoint>()
            .run {
                identifier.value shouldBe "PostTodo"
                method shouldBe POST
                requests.shouldNotBeEmpty().also { it.size shouldBe 1 }.first().run {
                    content.shouldNotBeNull().run {
                        type shouldBe "application/json"
                        reference.shouldBeInstanceOf<Reference.Custom>().run {
                            value shouldBe "Todo"
                        }
                        isNullable shouldBe false
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

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[1]
            .shouldBeInstanceOf<Endpoint>()
            .run {
                identifier.value shouldBe "GetTodo"
                method shouldBe GET
                path shouldBe listOf(
                    Literal("todos"), Endpoint.Segment.Param(
                        identifier = FieldIdentifier("id"),
                        reference = Primitive(
                            type = String,
                            isIterable = false,
                            isDictionary = false,
                        )
                    )
                )
                requests shouldBe listOf(
                    Endpoint.Request(
                        content = null
                    )
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

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[1]
            .shouldBeInstanceOf<Endpoint>()
            .queries.shouldNotBeEmpty().also { it.size shouldBe 2 }.take(2).let {
                val (one, two) = it
                one.run {
                    identifier.value shouldBe "name"
                    reference.shouldBeInstanceOf<Primitive>().type shouldBe String
                    isNullable shouldBe false
                }
                two.run {
                    identifier.value shouldBe "date"
                    reference.shouldBeInstanceOf<Primitive>().type shouldBe String
                    isNullable shouldBe false
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

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[1]
            .shouldBeInstanceOf<Endpoint>()
            .headers.shouldNotBeEmpty().also { it.size shouldBe 2 }.take(2).let {
                val (one, two) = it
                one.run {
                    identifier.value shouldBe "name"
                    reference.shouldBeInstanceOf<Primitive>().type shouldBe String
                    isNullable shouldBe false
                }
                two.run {
                    identifier.value shouldBe "date"
                    reference.shouldBeInstanceOf<Primitive>().type shouldBe String
                    isNullable shouldBe false
                }
            }
    }

    @Test
    fun testDictionaryResponse() {
        val source = """
            |endpoint GetTodos GET /todos ? {done:{String}} # {token:{String}} -> {
            |    200 -> {String}
            |}
        """.trimMargin()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }[0]
            .shouldBeInstanceOf<Endpoint>().run {
                responses.shouldNotBeEmpty()
                responses.first().content?.reference?.isDictionary?.shouldBeTrue()
                queries.first().reference.isDictionary.shouldBeTrue()
                headers.first().reference.isDictionary.shouldBeTrue()
            }

    }
}
