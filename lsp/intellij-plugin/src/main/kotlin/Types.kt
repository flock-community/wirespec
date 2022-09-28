package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.psi.tree.IElementType

interface Types {
    class ElementType(debugName: String) :
        IElementType(debugName, Language.INSTANCE)

    companion object {
        val COLON: IElementType = ElementType("COLON")
        val COMMA: IElementType = ElementType("COMMA")
        val CUSTOM_VALUE: IElementType = ElementType("CUSTOM_VALUE")
        val CUSTOM_TYPE: IElementType = ElementType("CUSTOM_TYPE")
        val BOOLEAN: IElementType = ElementType("BOOLEAN")
        val INTEGER: IElementType = ElementType("INTEGER")
        val STRING: IElementType = ElementType("STRING")
        val TYPE_DEF: IElementType = ElementType("TYPE_DEF")
        val LEFT_CURLY: IElementType = ElementType("LEFT_CURLY")
        val RIGHT_CURLY: IElementType = ElementType("RIGHT_CURLY")
        val QUESTION_MARK: IElementType = ElementType("QUESTION_MARK")
        val BRACKETS: IElementType = ElementType("BRACKETS")
        val WHITE_SPACE: IElementType = ElementType("WHITE_SPACE")
        val END_OF_PROGRAM: IElementType = ElementType("INVALID")
        val INVALID: IElementType = ElementType("INVALID")
    }
}
