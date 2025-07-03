package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecField
import community.flock.wirespec.compiler.core.WirespecType
import community.flock.wirespec.compiler.core.tokenize.Precision.P64
import kotlin.test.Test

class TokenizeTest {

    @Test
    fun testEmptySource() = testTokenizer("", StartOfProgram, EndOfProgram, removeWhiteSpace = false)

    @Test
    fun testSourceLengthOfOneCharacterSource() = testTokenizer("t", DromedaryCaseIdentifier, EndOfProgram)

    @Test
    fun testCommentSource() = testTokenizer(
        """
            |/**
            | * This is a comment
            | */
        """.trimMargin(),
        Comment,
        EndOfProgram,
    )

    @Test
    fun testCommentRefinedSource() = testTokenizer(
        """
            |/**
            |  * comment Name
            |  */
            |type Name -> String(/^[0-9a-zA-Z]{1,50}${'$'}/g)
            |/**
            |  * comment Address
            |  */
            |type Address {
            |  street: Name?,
            |  houseNumber: Integer
            |}
        """.trimMargin(),
        Comment,
        TypeDefinition, WirespecType, Arrow, WsString, LeftParentheses, RegExp, RightParentheses,
        Comment,
        TypeDefinition, WirespecType, LeftCurly,
        DromedaryCaseIdentifier, Colon, WirespecType, QuestionMark, Comma,
        DromedaryCaseIdentifier, Colon, WsInteger(P64),
        RightCurly,
        EndOfProgram,
    )
}
