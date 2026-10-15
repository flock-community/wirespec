package community.flock.wirespec.ide.intellij

import com.intellij.psi.tree.IElementType
import junit.framework.TestCase

class LexerTest : TestCase() {

    private val source = """
        |type TodoDto {
        |    description: String,
        |    done: Boolean?
        |}
        |endpoint GetTodos GET /todos -> {
        |    200 -> TodoDto[]
        |}
    """.trimMargin()

    fun testTokensCoverTheWholeSourceWithoutGaps() {
        val tokens = tokenize(source)

        assertEquals(source, tokens.joinToString("") { it.text })
        tokens.zipWithNext().forEach { (previous, next) -> assertEquals(previous.end, next.start) }
    }

    fun testMapsWirespecTokensToElementTypes() {
        val expected = listOf(
            Types.TYPE_DEF to "type",
            Types.TYPE_IDENTIFIER to "TodoDto",
            Types.LEFT_CURLY to "{",
            Types.WIRESPEC_IDENTIFIER to "description",
            Types.COLON to ":",
            Types.WS_STRING to "String",
            Types.COMMA to ",",
            Types.WIRESPEC_IDENTIFIER to "done",
            Types.COLON to ":",
            Types.WS_BOOLEAN to "Boolean",
            Types.QUESTION_MARK to "?",
            Types.RIGHT_CURLY to "}",
            Types.ENDPOINT_DEF to "endpoint",
            Types.TYPE_IDENTIFIER to "GetTodos",
            Types.METHOD to "GET",
            Types.PATH to "/todos",
            Types.ARROW to "->",
            Types.LEFT_CURLY to "{",
            Types.INTEGER to "200",
            Types.ARROW to "->",
            Types.TYPE_IDENTIFIER to "TodoDto",
            Types.BRACKETS to "[]",
            Types.RIGHT_CURLY to "}",
        )

        assertEquals(expected, tokenize(source).filterNot { it.type == Types.WHITE_SPACE }.map { it.type to it.text })
    }

    private data class LexedToken(val type: IElementType, val text: String, val start: Int, val end: Int)

    private fun tokenize(text: String): List<LexedToken> = Lexer().run {
        start(text, 0, text.length, 0)
        generateSequence {
            tokenType?.let { LexedToken(it, text.substring(tokenStart, tokenEnd), tokenStart, tokenEnd) }?.also { advance() }
        }.toList()
    }
}
