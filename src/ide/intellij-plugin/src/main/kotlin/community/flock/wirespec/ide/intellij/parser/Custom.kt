package community.flock.wirespec.ide.intellij.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import community.flock.wirespec.ide.intellij.FileType
import community.flock.wirespec.ide.intellij.Icons
import community.flock.wirespec.ide.intellij.Reference

abstract class CustomTypeElement(ast: ASTNode) :
    ASTWrapperPsiElement(ast),
    PsiNamedElement {

    override fun getName(): String? = text

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {

        override fun getLocationString() = ""

        override fun getPresentableText() = text

        override fun getIcon(unused: Boolean) = Icons.FILE
    }
}

class CustomTypeElementDef(private val ast: ASTNode) :
    CustomTypeElement(ast),
    PsiNameIdentifierOwner {

    override fun setName(name: String): PsiElement = also {
        parent.node.replaceChild(node, project.createDefNode(name))
    }

    override fun getNameIdentifier(): PsiElement = ast.firstChildNode.psi
}

class CustomTypeElementRef(private val ast: ASTNode) :
    CustomTypeElement(ast),
    PsiNameIdentifierOwner {

    override fun setName(name: String): PsiElement = also {
        parent.node.replaceChild(node, project.createRefNode(name))
    }

    override fun getNameIdentifier(): PsiElement = ast.firstChildNode.psi

    override fun getReference(): PsiReference = Reference(this)
}

fun Project.createDefNode(name: String) = PsiFileFactory
    .getInstance(this)
    .createFileFromText("dummy.ws", FileType, "type $name {}")
    .run { PsiTreeUtil.findChildOfType(firstChild, CustomTypeElementDef::class.java) }
    ?.node
    ?: error("Cannot create new node")

fun Project.createRefNode(name: String) = PsiFileFactory
    .getInstance(this)
    .createFileFromText("dummy.ws", FileType, "type X { y: $name }")
    .run { PsiTreeUtil.findChildOfType(firstChild, CustomTypeElementRef::class.java) }
    ?.node
    ?: error("Cannot create new node")
