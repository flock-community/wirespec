package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint.Method.GET
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint.Segment.Literal
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseEndpointTest {

    private fun parser() = Parser(noLogger)

    @Test
    fun testEndpointParserWithCorrectInput() {
        val source = """
            endpoint GetTodos GET /todos -> {
                200 -> Todo[]
            }

        """.trimIndent()

        Wirespec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .run {
                name shouldBe "GetTodos"
                method shouldBe GET
                path shouldBe listOf(Literal("todos"))
                requests.shouldBeEmpty()
            }
    }

    @Test
    fun testPathParamsParserWithCorrectInput() {
        val source = """
            endpoint GetTodos GET /todos/{id: String} -> {
                200 -> Todo
            }

        """.trimIndent()

        Wirespec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .run {
                name shouldBe "GetTodos"
                method shouldBe GET
                path shouldBe listOf(
                    Literal("todos"), Endpoint.Segment.Param(
                        identifier = Identifier("id"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false,
                            isMap = false,
                        )
                    )
                )
                requests.shouldBeEmpty()
            }
    }
}
