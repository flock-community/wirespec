package community.flock.wirespec.ide.intellij

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import community.flock.wirespec.ide.intellij.parser.CustomTypeElement

public class RefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(elementToRename: PsiElement, context: PsiElement?): Boolean = elementToRename is CustomTypeElement
}
