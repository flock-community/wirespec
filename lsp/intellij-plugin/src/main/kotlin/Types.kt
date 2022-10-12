// This is a generated file. Not intended for manual editing.
package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.psi.tree.IElementType


interface Types {
    class ElementType(debugName: String) :
        IElementType(debugName, Language.INSTANCE)

    companion object {
        val BRACKETS: IElementType = ElementType("BRACKETS")
        val TYPE: IElementType = ElementType("TYPE")
        val KEYWORD: IElementType = ElementType("KEYWORD")
        val VALUE: IElementType = ElementType("VALUE")
        val COLON: IElementType = ElementType("COLON")
        val COMMA: IElementType = ElementType("COMMA")
        val COMMENTS: IElementType = ElementType("COMMENTS")
    }
}