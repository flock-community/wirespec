package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import community.flock.wirespec.lsp.intellij_plugin.parser.CustomTypeElement

class RefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(elementToRename: PsiElement, context: PsiElement?) =
        elementToRename is CustomTypeElement
}
