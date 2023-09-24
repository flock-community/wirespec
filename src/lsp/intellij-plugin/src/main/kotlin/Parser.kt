package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.CUSTOM_TYPE
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.ENDPOINT_DEF
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.ENUM_DEF
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.LEFT_CURLY
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.REFINED_TYPE_DEF
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.RIGHT_CURLY
import community.flock.wirespec.lsp.intellij_plugin.Types.Companion.TYPE_DEF
import com.intellij.lang.ParserDefinition as IntellijParserDefinition
import com.intellij.psi.tree.TokenSet as IntellijTokenSet

class Parser : PsiParser {

    class TypeDef : IElementType("TYPE_DEF", Language.INSTANCE)
    class EndpointDef : IElementType("ENDPOINT_DEF", Language.INSTANCE)
    class CustomTypeDef : IElementType("CUSTOM_TYPE_DEF", Language.INSTANCE)
    class CustomTypeRef : IElementType("CUSTOM_TYPE_REF", Language.INSTANCE)
    class Body : IElementType("BODY", Language.INSTANCE)


    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()

        fun parseBody() {
            when {
                builder.eof() -> return
                builder.def() -> return
                builder.tokenType == RIGHT_CURLY -> return
                builder.tokenType == CUSTOM_TYPE -> {
                    val customTypeMarker = builder.mark()
                    builder.advanceLexer()
                    customTypeMarker.done(CustomTypeRef())
                    parseBody()
                }

                else -> {
                    builder.advanceLexer()
                    parseBody()
                }
            }
        }

        fun parseDef() {
            when {
                builder.eof() -> return
                builder.def() -> return
                builder.tokenType == CUSTOM_TYPE -> {
                    val customTypeMarker = builder.mark()
                    builder.advanceLexer()
                    customTypeMarker.done(CustomTypeDef())
                    parseDef()
                }

                builder.tokenType == LEFT_CURLY -> {
                    val bodyMarker = builder.mark()
                    builder.advanceLexer()
                    parseBody()
                    bodyMarker.done(Body())
                    builder.advanceLexer()
                    parseDef()
                }

                else -> {
                    builder.advanceLexer()
                    parseDef()
                }
            }
        }

        fun parse() {
            when {
                builder.eof() -> return
                builder.def() -> {
                    val marker = builder.mark()
                    builder.advanceLexer()
                    parseDef()
                    marker.done(TypeDef())
                    parse()
                }

                else -> {
                    builder.advanceLexer()
                    parse()
                }
            }
        }

        parse()

        rootMarker.done(root)

        return builder.treeBuilt
    }

    fun PsiBuilder.def() = when (this.tokenType) {
        TYPE_DEF, ENDPOINT_DEF, REFINED_TYPE_DEF, ENUM_DEF -> true
        else -> false
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
            is Parser.EndpointDef -> EndpointDefElement(node)
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
class EndpointDefElement(ast: ASTNode) : ASTWrapperPsiElement(ast)
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
