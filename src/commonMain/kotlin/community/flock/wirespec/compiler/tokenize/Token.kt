package community.flock.wirespec.compiler.tokenize

data class Token(
    val type: Type,
    val value: String,
    val index: Long
) {
    interface Type
}

object Whitespace : Token.Type
object Identifier : Token.Type
object EndOfProgram : Token.Type

object LeftCurly : Token.Type
object RightCurly : Token.Type
object Colon : Token.Type
object Comma : Token.Type

interface Keyword : Token.Type

object WsType : Keyword
object WsString : Keyword
object WsInteger : Keyword
