package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse.Endpoint.Method.GET
import community.flock.wirespec.compiler.core.parse.Endpoint.Method.POST
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive.Type.String
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
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
            endpoint GetTodos GET /todos -> {
                200 -> Todo[]
            }
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .run {
                name shouldBe "GetTodos"
                method shouldBe GET
                path shouldBe listOf(
                    Endpoint.Segment.Literal("todos")
                )
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
            endpoint GetTodos GET /To-Do_List -> {
                200 -> Todo[]
            }
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .run {
                name shouldBe "GetTodos"
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
            endpoint PostTodo POST Todo /todos -> {
                200 -> Todo
            }
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .run {
                name shouldBe "PostTodo"
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
            endpoint GetTodo GET /todos/{id: String} -> {
                200 -> Todo
            }
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .run {
                name shouldBe "GetTodo"
                method shouldBe GET
                path shouldBe listOf(
                    Endpoint.Segment.Literal("todos"),
                    Endpoint.Segment.Param(
                        identifier = Identifier("id"),
                        reference = Primitive(
                            type = String,
                            isIterable = false,
                            isMap = false,
                            coordinates= Token.Coordinates(
                                line = 1,
                                position = 40,
                                idxAndLength = Token.Coordinates.IdxAndLength(idx = 39, length = 6)
                            )
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
            endpoint GetTodos GET /todos?{name: String, date: String} -> {
                200 -> Todo[]
            }
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .query.shouldNotBeEmpty().also { it.size shouldBe 2 }.take(2).let {
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
            endpoint GetTodos GET /todos#{name: String, date: String} -> {
                200 -> Todo[]
            }
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
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
}
