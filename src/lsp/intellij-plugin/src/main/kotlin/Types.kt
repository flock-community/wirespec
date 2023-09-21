package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.psi.tree.IElementType

interface Types {
    class ElementType(debugName: String) : IElementType(debugName, Language.INSTANCE)

    companion object {
        val COLON = ElementType("COLON")
        val COMMA = ElementType("COMMA")
        val CUSTOM_VALUE = ElementType("CUSTOM_VALUE")
        val CUSTOM_TYPE = ElementType("CUSTOM_TYPE")
        val BOOLEAN = ElementType("BOOLEAN")
        val INTEGER = ElementType("INTEGER")
        val STRING = ElementType("STRING")
        val TYPE_DEF = ElementType("TYPE_DEF")
        val ENUM_TYPE_DEF = ElementType("REFINED_TYPE_DEF")
        val REFINED_TYPE_DEF = ElementType("REFINED_TYPE_DEF")
        val ENDPOINT_TYPE_DEF = ElementType("ENDPOINT_TYPE_DEF")
        val CUSTOM_REGEX = ElementType("CUSTOM_REGEX")
        val LEFT_CURLY = ElementType("LEFT_CURLY")
        val RIGHT_CURLY = ElementType("RIGHT_CURLY")
        val QUESTION_MARK = ElementType("QUESTION_MARK")
        val BRACKETS = ElementType("BRACKETS")
        val WHITE_SPACE = ElementType("WHITE_SPACE")
        val END_OF_PROGRAM = ElementType("END_OF_PROGRAM")
        val INVALID = ElementType("INVALID")
    }
}
