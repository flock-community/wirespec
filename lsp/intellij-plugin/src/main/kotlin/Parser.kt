package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
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
    class CustomTypeDef : IElementType("CUSTOM_TYPE_DEF", Language.INSTANCE)
    class CustomTypeRef : IElementType("CUSTOM_TYPE_REF", Language.INSTANCE)
    class Body : IElementType("BODY", Language.INSTANCE)


    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        var bodyMarker: PsiBuilder.Marker? = null

        fun parseNode() {
            when (builder.tokenType) {
                Types.CUSTOM_TYPE -> {
                    val marker = builder.mark()
                    builder.advanceLexer()
                    if (bodyMarker == null) {
                        marker.done(CustomTypeDef())
                    } else {
                        marker.done(CustomTypeRef())
                    }

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
            is Parser.CustomTypeDef -> CustomTypeElementDef(node)
            is Parser.CustomTypeRef -> CustomTypeElementRef(node)
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

class CustomTypeElementDef(ast: ASTNode) : ASTWrapperPsiElement(ast), PsiNamedElement {
    override fun setName(name: String): PsiElement {
        fun createNewNode() = PsiFileFactory
            .getInstance(project)
            .createFileFromText("dummy.ws", FileType.INSTANCE, "type $name {}")
            .firstChild
            .let { PsiTreeUtil.findChildOfType(it, CustomTypeElementDef::class.java) }
            ?.node
            ?: error("Cannot create new node")

        FileTypeIndex
            .getFiles(FileType.INSTANCE, GlobalSearchScope.everythingScope(project))
            .forEach {
                val file = PsiManager.getInstance(project).findFile(it)
                Utils.visitAllElements(file)
                    .filterIsInstance(CustomTypeElementDef::class.java)
                    .filter { element -> element.node.chars == node.chars }
                    .forEach { element -> element.parent.node.replaceChild(element.node, createNewNode()) }
            }
        return this
    }

    override fun getName(): String? {
        return this.text
    }



    override fun getPresentation(): ItemPresentation {
        return Utils.getPresentation(this)
    }

    override fun getReferences(): Array<PsiReference> {

        return FileTypeIndex
            .getFiles(FileType.INSTANCE, GlobalSearchScope.everythingScope(project))
            .flatMap {
                val file = PsiManager.getInstance(project).findFile(it)
                Utils.visitAllElements(file)
                    .filterIsInstance(CustomTypeElementRef::class.java)
                    .filter { element -> element.node.chars == node.chars }
                    .map { element ->
                        object: PsiReferenceBase<CustomTypeElementDef>(this, this.textRange) {
                            override fun resolve(): PsiElement {
                                return element
                            }

                            override fun handleElementRename(newElementName: String): PsiElement {
                                return element.setName(newElementName)
                            }
                        }
                    }
            }
            .toTypedArray()

    }
}

class CustomTypeElementRef(ast: ASTNode) : ASTWrapperPsiElement(ast), PsiNameIdentifierOwner{

    override fun getName(): String? {
        return this.text
    }

    override fun setName(name: String): PsiElement {
        println("Set name $name")
        return this
    }

    override fun getNameIdentifier(): PsiElement? {
        val virtualFiles: Collection<VirtualFile> =
            FileTypeIndex.getFiles(FileType.INSTANCE, GlobalSearchScope.allScope(project))
        val res = virtualFiles.flatMap {
            val file = PsiManager.getInstance(project).findFile(it)
            PsiTreeUtil
                .getChildrenOfType(file, TypeDefElement::class.java)
                ?.map { type -> PsiTreeUtil.findChildOfType(type, CustomTypeElementDef::class.java) }
                ?.filter { node.chars.toString() == it?.node?.chars.toString() }
                ?: listOf()
        }
        return res.firstOrNull()
    }


}