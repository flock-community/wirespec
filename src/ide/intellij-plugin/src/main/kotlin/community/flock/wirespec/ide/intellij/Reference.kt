package community.flock.wirespec.ide.intellij

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import community.flock.wirespec.ide.intellij.parser.CustomTypeElement
import community.flock.wirespec.ide.intellij.parser.CustomTypeElementDef

class Reference<A : CustomTypeElement>(element: A) : PsiReferenceBase<A>(element, TextRange(0, element.textLength)) {

    override fun resolve(): PsiElement? = FileTypeIndex
        .getFiles(FileType, GlobalSearchScope.allScope(element.project))
        .map(PsiManager.getInstance(element.project)::findFile)
        .flatMap { file ->
            file?.visitAllElements().orEmpty()
                .filterIsInstance<CustomTypeElementDef>()
                .filter { it.text == element.text }
        }
        .firstOrNull()

    override fun handleElementRename(newElementName: String): PsiElement = element.setName(newElementName)

    private fun PsiElement.visitAllElements(): List<PsiElement> = listOf(this) + children.flatMap { it.visitAllElements() }
}
