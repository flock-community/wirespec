package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint.Method.GET
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint.Segment.Literal
import community.flock.wirespec.compiler.core.tokenize.tokenize
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseEndpointTest {

    private fun parser() = Parser(TestLogger)

    @Test
    fun testParserWithCorrectInput() {
        val source = """
            endpoint GetTodos GET /todos {
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
                path shouldBe listOf(Literal("todos"))
                requests.shouldBeEmpty()
            }
    }
}
