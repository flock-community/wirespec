package community.flock.wirespec.compiler.core.tokenize

import kotlin.test.Test

class TokenizeTest {

    @Test
    fun testEmptySource() =
        testTokenizer("", StartOfProgram, EndOfProgram, removeWhiteSpace = false)

    @Test
    fun testSourceLengthOfOneCharacterSource() = testTokenizer("t", CustomValue, EndOfProgram)

    @Test
    fun testCommentSource() = testTokenizer(
        """
            |/**
            | * This is a comment
            | */
        """.trimMargin(),
        Comment, EndOfProgram
    )
}
