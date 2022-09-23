package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.psi.tree.TokenSet;

object TokenSet {
    val TYPE: TokenSet = TokenSet.create(Types.TYPE)
    val KEYWORD: TokenSet = TokenSet.create(Types.KEYWORD)
    val VALUE: TokenSet = TokenSet.create(Types.VALUE)
    val COMMENTS: TokenSet = TokenSet.create(Types.COMMENTS)
}