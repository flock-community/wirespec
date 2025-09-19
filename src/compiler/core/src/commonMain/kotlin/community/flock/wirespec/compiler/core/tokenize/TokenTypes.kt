package community.flock.wirespec.compiler.core.tokenize

fun TokenType.name(): String = this::class.simpleName!!

sealed interface TokenType
data object RightCurly : TokenType
data object RightParenthesis : TokenType
data object LeftBracket : TokenType
data object RightBracket : TokenType

data object Colon : TokenType
data object Comma : TokenType
data object QuestionMark : TokenType
data object Hash : TokenType
data object Brackets : TokenType
data object Comment : TokenType
data object Number : TokenType
data object Integer : TokenType

data object Underscore : TokenType
data object Character : TokenType
data object Arrow : TokenType
data object Pipe : TokenType
data object LiteralString : TokenType
data object EndOfProgram : TokenType {
    const val VALUE = "EOP"
}

sealed interface WirespecIdentifier : TokenType
interface FieldIdentifier : WirespecIdentifier {
    val caseVariants: List<Pair<Regex, CaseVariant>>
}

data object RegExp : TokenType
sealed interface CaseVariant : WirespecIdentifier
data object PascalCaseIdentifier : CaseVariant
data object DromedaryCaseIdentifier : CaseVariant
data object KebabCaseIdentifier : CaseVariant
data object ScreamingKebabCaseIdentifier : CaseVariant
data object SnakeCaseIdentifier : CaseVariant
data object ScreamingSnakeCaseIdentifier : CaseVariant

data object Annotation : TokenType

sealed interface TypeDefinitionStart : TokenType
data object LeftCurly : TypeDefinitionStart
data object LeftParenthesis : TypeDefinitionStart
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

sealed interface WirespecType : TokenType
sealed interface SpecificType : WirespecType
sealed interface PrimitiveType : SpecificType
interface TypeIdentifier : WirespecType {
    val specificTypes: Map<String, SpecificType>
}

data object WsUnit : SpecificType
data object WsString : PrimitiveType
data object WsBoolean : PrimitiveType
data object WsBytes : PrimitiveType
data class WsInteger(override val precision: Precision) :
    PrimitiveType,
    HasPrecision

data class WsNumber(override val precision: Precision) :
    PrimitiveType,
    HasPrecision

interface HasPrecision {
    val precision: Precision
}

enum class Precision {
    P32,
    P64,
}
