package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WsCustomType
import kotlin.test.Test

class TokenizeTypeTest {

    @Test
    fun testTypeTokenize() = testTokenizer(
        """
            |type Foo {
            |    bar: String
            |}
        """.trimMargin(),
        TypeDefinition, WsCustomType, LeftCurly, CustomValue,
        Colon, WsString, RightCurly, EndOfProgram,
    )

    @Test
    fun testRefinedTypeTokenize() = testTokenizer(
        "type DutchPostalCode /^([0-9]{4}[A-Z]{2})$/g",
        TypeDefinition, WsCustomType, ForwardSlash, Character, Character,
        Character, Character, Character, Character, Character,
        LeftCurly, Character, RightCurly, Character, WsCustomType,
        Character, WsCustomType, Character, LeftCurly, Character,
        RightCurly, Character, Character, Path, EndOfProgram
    )

    @Test
    fun testUnionTypeTokenize() = testTokenizer(
        "type Foo = Bar | Bal",
        TypeDefinition, WsCustomType, Equals, WsCustomType,
        Pipe, WsCustomType, EndOfProgram,
    )

    @Test
    fun testNullableTypes() = testTokenizer(
        """
            |type Foo {
            |    bar: { String?[]? }?
            |}
        """.trimMargin(),
        TypeDefinition, WsCustomType, LeftCurly, CustomValue, Colon,
        LeftCurly, WsString, QuestionMark, Brackets, QuestionMark,
        RightCurly, QuestionMark, RightCurly, EndOfProgram
    )
}
