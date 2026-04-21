package community.flock.wirespec.ide.intellij

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.BRACKETS
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.COMMA
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.IDENTIFIER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.KEYWORD
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.LINE_COMMENT
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.METADATA
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.NUMBER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.PARAMETER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.PARENTHESES
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.SEMICOLON
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.STRING
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class SyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getTokenHighlights(tokenType: IElementType) =
        TOKEN_HIGHLIGHTS[tokenType]?.let(::arrayOf) ?: arrayOfNulls(0)

    override fun getHighlightingLexer() = Lexer()

    companion object {
        private val TOKEN_HIGHLIGHTS = mapOf(
            Types.COMMENT to DOC_COMMENT_MARKUP,
            Types.BRACKETS to BRACKETS,
            Types.LEFT_BRACKET to BRACKETS,
            Types.RIGHT_BRACKET to BRACKETS,
            Types.COLON to SEMICOLON,
            Types.COMMA to COMMA,
            Types.WIRESPEC_IDENTIFIER to PARAMETER,
            Types.TYPE_IDENTIFIER to IDENTIFIER,
            Types.WS_BOOLEAN to KEYWORD,
            Types.WS_INTEGER to KEYWORD,
            Types.WS_NUMBER to KEYWORD,
            Types.WS_STRING to KEYWORD,
            Types.UNIT to KEYWORD,
            Types.TYPE_DEF to KEYWORD,
            Types.ENDPOINT_DEF to KEYWORD,
            Types.CHANNEL_DEF to KEYWORD,
            Types.ENUM_DEF to KEYWORD,
            Types.LEFT_CURLY to BRACKETS,
            Types.RIGHT_CURLY to BRACKETS,
            Types.LEFT_PARENTHESES to PARENTHESES,
            Types.RIGHT_PARENTHESES to PARENTHESES,
            Types.QUESTION_MARK to IDENTIFIER,
            Types.REG_EXP to STRING,
            Types.UNDERSCORE to LINE_COMMENT,
            Types.ANNOTATION to METADATA,
            Types.LITERAL_STRING to STRING,
            Types.NUMBER to NUMBER,
            Types.INTEGER to NUMBER,
        )
    }
}

class SyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = SyntaxHighlighter()
}
