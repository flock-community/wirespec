package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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
            .shouldBeRight()
            .size shouldBe 1
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
            .shouldBeLeft()
            .also { it.size shouldBe 1 }
            .first()
            .message shouldBe "RightCurly expected, not: CustomValue at line 3 and position 3"
    }
}
