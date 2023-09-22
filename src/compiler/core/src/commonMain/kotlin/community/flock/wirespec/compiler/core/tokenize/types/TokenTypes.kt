package community.flock.wirespec.compiler.core.tokenize.types

sealed interface TokenType {
    fun name(): String = this::class.simpleName!!
}

data object LeftCurly : TokenType
data object RightCurly : TokenType
data object Colon : TokenType
data object Comma : TokenType
data object QuestionMark : TokenType
data object Brackets : TokenType
data object CustomValue : TokenType
data object Invalid : TokenType
data object EndOfProgram : TokenType {
    const val VALUE = "EOP"
}

sealed interface WhiteSpace : TokenType
data object WhiteSpaceExceptNewLine : WhiteSpace
data object NewLine : WhiteSpace

sealed interface Keyword : TokenType
sealed interface WirespecDefinition : Keyword
data object WsTypeDef : WirespecDefinition
data object WsEnumTypeDef : WirespecDefinition
data object WsRefinedTypeDef : WirespecDefinition
data object WsEndpointDef : WirespecDefinition

sealed interface WirespecType : Keyword
data object WsString : WirespecType
data object WsInteger : WirespecType
data object WsBoolean : WirespecType
data object CustomType : WirespecType

sealed interface Method : Keyword
data object GET : Method
data object POST : Method
data object PUT : Method
data object DELETE : Method
data object OPTIONS : Method
data object HEAD : Method
data object PATCH : Method
data object TRACE : Method

data object StatusCode : Keyword
data object Path : Keyword
data object Arrow : Keyword

data object CustomRegex : TokenType
