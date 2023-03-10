package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.psi.tree.TokenSet as IntellijTokenSet

object TokenSets {
    var CUSTOM_TYPE = IntellijTokenSet.create(Types.CUSTOM_TYPE)
}