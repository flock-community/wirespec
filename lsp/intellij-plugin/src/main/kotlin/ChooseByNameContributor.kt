package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.navigation.ChooseByNameContributor as IntellijChooseByNameContributor


class ChooseByNameContributor : IntellijChooseByNameContributor {

    var map: Map<String, PsiElement>? = null
    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        val virtualFiles: Collection<VirtualFile> =
            FileTypeIndex.getFiles(FileType.INSTANCE, GlobalSearchScope.allScope(project))
        map = virtualFiles
            .flatMap { virtualFile ->
                val file = PsiManager.getInstance(project).findFile(virtualFile)
                PsiTreeUtil
                    .getChildrenOfType(file, TypeDefElement::class.java)
                    ?.map { type -> PsiTreeUtil.findChildOfType(type, CustomTypeElement::class.java) }
                    ?.map { it?.node }
                    ?.map { node -> node?.let { it.chars.toString() to it.psi } }
                    ?: listOf()
            }
            .filterNotNull()
            .toMap()



        return map?.keys?.toTypedArray() ?: arrayOf()

    }


    override fun getItemsByName(
        name: String,
        pattern: String,
        project: Project,
        includeNonProjectItems: Boolean
    ): Array<NavigationItem> {
        val res = listOfNotNull(map?.get(name) as NavigationItem).toTypedArray()
        return res
    }
}