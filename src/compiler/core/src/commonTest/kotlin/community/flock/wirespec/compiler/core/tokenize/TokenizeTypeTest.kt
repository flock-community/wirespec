package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecType
import kotlin.test.Test

class TokenizeTypeTest {

    @Test
    fun testTypeTokenize() = testTokenizer(
        """
            |type Foo {
            |    bar: String,
            |    `Baz`: String,
            |    `foo-bar`: String,
            |    `FOO-BAR`: String,
            |    `foo_bar`: String,
            |    `FOO_BAR`: String
            |}
        """.trimMargin(),
        TypeDefinition, WirespecType, LeftCurly,
        DromedaryCaseIdentifier, Colon, WsString, Comma,
        PascalCaseIdentifier, Colon, WsString, Comma,
        KebabCaseIdentifier, Colon, WsString, Comma,
        ScreamingKebabCaseIdentifier, Colon, WsString, Comma,
        SnakeCaseIdentifier, Colon, WsString, Comma,
        ScreamingSnakeCaseIdentifier, Colon, WsString,
        RightCurly, EndOfProgram,
    )

    @Test
    fun testRefinedTypeTokenize() = testTokenizer(
        "type DutchPostalCode /^([0-9]{4}[A-Z]{2})$/g",
        TypeDefinition, WirespecType, ForwardSlash, Character, Character,
        Character, Character, Character, Character, Character,
        LeftCurly, Character, RightCurly, Character, WirespecType,
        Character, WirespecType, Character, LeftCurly, Character,
        RightCurly, Character, Character, Path, EndOfProgram,
    )

    @Test
    fun testUnionTypeTokenize() = testTokenizer(
        "type Foo = Bar | Bal",
        TypeDefinition,
        WirespecType,
        Equals,
        WirespecType,
        Pipe,
        WirespecType,
        EndOfProgram,
    )

    @Test
    fun testNullableTypes() = testTokenizer(
        """
            |type Foo {
            |    bar: { String?[]? }?
            |}
        """.trimMargin(),
        TypeDefinition, WirespecType, LeftCurly, DromedaryCaseIdentifier,
        Colon, LeftCurly, WsString, QuestionMark, Brackets, QuestionMark,
        RightCurly, QuestionMark, RightCurly, EndOfProgram,
    )
}
