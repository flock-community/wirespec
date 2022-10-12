package community.flock.wirespec.compiler.core.tokenize.types

sealed interface TokenType {
    fun name(): String = this::class.simpleName!!
}

object LeftCurly : TokenType
object Slash : TokenType
object RightCurly : TokenType
object Colon : TokenType
object Comma : TokenType
object QuestionMark : TokenType
object Arrow : TokenType
object Brackets : TokenType
object CustomValue : TokenType
object EndOfProgram : TokenType
object Invalid : TokenType

sealed interface WhiteSpace : TokenType
object WhiteSpaceExceptNewLine : WhiteSpace
object NewLine : WhiteSpace

sealed interface Keyword : TokenType
object WsTypeDef : Keyword
object WsEndpointDef : Keyword
sealed interface WsType : Keyword
object WsString : WsType
object WsInteger : WsType
object WsBoolean : WsType
object CustomType : WsType
