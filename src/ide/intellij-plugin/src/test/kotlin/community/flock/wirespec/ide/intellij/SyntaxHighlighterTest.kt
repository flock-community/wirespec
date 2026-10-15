package community.flock.wirespec.ide.intellij

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.BRACKETS
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.COMMA
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.IDENTIFIER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.KEYWORD
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.NUMBER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.PARAMETER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.SEMICOLON
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory as PlatformSyntaxHighlighterFactory

class SyntaxHighlighterTest : BasePlatformTestCase() {

    private val source = """
        |type TodoDto {
        |    description: String,
        |    count: Integer
        |}
        |endpoint GetTodos GET /todos -> {
        |    200 -> TodoDto[]
        |}
    """.trimMargin()

    fun testHighlightsWirespecTokens() {
        val highlights = highlightsByText()

        assertEquals(listOf(KEYWORD), highlights["type"])
        assertEquals(listOf(KEYWORD), highlights["endpoint"])
        assertEquals(listOf(IDENTIFIER), highlights["TodoDto"])
        assertEquals(listOf(PARAMETER), highlights["description"])
        assertEquals(listOf(KEYWORD), highlights["String"])
        assertEquals(listOf(KEYWORD), highlights["Integer"])
        assertEquals(listOf(BRACKETS), highlights["{"])
        assertEquals(listOf(SEMICOLON), highlights[":"])
        assertEquals(listOf(COMMA), highlights[","])
        assertEquals(listOf(NUMBER), highlights["200"])
        assertEquals(listOf(BRACKETS), highlights["[]"])
    }

    private fun highlightsByText(): Map<String, List<TextAttributesKey?>> {
        val highlighter = PlatformSyntaxHighlighterFactory.getSyntaxHighlighter(FileType, project, null)
            ?: error("No syntax highlighter registered for Wirespec")
        val lexer = highlighter.highlightingLexer.apply { start(source) }
        return generateSequence {
            lexer.tokenType
                ?.let { source.substring(lexer.tokenStart, lexer.tokenEnd) to highlighter.getTokenHighlights(it).toList() }
                ?.also { lexer.advance() }
        }.toMap()
    }
}
