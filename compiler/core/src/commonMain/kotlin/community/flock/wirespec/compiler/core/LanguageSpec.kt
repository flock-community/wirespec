package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.Invalid
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.NewLine
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.Slash
import community.flock.wirespec.compiler.core.tokenize.types.TokenType
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpaceExceptNewLine
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef

interface LanguageSpec {
    val matchers: List<Pair<Regex, TokenType>>
}

object WireSpec : LanguageSpec {
    @Suppress("RegExpRedundantEscape")
    override val matchers = listOf(
        Regex("^type") to WsTypeDef,
        Regex("^endpoint") to WsEndpointDef,
        Regex("^[^\\S\\r\\n]+") to WhiteSpaceExceptNewLine,
        Regex("^[\\r\\n]") to NewLine,
        Regex("^\\{") to LeftCurly,
        Regex("^:") to Colon,
        Regex("^,") to Comma,
        Regex("^\\?") to QuestionMark,
        Regex("^\\[\\]") to Brackets,
        Regex("^String") to WsString,
        Regex("^Integer") to WsInteger,
        Regex("^Boolean") to WsBoolean,
        Regex("^\\}") to RightCurly,
        Regex("^\\/") to Slash,
        Regex("^->") to Arrow,
        Regex("^[a-z][a-zA-Z]*") to CustomValue,
        Regex("^[A-Z][a-zA-Z]*") to CustomType,
        Regex("^.") to Invalid // Catch all regular expression if none of the above matched
    )
}
