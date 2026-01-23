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

    override fun resolve(): PsiElement? {
        val scope = GlobalSearchScope.allScope(element.project)
        val files = FileTypeIndex.getFiles(FileType, scope)
        val psiManager = PsiManager.getInstance(element.project)

        for (file in files) {
            val psiFile = psiManager.findFile(file) ?: continue
            if (psiFile is File) {
                val defs = psiFile.visitAllElements()
                    .filterIsInstance<CustomTypeElementDef>()
                for (def in defs) {
                    if (def.text == element.text) {
                        return def
                    }
                }
            }
        }
        return null
    }

    override fun handleElementRename(newElementName: String): PsiElement = element.setName(newElementName)

    private fun PsiElement.visitAllElements(): List<PsiElement> = listOf(this) + children.flatMap { it.visitAllElements() }
}
