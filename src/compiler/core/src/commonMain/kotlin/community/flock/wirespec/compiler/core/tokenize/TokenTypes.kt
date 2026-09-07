package community.flock.wirespec.compiler.core.tokenize

public fun TokenType.name(): String = this::class.simpleName!!

public sealed interface TokenType
public data object RightCurly : TokenType
public data object RightParenthesis : TokenType
public data object LeftBracket : TokenType
public data object RightBracket : TokenType

public data object Colon : TokenType
public data object Comma : TokenType
public data object QuestionMark : TokenType
public data object ExclamationMark : TokenType
public data object Hash : TokenType
public data object Brackets : TokenType
public data object Comment : TokenType
public data object Number : TokenType
public data object Integer : TokenType

public data object Underscore : TokenType
public data object Character : TokenType
public data object Arrow : TokenType
public data object Pipe : TokenType
public data object LiteralString : TokenType
public data object EndOfProgram : TokenType {
    const val VALUE: String = "EOP"
}

public sealed interface WirespecIdentifier : TokenType
public interface FieldIdentifier : WirespecIdentifier {
    public val caseVariants: List<Pair<Regex, CaseVariant>>
}

public data object RegExp : TokenType
public sealed interface CaseVariant : WirespecIdentifier
public data object PascalCaseIdentifier : CaseVariant
public data object DromedaryCaseIdentifier : CaseVariant
public data object KebabCaseIdentifier : CaseVariant
public data object ScreamingKebabCaseIdentifier : CaseVariant
public data object SnakeCaseIdentifier : CaseVariant
public data object ScreamingSnakeCaseIdentifier : CaseVariant

public data object Annotation : TokenType

internal sealed interface TypeDefinitionStart : TokenType
public data object LeftCurly : TypeDefinitionStart
public data object LeftParenthesis : TypeDefinitionStart
public data object ForwardSlash : TypeDefinitionStart
public data object Equals : TypeDefinitionStart

public sealed interface WhiteSpace : TokenType
internal data object WhiteSpaceExceptNewLine : WhiteSpace
internal data object NewLine : WhiteSpace
internal data object StartOfProgram : WhiteSpace

public sealed interface Keyword : TokenType
internal sealed interface WirespecDefinition : Keyword
public data object TypeDefinition : WirespecDefinition
public data object EnumTypeDefinition : WirespecDefinition
public data object ChannelDefinition : WirespecDefinition
public data object EndpointDefinition : WirespecDefinition
public data object RpcDefinition : WirespecDefinition

private sealed interface ChannelTokenType : TokenType
public data object Method : ChannelTokenType
public data object Path : ChannelTokenType

public sealed interface WirespecType : TokenType
public sealed interface SpecificType : WirespecType
internal sealed interface PrimitiveType : SpecificType
public interface TypeIdentifier : WirespecType {
    public val specificTypes: Map<String, SpecificType>
}

public data object WsUnit : SpecificType
public data object WsAny : SpecificType
public data object WsString : PrimitiveType
public data object WsBoolean : PrimitiveType
public data object WsBytes : PrimitiveType
public data class WsInteger(override val precision: Precision) :
    PrimitiveType,
    HasPrecision

public data class WsNumber(override val precision: Precision) :
    PrimitiveType,
    HasPrecision

private interface HasPrecision {
    val precision: Precision
}

public enum class Precision {
    P32,
    P64,
}
