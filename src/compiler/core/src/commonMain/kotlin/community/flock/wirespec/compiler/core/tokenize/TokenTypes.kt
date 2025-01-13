package community.flock.wirespec.compiler.core.tokenize

fun TokenType.name(): String = this::class.simpleName!!

sealed interface TokenType
data object RightCurly : TokenType
data object Colon : TokenType
data object Comma : TokenType
data object QuestionMark : TokenType
data object Hash : TokenType
data object Brackets : TokenType
data object CustomValue : TokenType
data object Comment : TokenType
data object Character : TokenType
data object Arrow : TokenType
data object Pipe : TokenType
data object EndOfProgram : TokenType {
    const val VALUE = "EOP"
}

sealed interface TypeDefinitionStart : TokenType
data object LeftCurly : TypeDefinitionStart
data object ForwardSlash : TypeDefinitionStart
data object Equals : TypeDefinitionStart

sealed interface WhiteSpace : TokenType
data object WhiteSpaceExceptNewLine : WhiteSpace
data object NewLine : WhiteSpace
data object StartOfProgram : WhiteSpace

sealed interface Keyword : TokenType
sealed interface WirespecDefinition : Keyword
data object TypeDefinition : WirespecDefinition
data object EnumTypeDefinition : WirespecDefinition
data object ChannelDefinition : WirespecDefinition
data object EndpointDefinition : WirespecDefinition

sealed interface ChannelTokenType : TokenType
data object Method : ChannelTokenType
data object Path : ChannelTokenType
data object StatusCode : ChannelTokenType

sealed interface WirespecType : TokenType
sealed interface SpecificType : WirespecType
interface CustomType : WirespecType {
    val types: Map<String, SpecificType>
}

data object WsString : SpecificType
data object WsBoolean : SpecificType
data object WsBytes : SpecificType
data object WsUnit : SpecificType
data class WsInteger(override val precision: Precision) : SpecificType, HasPrecision
data class WsNumber(override val precision: Precision) : SpecificType, HasPrecision

interface HasPrecision {
    val precision: Precision
}

enum class Precision {
    P32, P64
}
