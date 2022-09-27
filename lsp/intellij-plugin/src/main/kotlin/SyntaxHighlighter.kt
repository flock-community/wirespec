package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.*
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import community.flock.wirespec.compiler.core.tokenize.types.*

class SyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getTokenHighlights(tokenType: IElementType) =
        if(tokenType is Lexer.ElementType){
            when(tokenType.token.type) {
                is Keyword -> arrayOf(KEYWORD)
                Arrow -> arrayOf(KEYWORD)
                Brackets -> arrayOf(BRACKETS)
                Colon -> arrayOf(SEMICOLON)
                Comma -> arrayOf(COMMA)
                CustomValue -> arrayOf(PARAMETER)
                Invalid -> arrayOf(PARAMETER)
                LeftCurly -> arrayOf(BRACKETS)
                RightCurly -> arrayOf(BRACKETS)
                else -> arrayOfNulls(0)
            }
        } else {
            arrayOfNulls(0)
        }

    override fun getHighlightingLexer() = Lexer()
}

class SyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = SyntaxHighlighter()
}