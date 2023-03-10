package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope


class Reference<A : CustomTypeElement>(element: A) : PsiReferenceBase<A>(element, TextRange(0, element.textLength)) {

    override fun resolve(): PsiElement? {
        return FileTypeIndex
            .getFiles(FileType.INSTANCE, GlobalSearchScope.allScope(element.project))
            .map { PsiManager.getInstance(element.project).findFile(it) }
            .flatMap { file ->
                Utils.visitAllElements(file)
                    .filterIsInstance(CustomTypeElementDef::class.java)
                    .filter { it.text == element.text }
            }
            .firstOrNull()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.setName(newElementName)
    }
}