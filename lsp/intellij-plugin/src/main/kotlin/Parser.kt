package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.psi.*
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
        var typeMarker: PsiBuilder.Marker? = null
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
                    parseNode()

                }

                Types.LEFT_CURLY -> {
                    bodyMarker = builder.mark()
                    builder.advanceLexer()
                    parseNode()
                }

                Types.RIGHT_CURLY -> {
                    builder.advanceLexer()
                    bodyMarker?.done(Body()).also { bodyMarker = null }
                    typeMarker?.done(TypeDef()).also { typeMarker = null }
                }

                else -> {
                    builder.advanceLexer()
                    parseNode()
                }
            }
        }

        fun parseDef() {
            typeMarker = builder.mark()
            builder.advanceLexer()
            if (!builder.eof() && builder.tokenType != Types.TYPE_DEF) {
                parseNode()
            }
        }

        while (!builder.eof()) {
            when (builder.tokenType) {
                Types.TYPE_DEF -> parseDef()
                else -> builder.advanceLexer()
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
            else -> error("Cannot create type")
        }
    }

    companion object {
        val FILE = IFileElementType(Language.INSTANCE)
    }

}

class TypeDefElement(ast: ASTNode) : ASTWrapperPsiElement(ast)
class BodyElement(ast: ASTNode) : ASTWrapperPsiElement(ast)

fun createDefNode(project: Project, name: String) = PsiFileFactory
    .getInstance(project)
    .createFileFromText("dummy.ws", FileType.INSTANCE, "type $name {}")
    .firstChild
    .let { PsiTreeUtil.findChildOfType(it, CustomTypeElementDef::class.java) }
    ?.node
    ?: error("Cannot create new node")

fun createRefNode(project: Project, name: String) = PsiFileFactory
    .getInstance(project)
    .createFileFromText("dummy.ws", FileType.INSTANCE, "type X { y: $name }")
    .firstChild
    .let { PsiTreeUtil.findChildOfType(it, CustomTypeElementRef::class.java) }
    ?.node
    ?: error("Cannot create new node")

abstract class CustomTypeElement(ast: ASTNode) : ASTWrapperPsiElement(ast), PsiNamedElement {

    override fun getName(): String? = this.text

    override fun getPresentation(): ItemPresentation = Utils.getPresentation(this)
}

class CustomTypeElementDef(val ast: ASTNode) : CustomTypeElement(ast), PsiNameIdentifierOwner {

    override fun setName(name: String): PsiElement {
        val newNode = createDefNode(project, name)
        this.parent.node.replaceChild(this.node, newNode)
        return this
    }

    override fun getNameIdentifier(): PsiElement = ast.firstChildNode.psi
}

class CustomTypeElementRef(val ast: ASTNode) : CustomTypeElement(ast), PsiNameIdentifierOwner {

    override fun setName(name: String): PsiElement {
        val newNode = createRefNode(project, name)
        this.parent.node.replaceChild(this.node, newNode)
        return this
    }

    override fun getNameIdentifier(): PsiElement = ast.firstChildNode.psi

    override fun getReference(): PsiReference = Reference(this)

}