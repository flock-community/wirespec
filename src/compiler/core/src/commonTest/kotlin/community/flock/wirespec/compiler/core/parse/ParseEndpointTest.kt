package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ParseEndpointTest {

    private fun parser() = Parser(TestLogger)

    @Test
    @Ignore
    fun testParserWithCorrectInput() {
        val source = """
            endpoint GetTodos GET /todos {
                200 -> Todo
            }

        """.trimIndent()

        Wirespec.tokenize(source)
            .let(parser()::parse)
            .onRight { assertEquals(1, it.size) }
            .onLeft { fail("Should be Right, but was Left: ${it.first()}") }
    }
}
