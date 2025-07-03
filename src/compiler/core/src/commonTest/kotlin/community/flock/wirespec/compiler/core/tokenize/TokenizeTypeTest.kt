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
        RightCurly,
        EndOfProgram,
    )

    @Test
    fun testRefinedTypeTokenize() = testTokenizer(
        "type DutchPostalCode -> String(/^([0-9]{4}[A-Z]{2})$/g)",
        TypeDefinition, WirespecType, Arrow, WsString, LeftParentheses, RegExp, RightParentheses,
        EndOfProgram,
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
        TypeDefinition, WirespecType, LeftCurly,
        DromedaryCaseIdentifier, Colon, LeftCurly, WsString, QuestionMark, Brackets, QuestionMark,
        RightCurly, QuestionMark, RightCurly,
        EndOfProgram,
    )

    @Test
    fun testBoundsIntegerSimple() = testTokenizer(
        """Integer(0, 1)""",
        WsInteger(Precision.P64),
        LeftParentheses,
        Integer,
        Comma,
        Integer,
        RightParentheses,
        EndOfProgram,
    )

    @Test
    fun testBoundsIntegerStatusCode() = testTokenizer(
        """Integer(200, 500)""",
        WsInteger(Precision.P64),
        LeftParentheses,
        Integer,
        Comma,
        Integer,
        RightParentheses,
        EndOfProgram,
    )

    @Test
    fun testBoundsNumberSimple() = testTokenizer(
        """Number(0.0, 1.0)""",
        WsNumber(Precision.P64),
        LeftParentheses,
        Number,
        Comma,
        Number,
        RightParentheses,
        EndOfProgram,
    )

    @Test
    fun testRegexSimple() = testTokenizer(
        """String(/.*/)""",
        WsString,
        LeftParentheses,
        RegExp,
        RightParentheses,
        EndOfProgram,
    )

    @Test
    fun testRegexWithFlag() = testTokenizer(
        """String(/.*/g)""",
        WsString,
        LeftParentheses,
        RegExp,
        RightParentheses,
        EndOfProgram,
    )

    @Test
    fun testRegexWithEscapedForwardSlash() = testTokenizer(
        """String(/.*\//g)""",
        WsString,
        LeftParentheses,
        RegExp,
        RightParentheses,
        EndOfProgram,
    )

    @Test
    fun testRegexWithExtraSpaces() = testTokenizer(
        """String(  /.*\//g  )""",
        WsString,
        LeftParentheses,
        RegExp,
        RightParentheses,
        EndOfProgram,
    )

    @Test
    fun testListOfComplexRegex() {
        val regex = listOf(
            """(test) (test)""",
            """(?(?<=<(\w+)>).+?(?=<\/\1>)|.+)""",
            """^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\da-zA-Z]).{8,15}$""",
            """\b(\w+)\s+(?<!\1\s+)\1\b""",
            """(\((?:[^()]++|(?1))*\)|\[(?:[^\[\]]++|(?1))*\])""",
            """(?(DEFINE)(?<string>"[^"\\]*(?:\\.[^"\\]*)*")(?<number>-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)(?<boolean>true|false)(?<null>null))^(?&string)$""",
            """(?<quote>["'])(?>(?:(?!\k<quote>|\\).)*)(?:\\[\s\S])*(?:\k<quote>)""",
            """\b\p{Lu}\p{L}*+(?:\s+\p{Lu}\p{L}*+)*\b""",
            """(?ix-s)^foo(?#comment)(?i:bar)(?-i:BAZ)$""",
            """(a(*PRUNE)b|c(*FAIL))|(?|x(y)z|p(q)r)""",
        )
        regex.map {
            testTokenizer(
                """String(/$it/g)""",
                WsString,
                LeftParentheses,
                RegExp,
                RightParentheses,
                EndOfProgram,
            )
            testTokenizer(
                """String(/$it/)""",
                WsString,
                LeftParentheses,
                RegExp,
                RightParentheses,
                EndOfProgram,
            )
        }
    }
}
