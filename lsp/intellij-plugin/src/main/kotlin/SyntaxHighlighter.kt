package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.lexer.Lexer as IntellijLexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.*
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class SyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getTokenHighlights(tokenType: IElementType) =
        when (tokenType) {
            Types.KEYWORD -> arrayOf(KEYWORD)
            Types.VALUE -> arrayOf(IDENTIFIER)
            Types.TYPE -> arrayOf(PARAMETER)
            Types.BRACKETS -> arrayOf(BRACKETS)
            Types.COLON -> arrayOf(SEMICOLON)
            Types.COMMA -> arrayOf(COMMA)
            else -> arrayOfNulls(0)
        }

    override fun getHighlightingLexer(): IntellijLexer {
        return Lexer()
    }
}

class SyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return SyntaxHighlighter()
    }
}