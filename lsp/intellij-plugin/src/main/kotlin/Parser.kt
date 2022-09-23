package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.lang.ParserDefinition as IntellijParserDefinition
import com.intellij.lexer.Lexer as IntellijLexer
import com.intellij.psi.tree.TokenSet as IntellijTokenSet


class Parser: PsiParser{
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {

        val rootMarker = builder.mark()

        while (!builder.eof()) {
            val token = builder.tokenType
            if (token != null) {
                builder.mark().done(token)
            }
            builder.advanceLexer()
        }

        rootMarker.done(root)
        return builder.treeBuilt
    }

}


class ParserDefinition : IntellijParserDefinition {
    override fun createLexer(project: Project): IntellijLexer {
        return Lexer()
    }

    override fun getCommentTokens(): IntellijTokenSet {
        return TokenSet.COMMENTS
    }

    override fun getStringLiteralElements(): IntellijTokenSet {
        return IntellijTokenSet.EMPTY
    }

    override fun createParser(project: Project): PsiParser {
        return Parser()
    }

    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return File(viewProvider)
    }

    override fun createElement(node: ASTNode): PsiElement {
        return Element(node)
    }

    companion object {
        val FILE = IFileElementType(Language.INSTANCE)
    }
}