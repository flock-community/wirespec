package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint.Method.GET
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint.Segment.Literal
import community.flock.wirespec.compiler.core.tokenize.tokenize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ParseEndpointTest {

    private fun parser() = Parser(TestLogger)

    @Test
    fun testParserWithCorrectInput() {
        val source = """
            endpoint GetTodos GET /todos {
                200 -> Todo
            }

        """.trimIndent()

        val endpoint = Wirespec.tokenize(source)
            .let(parser()::parse)
            .onRight { assertEquals(1, it.size) }
            .onLeft { fail("Should be Right, but was Left: ${it.first()}") }
            .getOrNull()!!.first().let {
                assertTrue { it is Endpoint }
                it as Endpoint
            }

        assertEquals("GetTodos", endpoint.name)
        assertEquals(GET, endpoint.method)
        assertEquals(listOf(Literal("todos")), endpoint.path)
        assertTrue(endpoint.requests.isEmpty())
    }
}
