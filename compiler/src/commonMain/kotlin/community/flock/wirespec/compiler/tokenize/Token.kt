package community.flock.wirespec.compiler.tokenize

data class Token(
    val type: Type,
    val value: String,
    val index: Index
) {
    sealed class Type(val string: String) {
        override fun toString() = string
    }

    data class Index(val line: Int = 1, val position: Int = 1)
}

sealed class WhiteSpace(string: String) : Token.Type(string)

object WhiteSpaceExceptNewLine : WhiteSpace("WhiteSpaceExceptNewLine")
object NewLine : WhiteSpace("NewLine")
object LeftCurly : Token.Type("LeftCurly")
object RightCurly : Token.Type("RightCurly")
object Colon : Token.Type("Colon")
object Comma : Token.Type("Comma")
object Identifier : Token.Type("Identifier")
object EndOfProgram : Token.Type("EndOfProgram")

sealed class Keyword(string: String) : Token.Type(string)
sealed class WsType(string: String) : Keyword(string)

object WsTypeDef : Keyword("WsTypeDef")
object WsString : WsType("WsString")
object WsInteger : WsType("WsInteger")
object WsBoolean : WsType("WsBoolean")
