package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.tokenize.types.Character
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.CustomRegex
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.Equals
import community.flock.wirespec.compiler.core.tokenize.types.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.Pipe
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StartOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpaceExceptNewLine
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import kotlin.test.Test

class TokenizeTypeTest {

    @Test
    fun testTypeTokenize() = testTokenizer(
        """
            |type Foo {
            |    bar: String
            |}
        """.trimMargin(),
        WsTypeDef, CustomType, LeftCurly, CustomValue,
        Colon, WsString, RightCurly, EndOfProgram,
    )

    @Test
    fun testRefinedTypeTokenize() = testTokenizer(
        "type DutchPostalCode /^([0-9]{4}[A-Z]{2})$/g",
        WsTypeDef, CustomType, ForwardSlash, Character, Character,
        Character, Character, Character, Character, Character,
        LeftCurly, Character, RightCurly, Character, CustomType,
        Character, CustomType, Character, LeftCurly, Character,
        RightCurly, Character, Character, Path, EndOfProgram
    )

    @Test
    fun testUnionTypeTokenize() = testTokenizer(
        "type Foo = Bar | Bal",
        WsTypeDef, CustomType, Equals, CustomType,
        Pipe, CustomType, EndOfProgram,
    )
}
