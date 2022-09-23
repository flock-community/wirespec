package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.navigation.ChooseByNameContributor as IntellijChooseByNameContributor


class ChooseByNameContributor : IntellijChooseByNameContributor {
    override fun getNames(project: Project?, includeNonProjectItems: Boolean): Array<String> {
        return arrayOf()
    }

    override fun getItemsByName(
        name: String?,
        pattern: String?,
        project: Project?,
        includeNonProjectItems: Boolean
    ): Array<NavigationItem> {
        return arrayOf()
    }


}