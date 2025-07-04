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
        tokens.shouldBeSound()
    }

    @Test
    fun testRegexValueNotEnding() {
        val tokens = WirespecSpec.tokenize("""type Test { test: String(/.*) } """, TokenizeOptions(removeWhitespace = false))
        tokens.map { it.type } shouldBe listOf(StartOfProgram, TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, WhiteSpaceExceptNewLine, DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParentheses, RegExp, EndOfProgram)
        tokens.find { it.type == RegExp }?.value shouldBe """/.*) } """
        tokens.shouldBeSound()
    }

    @Test
    fun testBoundValue() {
        val tokens = WirespecSpec.tokenize("""type Test { test: Integer(   1,    5    ) } """, TokenizeOptions(removeWhitespace = false))
        tokens.map { it.type } shouldBe listOf(StartOfProgram, TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, WhiteSpaceExceptNewLine, DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsInteger(Precision.P64), LeftParentheses, WhiteSpaceExceptNewLine, Integer, Comma, WhiteSpaceExceptNewLine, Integer, WhiteSpaceExceptNewLine, RightParentheses, WhiteSpaceExceptNewLine, RightCurly, WhiteSpaceExceptNewLine, EndOfProgram)
        tokens.shouldBeSound()
    }

    @Test
    fun testBoundValueWithUnderscore() {
        val source = """
            |type Todo {
            |    id: UUID,
            |    name: String(/.{0,50}/),
            |    done: Integer(  _,5),
            |    done: Number( 0.1,5.0)
            |}
        """.trimMargin()
        val tokens = WirespecSpec.tokenize(source, TokenizeOptions(removeWhitespace = false))
        println(tokens.map { it.type })
        tokens.map { it.type } shouldBe listOf(
            StartOfProgram,
            TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WirespecType, Comma, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParentheses, RegExp, RightParentheses, Comma, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsInteger(Precision.P64), LeftParentheses, WhiteSpaceExceptNewLine, Underscore, Comma, Integer, RightParentheses, Comma, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsNumber(Precision.P64), LeftParentheses, WhiteSpaceExceptNewLine, Number, Comma, Number, RightParentheses, NewLine, RightCurly,
            EndOfProgram,
        )
        tokens.shouldBeSound()
    }

    fun Iterable<Token>.shouldBeSound() = windowed(2, 1, false) { (a, b) ->
        a.coordinates.idxAndLength.idx + b.coordinates.idxAndLength.length shouldBe b.coordinates.idxAndLength.idx
        a.getEnd() shouldBe b.getStart()
    }

    private fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length
    private fun Token.getStart() = coordinates.getStartPos()
    private fun Token.getEnd() = coordinates.idxAndLength.idx
}
