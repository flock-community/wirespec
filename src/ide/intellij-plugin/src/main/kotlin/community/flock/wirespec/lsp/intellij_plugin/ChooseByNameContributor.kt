package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import community.flock.wirespec.lsp.intellij_plugin.parser.CustomTypeElementDef
import community.flock.wirespec.lsp.intellij_plugin.parser.TypeDefElement
import com.intellij.navigation.ChooseByNameContributor as IntellijChooseByNameContributor

class ChooseByNameContributor : IntellijChooseByNameContributor {

    private lateinit var map: Map<String, PsiElement>

    override fun getNames(project: Project, includeNonProjectItems: Boolean) = FileTypeIndex
        .getFiles(FileType, GlobalSearchScope.allScope(project))
        .map(PsiManager.getInstance(project)::findFile)
        .flatMap { file ->
            PsiTreeUtil.getChildrenOfType(file, TypeDefElement::class.java).orEmpty()
                .mapNotNull { PsiTreeUtil.findChildOfType(it, CustomTypeElementDef::class.java) }
                .map { it.node }
                .map { it.chars.toString() to it.psi }
        }
        .toMap()
        .also { map = it }
        .keys.toTypedArray()

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean) =
        listOfNotNull(map[name] as NavigationItem).toTypedArray()
}
