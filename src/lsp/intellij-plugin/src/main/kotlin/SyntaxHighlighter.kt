package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.BRACKETS
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.COMMA
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.IDENTIFIER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.KEYWORD
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.PARAMETER
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.SEMICOLON
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class SyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getTokenHighlights(tokenType: IElementType) = when (tokenType) {
        Types.BRACKETS -> arrayOf(BRACKETS)
        Types.COLON -> arrayOf(SEMICOLON)
        Types.COMMA -> arrayOf(COMMA)
        Types.CUSTOM_VALUE -> arrayOf(PARAMETER)
        Types.CUSTOM_TYPE -> arrayOf(IDENTIFIER)
        Types.BOOLEAN -> arrayOf(KEYWORD)
        Types.INTEGER -> arrayOf(KEYWORD)
        Types.NUMBER -> arrayOf(KEYWORD)
        Types.STRING -> arrayOf(KEYWORD)
        Types.UNIT -> arrayOf(KEYWORD)
        Types.TYPE_DEF -> arrayOf(KEYWORD)
        Types.ENDPOINT_DEF -> arrayOf(KEYWORD)
        Types.ENUM_DEF -> arrayOf(KEYWORD)
        Types.LEFT_CURLY -> arrayOf(BRACKETS)
        Types.RIGHT_CURLY -> arrayOf(BRACKETS)
        Types.QUESTION_MARK -> arrayOf(IDENTIFIER)
        else -> arrayOfNulls(0)
    }

    override fun getHighlightingLexer() = Lexer()
}

class SyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = SyntaxHighlighter()
}
