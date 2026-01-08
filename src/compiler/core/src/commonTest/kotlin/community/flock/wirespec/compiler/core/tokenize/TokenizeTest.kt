package community.flock.wirespec.compiler.core.tokenize

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
        // language=ws
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
        // language=ws
        """
        |/**
        |  * comment Name
        |  */
        |type Name = String(/^[0-9a-zA-Z]{1,50}$/g)
        |/**
        |  * comment Address
        |  */
        |type Address {
        |  street: Name?,
        |  houseNumber: Integer
        |}
        """.trimMargin(),
        Comment,
        TypeDefinition, WirespecType, Equals, WsString, LeftParenthesis, RegExp, RightParenthesis,
        Comment,
        TypeDefinition, WirespecType, LeftCurly,
        DromedaryCaseIdentifier, Colon, WirespecType, QuestionMark, Comma,
        DromedaryCaseIdentifier, Colon, WsInteger(P64),
        RightCurly,
        EndOfProgram,
    )

    @Test
    fun testDoubleQuotedLiteralString() = testTokenizer(
        "\"Hello World\"",
        LiteralString,
        EndOfProgram,
    )

    @Test
    fun testEmptyDoubleQuotedString() = testTokenizer(
        "\"\"",
        LiteralString,
        EndOfProgram,
    )

    @Test
    fun testEscapedDoubleQuoteInString() = testTokenizer(
        "\"Hello \\\"World\\\"\"",
        LiteralString,
        EndOfProgram,
    )

    @Test
    fun testLiteralStringWithEscapedBackslash() = testTokenizer(
        "\"Path\\\\to\\\\file\"",
        LiteralString,
        EndOfProgram,
    )

    @Test
    fun testLiteralStringWithNewlineEscape() = testTokenizer(
        "\"Hello\\nWorld\"",
        LiteralString,
        EndOfProgram,
    )

    @Test
    fun testLiteralStringWithTabEscape() = testTokenizer(
        "\"Hello\\tWorld\"",
        LiteralString,
        EndOfProgram,
    )

    @Test
    fun testLiteralStringWithSpecialCharacters() = testTokenizer(
        "\"Hello @#\$%^&*()_+{}|:<>?[]\\\\;',./ World\"",
        LiteralString,
        EndOfProgram,
    )
}
