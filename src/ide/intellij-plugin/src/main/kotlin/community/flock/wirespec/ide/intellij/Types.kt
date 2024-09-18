package community.flock.wirespec.ide.intellij

import com.intellij.psi.tree.IElementType

interface Types {
    class ElementType(debugName: String) : IElementType(debugName, Language)

    companion object {
        val LEFT_CURLY = ElementType("LEFT_CURLY")
        val RIGHT_CURLY = ElementType("RIGHT_CURLY")
        val COLON = ElementType("COLON")
        val COMMA = ElementType("COMMA")
        val QUESTION_MARK = ElementType("QUESTION_MARK")
        val HASH = ElementType("HASH")
        val FORWARD_SLASH = ElementType("FORWARD_SLASH")
        val BRACKETS = ElementType("BRACKETS")
        val CUSTOM_VALUE = ElementType("CUSTOM_VALUE")
        val COMMENT = ElementType("COMMENT")
        val Character = ElementType("INVALID")
        val END_OF_PROGRAM = ElementType("END_OF_PROGRAM")
        val WHITE_SPACE = ElementType("WHITE_SPACE")
        val TYPE_DEF = ElementType("TYPE_DEF")
        val ENUM_DEF = ElementType("ENUM_DEF")
        val ENDPOINT_DEF = ElementType("ENDPOINT_DEF")
        val CHANNEL_DEF = ElementType("CHANNEL_DEF")
        val STRING = ElementType("STRING")
        val INTEGER = ElementType("INTEGER")
        val NUMBER = ElementType("NUMBER")
        val BOOLEAN = ElementType("BOOLEAN")
        val CUSTOM_TYPE = ElementType("CUSTOM_TYPE")
        val UNIT = ElementType("UNIT")
        val METHOD = ElementType("METHOD")
        val PATH = ElementType("PATH")
        val STATUS_CODE = ElementType("STATUS_CODE")
        val ARROW = ElementType("ARROW")
        val EQUALS = ElementType("EQUALS")
        val PIPE = ElementType("PIPE")
        val CUSTOM_REGEX = ElementType("CUSTOM_REGEX")
    }
}
