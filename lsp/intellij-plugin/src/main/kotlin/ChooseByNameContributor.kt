package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.navigation.ChooseByNameContributor as IntellijChooseByNameContributor


class ChooseByNameContributor : IntellijChooseByNameContributor {
    override fun getNames(project: Project?, includeNonProjectItems: Boolean) =
        arrayOf<String>()

    override fun getItemsByName(
        name: String?,
        pattern: String?,
        project: Project?,
        includeNonProjectItems: Boolean
    ) =
        arrayOf<NavigationItem>()


}