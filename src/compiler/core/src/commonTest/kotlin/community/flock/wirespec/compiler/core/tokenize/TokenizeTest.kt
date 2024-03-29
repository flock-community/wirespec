package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.StartOfProgram
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TokenizeTest {

    @Test
    fun testEmptySource() {
        val source = ""

        val expected = listOf(StartOfProgram, EndOfProgram)

        Wirespec.tokenize(source)
            .shouldNotBeEmpty()
            .also { it.size shouldBe expected.size }
            .onEachIndexed { index, token -> token.type shouldBe expected[index] }
    }

    @Test
    fun testSourceLengthOfOneCharacterSource() {
        val source = "t"

        val expected = listOf(CustomValue, EndOfProgram)

        Wirespec.tokenize(source).removeWhiteSpace()
            .shouldNotBeEmpty()
            .also { it.size shouldBe expected.size }
            .onEachIndexed { index, token -> token.type shouldBe expected[index] }
    }
}
