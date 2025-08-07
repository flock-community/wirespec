package community.flock.wirespec.ide.intellij

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import community.flock.wirespec.ide.intellij.parser.CustomTypeElementDef
import community.flock.wirespec.ide.intellij.parser.TypeDefElement
import com.intellij.navigation.ChooseByNameContributor as IntellijChooseByNameContributor

class ChooseByNameContributor : IntellijChooseByNameContributor {

    fun getMap(project: Project) = FileTypeIndex
        .getFiles(FileType, GlobalSearchScope.allScope(project))
        .map(PsiManager.getInstance(project)::findFile)
        .flatMap { file ->
            PsiTreeUtil.getChildrenOfType(file, TypeDefElement::class.java).orEmpty()
                .mapNotNull { PsiTreeUtil.findChildOfType(it, CustomTypeElementDef::class.java) }
                .map { it.node }
                .map { it.chars.toString() to it.psi }
        }
        .toMap()

    override fun getNames(project: Project, includeNonProjectItems: Boolean) = getMap(project).keys.toTypedArray()

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<NavigationItem> = listOfNotNull((getMap(project)[name]) as NavigationItem).toTypedArray()
}
