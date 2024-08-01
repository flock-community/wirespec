package community.flock.wirespec.ide.intellij.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import community.flock.wirespec.ide.intellij.File
import community.flock.wirespec.ide.intellij.Language
import community.flock.wirespec.ide.intellij.Lexer
import community.flock.wirespec.ide.intellij.parser.Parser.Body
import community.flock.wirespec.ide.intellij.parser.Parser.ChannelDef
import community.flock.wirespec.ide.intellij.parser.Parser.CustomTypeDef
import community.flock.wirespec.ide.intellij.parser.Parser.CustomTypeRef
import community.flock.wirespec.ide.intellij.parser.Parser.EndpointDef
import community.flock.wirespec.ide.intellij.parser.Parser.TypeDef

class ParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer = Lexer()

    override fun getCommentTokens(): TokenSet = TokenSet.create()

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createParser(project: Project): Parser = Parser()

    override fun getFileNodeType(): IFileElementType = IFileElementType(Language)

    override fun createFile(viewProvider: FileViewProvider): File = File(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = when (node.elementType) {
        is TypeDef -> TypeDefElement(node)
        is ChannelDef -> ChannelDefElement(node)
        is EndpointDef -> EndpointDefElement(node)
        is CustomTypeDef -> CustomTypeElementDef(node)
        is CustomTypeRef -> CustomTypeElementRef(node)
        is Body -> BodyElement(node)
        else -> error("Cannot create type")
    }
}

class TypeDefElement(ast: ASTNode) : ASTWrapperPsiElement(ast)
class ChannelDefElement(ast: ASTNode) : ASTWrapperPsiElement(ast)
class EndpointDefElement(ast: ASTNode) : ASTWrapperPsiElement(ast)
class BodyElement(ast: ASTNode) : ASTWrapperPsiElement(ast)
