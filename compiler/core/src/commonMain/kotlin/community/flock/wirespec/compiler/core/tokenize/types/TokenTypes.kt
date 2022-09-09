package community.flock.wirespec.compiler.core.tokenize.types

sealed interface TokenType {
    fun name(): String = this::class.simpleName!!
}

object LeftCurly : TokenType
object RightCurly : TokenType
object Colon : TokenType
object Comma : TokenType
object CustomValue : TokenType
object EndOfProgram : TokenType

sealed interface WhiteSpace : TokenType
object WhiteSpaceExceptNewLine : WhiteSpace
object NewLine : WhiteSpace

sealed interface Keyword : TokenType
object WsTypeDef : Keyword

sealed interface WsType : Keyword {
    val iterable: Boolean
    val nullable: Boolean
}

class WsString(
    override val iterable: Boolean = false,
    override val nullable: Boolean = false
) : WsType

class WsInteger(
    override val iterable: Boolean = false,
    override val nullable: Boolean = false
) : WsType

class WsBoolean(
    override val iterable: Boolean = false,
    override val nullable: Boolean = false
) : WsType

class CustomType(
    override val iterable: Boolean = false,
    override val nullable: Boolean = false
) : WsType
