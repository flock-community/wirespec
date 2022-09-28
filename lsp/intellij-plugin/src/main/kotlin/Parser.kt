package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.lang.ParserDefinition as IntellijParserDefinition
import com.intellij.psi.tree.TokenSet as IntellijTokenSet


class Parser : PsiParser {

    class TypeDef : IElementType("TYPE_DEF", Language.INSTANCE)
    class CustomType : IElementType("CUSTOM_TYPE", Language.INSTANCE)


    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()

        fun parseOther() {

            when (builder.tokenType) {
                Types.CUSTOM_TYPE -> {
                    val typeMarker = builder.mark()
                    builder.advanceLexer()
                    typeMarker.done(CustomType())
                }

                else -> builder.advanceLexer()
            }

            if (!builder.eof() && builder.tokenType != Types.TYPE_DEF) {
                parseOther()
            }

        }

        fun parseDef() {
            val typeMarker = builder.mark()
            builder.advanceLexer()
            if (!builder.eof() && builder.tokenType != Types.TYPE_DEF) {
                parseOther()
            }
            typeMarker.done(TypeDef())
        }

        while (!builder.eof()) {
            if (builder.tokenType == Types.TYPE_DEF) {
                parseDef()
            }
        }

        rootMarker.done(root)

        return builder.treeBuilt
    }
}


class ParserDefinition : IntellijParserDefinition {
    override fun createLexer(project: Project) = Lexer()

    override fun getCommentTokens() = TokenSet.create()

    override fun getStringLiteralElements() = IntellijTokenSet.EMPTY

    override fun createParser(project: Project) = Parser()

    override fun getFileNodeType() = FILE

    override fun createFile(viewProvider: FileViewProvider) = File(viewProvider)

    override fun createElement(node: ASTNode): PsiElement {
        return when (node.elementType) {
            is Parser.TypeDef -> TypeDefElement(node)
            is Parser.CustomType -> CustomTypeElement(node)
            else -> TODO("")
        }
    }

    companion object {
        val FILE = IFileElementType(Language.INSTANCE)
    }

}

class TypeDefElement(ast: ASTNode) : ASTWrapperPsiElement(ast), PsiNamedElement {
    override fun setName(name: String): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getPresentation(): ItemPresentation {
        return Util.getPresentation(this)
    }
}

class CustomTypeElement(ast: ASTNode) : ASTWrapperPsiElement(ast), PsiNameIdentifierOwner {
    override fun setName(name: String): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getNameIdentifier(): PsiElement? {
        return null
    }

    override fun getPresentation(): ItemPresentation {
        return Util.getPresentation(this)
    }
}