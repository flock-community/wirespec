package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Brackets
import community.flock.wirespec.compiler.core.tokenize.CaseVariant
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Character
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.DromedaryCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.Equals
import community.flock.wirespec.compiler.core.tokenize.FieldIdentifier
import community.flock.wirespec.compiler.core.tokenize.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.Hash
import community.flock.wirespec.compiler.core.tokenize.Integer
import community.flock.wirespec.compiler.core.tokenize.KebabCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.LeftParentheses
import community.flock.wirespec.compiler.core.tokenize.Method
import community.flock.wirespec.compiler.core.tokenize.NewLine
import community.flock.wirespec.compiler.core.tokenize.PascalCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.Path
import community.flock.wirespec.compiler.core.tokenize.Pipe
import community.flock.wirespec.compiler.core.tokenize.Precision.P32
import community.flock.wirespec.compiler.core.tokenize.Precision.P64
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.RightParentheses
import community.flock.wirespec.compiler.core.tokenize.ScreamingKebabCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.ScreamingSnakeCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.SnakeCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.StatusCode
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.TypeDefinition
import community.flock.wirespec.compiler.core.tokenize.TypeIdentifier
import community.flock.wirespec.compiler.core.tokenize.WhiteSpaceExceptNewLine
import community.flock.wirespec.compiler.core.tokenize.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.WsBytes
import community.flock.wirespec.compiler.core.tokenize.WsInteger
import community.flock.wirespec.compiler.core.tokenize.WsNumber
import community.flock.wirespec.compiler.core.tokenize.WsString
import community.flock.wirespec.compiler.core.tokenize.WsUnit

typealias TokenMatcher = Pair<Regex, TokenType>

interface LanguageSpec {
    val typeIdentifier: TypeIdentifier
    val fieldIdentifier: FieldIdentifier
    val orderedMatchers: List<TokenMatcher>
}

interface HasLanguageSpec {
    val spec: LanguageSpec get() = WirespecSpec
}

object WirespecSpec : LanguageSpec {
    override val typeIdentifier = WirespecType
    override val fieldIdentifier = WirespecField
    override val orderedMatchers = listOf(
        Regex("^\\btype\\b") to TypeDefinition,
        Regex("^\\benum\\b") to EnumTypeDefinition,
        Regex("^\\bendpoint\\b") to EndpointDefinition,
        Regex("^\\bchannel\\b") to ChannelDefinition,
        Regex("^[^\\S\\r\\n]+") to WhiteSpaceExceptNewLine,
        Regex("^[\\r\\n]") to NewLine,
        Regex("^\\{") to LeftCurly,
        Regex("^\\}") to RightCurly,
        Regex("^\\(") to LeftParentheses,
        Regex("^\\)") to RightParentheses,
        Regex("^->") to Arrow,
        Regex("^=") to Equals,
        Regex("^\\|") to Pipe,
        Regex("^:") to Colon,
        Regex("^,") to Comma,
        Regex("^\\?") to QuestionMark,
        Regex("^#") to Hash,
        Regex("^\\[\\]") to Brackets,
        Regex("^\\b(GET|POST|PUT|DELETE|OPTIONS|HEAD|PATCH|TRACE)\\b") to Method,
        Regex("^[1-5][0-9][0-9]") to StatusCode,
        Regex("^[a-z`][a-zA-Z0-9_\\-`]*") to fieldIdentifier,
        Regex("^\\b[A-Z][a-zA-Z0-9_]*\\b") to typeIdentifier,
        Regex("^/[a-zA-Z0-9-_]+") to Path,
        Regex("^//.*\n") to Comment,
        Regex("^\\/\\*(\\*(?!\\/)|[^*])*\\*\\/") to Comment,
        Regex("^/") to ForwardSlash,
        Regex("^[0-9]*") to Integer,
        Regex("^.") to Character, // Catch-all regular expression if none of the above matched
    )
}

data object WirespecType : TypeIdentifier {
    override val specificTypes = mapOf(
        "Boolean" to WsBoolean,
        "Bytes" to WsBytes,
        "Integer" to WsInteger(P64),
        "Integer32" to WsInteger(P32),
        "Number" to WsNumber(P64),
        "Number32" to WsNumber(P32),
        "String" to WsString,
        "Unit" to WsUnit,
    )
}

data object WirespecField : FieldIdentifier {
    override val caseVariants: List<Pair<Regex, CaseVariant>> = listOf(
        Regex("([A-Z][a-z0-9]+)((\\d)|([A-Z0-9][a-z0-9]+))*([A-Z])?") to PascalCaseIdentifier,
        Regex("[a-z]+((\\d)|([A-Z0-9][a-z0-9]+))*([A-Z])?") to DromedaryCaseIdentifier,
        Regex("[a-z]+(?:-[a-z0-9]+)*") to KebabCaseIdentifier,
        Regex("[A-Z]+(?:-[A-Z0-9]+)*") to ScreamingKebabCaseIdentifier,
        Regex("[a-z]+(?:_[a-z0-9]+)*") to SnakeCaseIdentifier,
        Regex("[A-Z]+(?:_[A-Z0-9]+)*") to ScreamingSnakeCaseIdentifier,
    )
}
