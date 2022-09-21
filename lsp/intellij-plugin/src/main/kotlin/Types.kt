// This is a generated file. Not intended for manual editing.
package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.psi.tree.IElementType


interface Types {
    class ElementType(debugName: String) :
        IElementType(debugName, Language.INSTANCE)

    companion object {
        @JvmField
        val BRACKETS: IElementType = ElementType("BRACKETS")

        @JvmField
        val TYPE: IElementType = ElementType("TYPE")

        @JvmField
        val KEYWORD: IElementType = ElementType("KEYWORD")

        @JvmField
        val VALUE: IElementType = ElementType("VALUE")

        @JvmField
        val COLON: IElementType = ElementType("COLON")

        @JvmField
        val COMMA: IElementType = ElementType("COMMA")
    }
}