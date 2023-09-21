package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ParserTest {

    private fun parser() = Parser(TestLogger)

    @Test
    fun testParserWithCorrectInput() {
        val source = """
            type Bla {
              foo: String,
              bar: String
            }

        """.trimIndent()

        Wirespec.tokenize(source)
            .let(parser()::parse)
            .onRight { assertEquals(1, it.size) }
            .onLeft { fail("Should not be Either.Left") }
    }

    @Test
    fun testParserWithFaultyInput() {
        val source = """
            type Bla {
              foo: String
              bar: String
            }

        """.trimIndent()

        Wirespec.tokenize(source)
            .let(parser()::parse)
            .onLeft {
                assertEquals(1, it.size)
                assertEquals("RightCurly expected, not: CustomValue at line 3 and position 3", it.first().message)
            }
            .onRight { fail("Should not be Either.Right") }
    }
}
