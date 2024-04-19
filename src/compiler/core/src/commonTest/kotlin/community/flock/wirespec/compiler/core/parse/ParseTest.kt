package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.Logger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ParseTest {

    private fun parser() = Parser(object : Logger(false) {})

    @Test
    fun testParserWithCorrectInput() {
        val source = """
            type Bla {
              foo: String,
              bar: String
            }

        """.trimIndent()

        WirespecSpec.tokenize(source)
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

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeLeft()
            .also { it.size shouldBe 1 }
            .first()
            .message shouldBe "RightCurly expected, not: CustomValue at line 3 and position 3"
    }
}
