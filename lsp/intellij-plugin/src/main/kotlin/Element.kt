package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.PsiElementBase
import com.intellij.lang.Language as IntellijLanguage

class Element(val ast: ASTNode): ASTWrapperPsiElement(ast) {
    override fun getReference(): PsiReference? {
        return super.getReference()
    }

    override fun getReferences(): Array<PsiReference> {
        return super.getReferences()
    }
}