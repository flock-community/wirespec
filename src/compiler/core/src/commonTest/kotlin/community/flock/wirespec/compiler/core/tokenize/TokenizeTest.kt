package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.Invalid
import community.flock.wirespec.compiler.core.tokenize.types.StartOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.WsComment
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TokenizeTest {

    @Test
    fun testEmptySource() {
        val source = ""

        val expected = listOf(StartOfProgram, EndOfProgram)

        WirespecSpec.tokenize(source)
            .shouldNotBeEmpty()
            .also { it.size shouldBe expected.size }
            .map { it.type }.shouldNotContain(Invalid)
            .onEachIndexed { index, tokenType -> tokenType shouldBe expected[index] }
    }

    @Test
    fun testSourceLengthOfOneCharacterSource() {
        val source = "t"

        val expected = listOf(CustomValue, EndOfProgram)

        WirespecSpec.tokenize(source).removeWhiteSpace()
            .shouldNotBeEmpty()
            .also { it.size shouldBe expected.size }
            .map { it.type }.shouldNotContain(Invalid)
            .onEachIndexed { index, tokenType -> tokenType shouldBe expected[index] }
    }

    @Test
    fun testCommentSource() {
        val source = """
            |/**
            | * This is a comment
            | */
        """.trimMargin()

        val expected = listOf(WsComment, EndOfProgram)

        WirespecSpec.tokenize(source).removeWhiteSpace()
            .shouldNotBeEmpty()
            .also(::println)
            .also { it.size shouldBe expected.size }
            .map { it.type }.shouldNotContain(Invalid)
            .onEachIndexed { index, tokenType -> tokenType shouldBe expected[index] }
    }
}
