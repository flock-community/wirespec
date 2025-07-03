package community.flock.wirespec.ide.intellij

import com.intellij.psi.tree.IElementType

interface Types {
    class ElementType(debugName: String) : IElementType(debugName, Language)

    companion object {
        val LEFT_CURLY = ElementType("LEFT_CURLY")
        val RIGHT_CURLY = ElementType("RIGHT_CURLY")
        val LEFT_PARENTHESES = ElementType("LEFT_PARENTHESES")
        val RIGHT_PARENTHESES = ElementType("RIGHT_PARENTHESES")
        val COLON = ElementType("COLON")
        val COMMA = ElementType("COMMA")
        val QUESTION_MARK = ElementType("QUESTION_MARK")
        val HASH = ElementType("HASH")
        val FORWARD_SLASH = ElementType("FORWARD_SLASH")
        val BRACKETS = ElementType("BRACKETS")
        val WIRESPEC_IDENTIFIER = ElementType("CUSTOM_VALUE")
        val COMMENT = ElementType("COMMENT")
        val CHARACTER = ElementType("CHARACTER")
        val END_OF_PROGRAM = ElementType("END_OF_PROGRAM")
        val WHITE_SPACE = ElementType("WHITE_SPACE")
        val TYPE_DEF = ElementType("TYPE_DEF")
        val ENUM_DEF = ElementType("ENUM_DEF")
        val ENDPOINT_DEF = ElementType("ENDPOINT_DEF")
        val CHANNEL_DEF = ElementType("CHANNEL_DEF")
        val WS_STRING = ElementType("STRING")
        val WS_INTEGER = ElementType("INTEGER")
        val WS_NUMBER = ElementType("NUMBER")
        val WS_BOOLEAN = ElementType("BOOLEAN")
        val WS_BYTES = ElementType("BYTES")
        val TYPE_IDENTIFIER = ElementType("CUSTOM_TYPE")
        val UNIT = ElementType("UNIT")
        val METHOD = ElementType("METHOD")
        val PATH = ElementType("PATH")
        val STATUS_CODE = ElementType("STATUS_CODE")
        val ARROW = ElementType("ARROW")
        val EQUALS = ElementType("EQUALS")
        val PIPE = ElementType("PIPE")

        val REG_EXP = ElementType("REG_EXP")
        val NUMBER = ElementType("NUMBER")
        val INTEGER = ElementType("INTEGER")
    }
}
