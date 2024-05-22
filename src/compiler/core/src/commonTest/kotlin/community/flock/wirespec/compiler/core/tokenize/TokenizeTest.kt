package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.StartOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.WsComment
import kotlin.test.Test

class TokenizeTest {

    @Test
    fun testEmptySource() = testTokenizer("", StartOfProgram, EndOfProgram, removeWhiteSpace = false)

    @Test
    fun testSourceLengthOfOneCharacterSource() = testTokenizer("t", CustomValue, EndOfProgram)

    @Test
    fun testCommentSource() = testTokenizer(
        """
            |/**
            | * This is a comment
            | */
        """.trimMargin(),
        WsComment, EndOfProgram
    )
}
