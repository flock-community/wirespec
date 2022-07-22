package community.flock.wirespec.compiler.core.tokenize

data class Token(
    val type: Type,
    val value: String,
    val index: Index
) {
    sealed class Type(private val name: String) {
        override fun toString() = name
    }

    data class Index(val line: Int = 1, val position: Int = 1, val idxAndLength: Pair<Int, Int> = 0 to 0) {
        companion object {
            operator fun Pair<Int, Int>.plus(length: Int) = (first + length) to length
        }
    }
}

sealed class WhiteSpace(string: String) : Token.Type(string)

object WhiteSpaceExceptNewLine : WhiteSpace("WhiteSpaceExceptNewLine")
object NewLine : WhiteSpace("NewLine")
object LeftCurly : Token.Type("LeftCurly")
object RightCurly : Token.Type("RightCurly")
object Colon : Token.Type("Colon")
object Comma : Token.Type("Comma")
object CustomValue : Token.Type("CustomValue")
object EndOfProgram : Token.Type("EndOfProgram")

sealed class Keyword(string: String) : Token.Type(string)
sealed class WsType(string: String) : Keyword(string)

object WsTypeDef : Keyword("WsTypeDef")
object WsString : WsType("WsString")
object WsInteger : WsType("WsInteger")
object WsBoolean : WsType("WsBoolean")
object CustomType : WsType("CustomType")
