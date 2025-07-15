package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.WirespecType
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TokenizerRegexTest {

    @Test
    fun testRegexValue() {
        val source = """type Test { test: String(/.*/) } """
        val tokens = WirespecSpec.tokenize(source, TokenizeOptions(removeWhitespace = false))
        tokens.map { it.type } shouldBe listOf(StartOfProgram, TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, WhiteSpaceExceptNewLine, DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParenthesis, RegExp, RightParenthesis, WhiteSpaceExceptNewLine, RightCurly, WhiteSpaceExceptNewLine, EndOfProgram)
        tokens.find { it.type == RegExp }?.value shouldBe """/.*/"""
        tokens.shouldBeSound(source)
    }

    @Test
    fun testRegexValueNotEnding() {
        val source = """type Test { test: String(/.*) } """
        val tokens = WirespecSpec.tokenize(source, TokenizeOptions(removeWhitespace = false))
        tokens.map { it.type } shouldBe listOf(StartOfProgram, TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, WhiteSpaceExceptNewLine, DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParenthesis, RegExp, EndOfProgram)
        tokens.find { it.type == RegExp }?.value shouldBe """/.*) } """
        tokens.shouldBeSound(source)
    }

    @Test
    fun testRegexComplex() {
        val source = """type Test { test: String(/^data:(text\/csv|application\/pdf|application\/vnd\.ms-excel|application\/vnd\.openxmlformats-officedocument\.spreadsheetml\.sheet);base64,[A-Za-z0-9+\/]+=*$/g) } """
        val tokens = WirespecSpec.tokenize(source, TokenizeOptions(removeWhitespace = false))
        tokens.map { it.type } shouldBe listOf(StartOfProgram, TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, WhiteSpaceExceptNewLine, DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParenthesis, RegExp, RightParenthesis, WhiteSpaceExceptNewLine, RightCurly, WhiteSpaceExceptNewLine, EndOfProgram)
        tokens.find { it.type == RegExp }?.value shouldBe """/^data:(text\/csv|application\/pdf|application\/vnd\.ms-excel|application\/vnd\.openxmlformats-officedocument\.spreadsheetml\.sheet);base64,[A-Za-z0-9+\/]+=*$/g"""
        tokens.shouldBeSound(source)
    }

    @Test
    fun testRegexComplexWithComments() {
        val source = """
            |/*
            |* File data structure containing filename and base64-encoded data URL for supported file types (CSV, PDF, XLS, XLSX)
            |*/
            |type UploadedFile {
            |    fileName: String,
            |    dataUrl: String(/^text\/test$/g)
            |}
        """.trimMargin()
        val tokens = WirespecSpec.tokenize(source, TokenizeOptions(removeWhitespace = false))
        tokens.map { it.type } shouldBe listOf(
            StartOfProgram,
            Comment, NewLine,
            TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, Comma, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParenthesis, RegExp, RightParenthesis, NewLine,
            RightCurly,
            EndOfProgram,
        )

        tokens.shouldBeSound(source)
    }

    @Test
    fun testBoundValue() {
        val source = """type Test { test: Integer(   1,    5    ) } """
        val tokens = WirespecSpec.tokenize(source, TokenizeOptions(removeWhitespace = false))
        tokens.map { it.type } shouldBe listOf(StartOfProgram, TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, WhiteSpaceExceptNewLine, DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsInteger(Precision.P64), LeftParenthesis, WhiteSpaceExceptNewLine, Integer, Comma, WhiteSpaceExceptNewLine, Integer, WhiteSpaceExceptNewLine, RightParenthesis, WhiteSpaceExceptNewLine, RightCurly, WhiteSpaceExceptNewLine, EndOfProgram)
        tokens.shouldBeSound(source)
    }

    @Test
    fun testBoundValueWithUnderscore() {
        val source = """
            |type Todo {
            |    id: UUID,
            |    name: String(/.{0,50}/),
            |    test: String(/^test\/test$/g),
            |    prio: Integer(  _,5),
            |    done: Number( 0.1,5.0)
            |}
        """.trimMargin()
        val tokens = WirespecSpec.tokenize(source, TokenizeOptions(removeWhitespace = false))
        println(tokens.map { it.type })
        tokens.map { it.type } shouldBe listOf(
            StartOfProgram,
            TypeDefinition, WhiteSpaceExceptNewLine, WirespecType, WhiteSpaceExceptNewLine, LeftCurly, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WirespecType, Comma, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParenthesis, RegExp, RightParenthesis, Comma, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsString, LeftParenthesis, RegExp, RightParenthesis, Comma, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsInteger(Precision.P64), LeftParenthesis, WhiteSpaceExceptNewLine, Underscore, Comma, Integer, RightParenthesis, Comma, NewLine, WhiteSpaceExceptNewLine,
            DromedaryCaseIdentifier, Colon, WhiteSpaceExceptNewLine, WsNumber(Precision.P64), LeftParenthesis, WhiteSpaceExceptNewLine, Number, Comma, Number, RightParenthesis, NewLine,
            RightCurly,
            EndOfProgram,
        )
        tokens.shouldBeSound(source)
    }

    fun Iterable<Token>.shouldBeSound(source: String) {
        with(filter { it.type != EndOfProgram }) {
            windowed(2, 1, false) { (a, b) ->
                a.coordinates.idxAndLength.idx + b.coordinates.idxAndLength.length shouldBe b.coordinates.idxAndLength.idx
                a.getEnd() shouldBe b.getStart()
                a.value.length shouldBe a.coordinates.idxAndLength.length
                b.value.length shouldBe b.coordinates.idxAndLength.length
            }
            source.length shouldBe sumOf { it.coordinates.idxAndLength.length }
        }
    }

    private fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length
    private fun Token.getStart() = coordinates.getStartPos()
    private fun Token.getEnd() = coordinates.idxAndLength.idx
}
