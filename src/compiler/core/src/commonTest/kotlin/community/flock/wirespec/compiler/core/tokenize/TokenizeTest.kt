package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WsCustomType
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
        Comment,
        EndOfProgram,
    )

    @Test
    fun testCommentRefinedSource() = testTokenizer(
        """
            |/**
            |  * comment Name
            |  */
            |type Name /^[0-9a-zA-Z]{1,50}${'$'}/g
            |/**
            |  * comment Address
            |  */
            |type Address {
            |  street: Name?,
            |  houseNumber: Integer
            |}
        """.trimMargin(),
        Comment,
        TypeDefinition, WsCustomType, ForwardSlash, Character, Character, Character, Character, Character, CustomValue, Character, CustomValue, Character, WsCustomType, Character, LeftCurly, Character, Comma, Character, Character, RightCurly, Character, Path,
        Comment,
        TypeDefinition, WsCustomType,
        LeftCurly,
        CustomValue, Colon, WsCustomType, QuestionMark, Comma,
        CustomValue, Colon, WsInteger(Precision.P64),
        RightCurly,
        EndOfProgram,
    )
}
