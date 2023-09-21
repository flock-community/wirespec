package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ParseEndpointTest {

    private fun parser() = Parser(TestLogger)

    @Test
    fun testParserWithCorrectInput() {
        val source = """
            type Todo {
                id: String
            }
            
            type Error {
                message: String
            }
            
            endpoint GetTodos 

        """.trimIndent()

        Wirespec.tokenize(source)
            .let(parser()::parse)
            .onRight { assertEquals(3, it.size) }
            .onLeft { fail("Should be Right, but was Left: ${it.first()}") }
            .getOrNull()!!
    }
}
