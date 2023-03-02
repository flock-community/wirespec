package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.toArray


class Reference(element: CustomTypeElementRef) : PsiReferenceBase<CustomTypeElementRef>(element, element.textRange) {

//    val key = element.text.substring(element.textRange.startOffset, element.textRange.endOffset);

    override fun resolve(): PsiElement {
        return element
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.setName(newElementName)
    }
}