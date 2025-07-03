package community.flock.wirespec.ide.intellij

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.BRACKETS
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.COMMA
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.IDENTIFIER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.KEYWORD
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

    override fun getTokenHighlights(tokenType: IElementType) = when (tokenType) {
        Types.COMMENT -> arrayOf(DOC_COMMENT_MARKUP)
        Types.BRACKETS -> arrayOf(BRACKETS)
        Types.COLON -> arrayOf(SEMICOLON)
        Types.COMMA -> arrayOf(COMMA)
        Types.WIRESPEC_IDENTIFIER -> arrayOf(PARAMETER)
        Types.TYPE_IDENTIFIER -> arrayOf(IDENTIFIER)
        Types.WS_BOOLEAN -> arrayOf(KEYWORD)
        Types.WS_INTEGER -> arrayOf(KEYWORD)
        Types.WS_NUMBER -> arrayOf(KEYWORD)
        Types.WS_STRING -> arrayOf(KEYWORD)
        Types.UNIT -> arrayOf(KEYWORD)
        Types.TYPE_DEF -> arrayOf(KEYWORD)
        Types.ENDPOINT_DEF -> arrayOf(KEYWORD)
        Types.CHANNEL_DEF -> arrayOf(KEYWORD)
        Types.ENUM_DEF -> arrayOf(KEYWORD)
        Types.LEFT_CURLY -> arrayOf(BRACKETS)
        Types.RIGHT_CURLY -> arrayOf(BRACKETS)
        Types.RIGHT_PARENTHESES -> arrayOf(PARENTHESES)
        Types.RIGHT_PARENTHESES -> arrayOf(PARENTHESES)
        Types.QUESTION_MARK -> arrayOf(IDENTIFIER)
        Types.REG_EXP -> arrayOf(STRING)
        else -> arrayOfNulls(0)
    }

    override fun getHighlightingLexer() = Lexer()
}

class SyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = SyntaxHighlighter()
}
