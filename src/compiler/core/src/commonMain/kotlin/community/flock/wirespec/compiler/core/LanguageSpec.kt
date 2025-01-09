package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Brackets
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Character
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.CustomType
import community.flock.wirespec.compiler.core.tokenize.CustomValue
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.Equals
import community.flock.wirespec.compiler.core.tokenize.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.Hash
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.Method
import community.flock.wirespec.compiler.core.tokenize.NewLine
import community.flock.wirespec.compiler.core.tokenize.Path
import community.flock.wirespec.compiler.core.tokenize.Pipe
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.StatusCode
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.TypeDefinition
import community.flock.wirespec.compiler.core.tokenize.WhiteSpaceExceptNewLine
import community.flock.wirespec.compiler.core.tokenize.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.WsBytes
import community.flock.wirespec.compiler.core.tokenize.WsInteger
import community.flock.wirespec.compiler.core.tokenize.WsNumber
import community.flock.wirespec.compiler.core.tokenize.WsString
import community.flock.wirespec.compiler.core.tokenize.WsUnit

typealias TokenMatcher = Pair<Regex, TokenType>

interface LanguageSpec {
    val customType: CustomType
    val orderedMatchers: List<TokenMatcher>
}

object WirespecSpec : LanguageSpec {
    override val customType = WsCustomType
    override val orderedMatchers = listOf(
        Regex("^type") to TypeDefinition,
        Regex("^enum") to EnumTypeDefinition,
        Regex("^endpoint") to EndpointDefinition,
        Regex("^channel") to ChannelDefinition,
        Regex("^[^\\S\\r\\n]+") to WhiteSpaceExceptNewLine,
        Regex("^[\\r\\n]") to NewLine,
        Regex("^\\{") to LeftCurly,
        Regex("^\\}") to RightCurly,
        Regex("^->") to Arrow,
        Regex("^=") to Equals,
        Regex("^\\|") to Pipe,
        Regex("^:") to Colon,
        Regex("^,") to Comma,
        Regex("^\\?") to QuestionMark,
        Regex("^#") to Hash,
        Regex("^\\[\\]") to Brackets,
        Regex("^GET|^POST|^PUT|^DELETE|^OPTIONS|^HEAD|^PATCH|^TRACE") to Method,
        Regex("^[1-5][0-9][0-9]") to StatusCode,
        Regex("^[a-z`][a-zA-Z0-9_`]*") to CustomValue,
        Regex("^[A-Z][a-zA-Z0-9_]*") to customType,
        Regex("^/[a-zA-Z0-9-_]+") to Path,
        Regex("^\\/\\*(\\*(?!\\/)|[^*])*\\*\\/") to Comment,
        Regex("^/") to ForwardSlash,
        Regex("^.") to Character // Catch all regular expression if none of the above matched
    )
}

data object WsCustomType : CustomType {
    override val types = mapOf(
        "Boolean" to WsBoolean,
        "Bytes" to WsBytes,
        "Integer" to WsInteger,
        "Integer32" to WsInteger,
        "Number" to WsNumber,
        "Number32" to WsNumber,
        "String" to WsString,
        "Unit" to WsUnit,
    )
}
