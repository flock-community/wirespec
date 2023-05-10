package community.flock.wirespec.compiler.core.tokenize.types

sealed interface TokenType {
    fun name(): String = this::class.simpleName!!
}

object LeftCurly : TokenType
object RightCurly : TokenType
object Colon : TokenType
object Comma : TokenType
object QuestionMark : TokenType
object Brackets : TokenType
object CustomValue : TokenType
object Invalid : TokenType
object EndOfProgram : TokenType {
    const val value = "EOP"
}

sealed interface WhiteSpace : TokenType
object WhiteSpaceExceptNewLine : WhiteSpace
object NewLine : WhiteSpace

sealed interface Keyword : TokenType
object WsTypeDef : Keyword
object WsRefinedTypeDef : Keyword

sealed interface WsType : Keyword
object WsString : WsType
object WsInteger : WsType
object WsBoolean : WsType
object CustomType : WsType

object CustomRegex : TokenType
