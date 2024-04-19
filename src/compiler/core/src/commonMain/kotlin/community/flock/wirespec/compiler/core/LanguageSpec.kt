package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomRegex
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.Equals
import community.flock.wirespec.compiler.core.tokenize.types.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.types.Hash
import community.flock.wirespec.compiler.core.tokenize.types.Invalid
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Method
import community.flock.wirespec.compiler.core.tokenize.types.NewLine
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.Pipe
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StatusCode
import community.flock.wirespec.compiler.core.tokenize.types.TokenType
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpaceExceptNewLine
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import community.flock.wirespec.compiler.core.tokenize.types.WsEnumTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsNumber
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsUnit

typealias TokenMatcher = Pair<Regex, TokenType>

interface LanguageSpec {
    val orderedMatchers: List<TokenMatcher>
}

object WirespecSpec : LanguageSpec {
    @Suppress("RegExpRedundantEscape")
    override val orderedMatchers = listOf(
        Regex("^type") to WsTypeDef,
        Regex("^enum") to WsEnumTypeDef,
        Regex("^endpoint") to WsEndpointDef,
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
        Regex("^String") to WsString,
        Regex("^Integer") to WsInteger,
        Regex("^Number") to WsNumber,
        Regex("^Boolean") to WsBoolean,
        Regex("^Unit") to WsUnit,
        Regex("^GET|^POST|^PUT|^DELETE|^OPTIONS|^HEAD|^PATCH|^TRACE") to Method,
        Regex("^/.*/g") to CustomRegex,
        Regex("^[1-5][0-9][0-9]") to StatusCode,
        Regex("^[a-z][a-zA-Z]*") to CustomValue,
        Regex("^[A-Z][a-zA-Z_]*") to CustomType,
        Regex("^/[a-z]+") to Path,
        Regex("^/") to ForwardSlash,
        Regex("^.") to Invalid // Catch all regular expression if none of the above matched
    )
}
