package community.flock.wirespec.ide.intellij

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import community.flock.wirespec.ide.intellij.parser.CustomTypeElementDef
import community.flock.wirespec.ide.intellij.parser.TypeDefElement
import com.intellij.navigation.ChooseByNameContributor as IntellijChooseByNameContributor

class ChooseByNameContributor : IntellijChooseByNameContributor {

    fun getMap(project: Project): Map<String, PsiElement> {
        val scope = GlobalSearchScope.allScope(project)
        val files = FileTypeIndex.getFiles(FileType, scope)
        val psiManager = PsiManager.getInstance(project)
        val map = mutableMapOf<String, PsiElement>()

        for (file in files) {
            val psiFile = psiManager.findFile(file) ?: continue
            if (psiFile is community.flock.wirespec.ide.intellij.File) {
                val typeDefs = PsiTreeUtil.getChildrenOfType(psiFile, TypeDefElement::class.java).orEmpty()
                for (typeDef in typeDefs) {
                    val customType = PsiTreeUtil.findChildOfType(typeDef, CustomTypeElementDef::class.java)
                    if (customType != null) {
                        map[customType.text] = customType
                    }
                }
            }
        }
        return map
    }

    override fun getNames(project: Project, includeNonProjectItems: Boolean) = getMap(project).keys.toTypedArray()

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<NavigationItem> = listOfNotNull((getMap(project)[name]) as NavigationItem).toTypedArray()
}
