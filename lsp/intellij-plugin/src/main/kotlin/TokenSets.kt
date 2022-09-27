package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.psi.tree.TokenSet;

object TokenSet {
    val TYPE: TokenSet = TokenSet.create()
    val KEYWORD: TokenSet = TokenSet.create()
    val VALUE: TokenSet = TokenSet.create()
    val COMMENTS: TokenSet = TokenSet.create()
}