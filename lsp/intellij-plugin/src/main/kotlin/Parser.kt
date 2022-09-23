package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.lang.ParserDefinition as IntellijParserDefinition
import com.intellij.psi.tree.TokenSet as IntellijTokenSet


class Parser : PsiParser {
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
    override fun createLexer(project: Project) = Lexer()

    override fun getCommentTokens() = TokenSet.COMMENTS

    override fun getStringLiteralElements() = IntellijTokenSet.EMPTY

    override fun createParser(project: Project) = Parser()

    override fun getFileNodeType() = FILE

    override fun createFile(viewProvider: FileViewProvider) = File(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = Element(node)

    companion object {
        val FILE = IFileElementType(Language.INSTANCE)
    }
}