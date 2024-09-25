package community.flock.wirespec.compiler.core.tokenize.types

fun TokenType.name(): String = this::class.simpleName!!

sealed interface TokenType
data object RightCurly : TokenType
data object Colon : TokenType
data object Comma : TokenType
data object QuestionMark : TokenType
data object Hash : TokenType
data object Brackets : TokenType
data object CustomValue : TokenType
data object WsComment : TokenType
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
data object WsTypeDef : WirespecDefinition
data object WsEnumTypeDef : WirespecDefinition
data object WsEndpointDef : WirespecDefinition
data object WsChannelDef : WirespecDefinition

sealed interface WirespecType : Keyword
data object WsString : WirespecType
data object WsInteger : WirespecType
data object WsNumber : WirespecType
data object WsBoolean : WirespecType
data object CustomType : WirespecType
data object WsUnit : WirespecType

data object Method : Keyword

data object Path : Keyword

data object StatusCode : Keyword
