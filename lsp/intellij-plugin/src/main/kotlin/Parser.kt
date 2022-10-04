package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.lang.ParserDefinition as IntellijParserDefinition
import com.intellij.psi.tree.TokenSet as IntellijTokenSet


class Parser : PsiParser {

    class TypeDef : IElementType("TYPE_DEF", Language.INSTANCE)
    class CustomType : IElementType("CUSTOM_TYPE", Language.INSTANCE)
    class Body : IElementType("BODY", Language.INSTANCE)


    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        var bodyMarker: PsiBuilder.Marker? = null

        fun parseNode() {
            when (builder.tokenType) {
                Types.CUSTOM_TYPE -> {
                    val marker = builder.mark()
                    builder.advanceLexer()
                    marker.done(CustomType())
                }

                Types.LEFT_CURLY -> {
                    bodyMarker = builder.mark()
                    builder.advanceLexer()
                }

                Types.RIGHT_CURLY -> {
                    builder.advanceLexer()
                    bodyMarker?.done(Body()).also { bodyMarker = null }
                }

                else -> builder.advanceLexer()
            }
            if (!builder.eof() && builder.tokenType != Types.TYPE_DEF) {
                parseNode()
            }
        }

        fun parseDef() {
            val marker = builder.mark()
            builder.advanceLexer()
            if (!builder.eof() && builder.tokenType != Types.TYPE_DEF) {
                parseNode()
            }
            bodyMarker?.done(Body()).also { bodyMarker = null }
            marker.done(TypeDef())
        }

        while (!builder.eof()) {
            if (builder.tokenType == Types.TYPE_DEF) {
                parseDef()
            } else {
                builder.advanceLexer()
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
            is Parser.Body -> BodyElement(node)
            else -> TODO("")
        }
    }

    companion object {
        val FILE = IFileElementType(Language.INSTANCE)
    }

}

class TypeDefElement(ast: ASTNode) : ASTWrapperPsiElement(ast)
class BodyElement(ast: ASTNode) : ASTWrapperPsiElement(ast)

class CustomTypeElement(val ast: ASTNode) : ASTWrapperPsiElement(ast), PsiNameIdentifierOwner {
    override fun setName(name: String): PsiElement {
        val newNode = PsiFileFactory
            .getInstance(project)
            .createFileFromText("dummy.ws", FileType.INSTANCE, "type $name {}")
            .firstChild
            .let { PsiTreeUtil.findChildOfType(it, CustomTypeElement::class.java)  }
            ?.node
        val customTypeNode: ASTNode? = node.findChildByType(Types.CUSTOM_TYPE)
        if (newNode != null && customTypeNode != null) {
            node.replaceChild(customTypeNode, newNode)
        }
        return this
    }

    override fun getName(): String? {
        return this.text
    }

    override fun getNameIdentifier(): PsiElement? {
        val virtualFiles: Collection<VirtualFile> =
            FileTypeIndex.getFiles(FileType.INSTANCE, GlobalSearchScope.allScope(project))
        val res = virtualFiles.flatMap {
            val file = PsiManager.getInstance(project).findFile(it)
            PsiTreeUtil
                .getChildrenOfType(file, TypeDefElement::class.java)
                ?.map { type -> PsiTreeUtil.findChildOfType(type, CustomTypeElement::class.java) }
                ?.filter { node.chars.toString() == it?.node?.chars.toString() }
                ?: listOf()
        }
        return res.firstOrNull()
    }

    override fun getPresentation(): ItemPresentation {
        return Util.getPresentation(this)
    }
}