package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.WirespecType
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TokenizerRegexTest {

    @Test
    fun testRegexValue() {
        val tokens = WirespecSpec.tokenize("""type Test { test: String(/.*/) } """, TokenizeOptions(removeWhitespace = false))
        tokens.map { it.type } shouldBe listOf(StartOfProgram, TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, WhiteSpaceExceptNewLine, DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParentheses, RegExp, RightParentheses, WhiteSpaceExceptNewLine, RightCurly, WhiteSpaceExceptNewLine, EndOfProgram)
        tokens.find { it.type == RegExp }?.value shouldBe """/.*/"""
        tokens.windowed(2, 1, false) { (a, b) ->
            a.coordinates.idxAndLength.idx + b.coordinates.idxAndLength.length shouldBe b.coordinates.idxAndLength.idx
        }
    }

    @Test
    fun testRegexValueNotEnding() {
        val tokens = WirespecSpec.tokenize("""type Test { test: String(/.*) } """, TokenizeOptions(removeWhitespace = false))
        tokens.map { it.type } shouldBe listOf(StartOfProgram, TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, WhiteSpaceExceptNewLine, DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParentheses, RegExp, EndOfProgram)
        tokens.find { it.type == RegExp }?.value shouldBe """/.*) } """
        tokens.windowed(2, 1, false) { (a, b) ->
            a.coordinates.idxAndLength.idx + b.coordinates.idxAndLength.length shouldBe b.coordinates.idxAndLength.idx
        }
    }
}
