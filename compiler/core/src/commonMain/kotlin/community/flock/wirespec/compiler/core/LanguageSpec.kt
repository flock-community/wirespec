package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.CustomType
import community.flock.wirespec.compiler.core.tokenize.CustomValue
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.NewLine
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.WhiteSpaceExceptNewLine
import community.flock.wirespec.compiler.core.tokenize.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.WsInteger
import community.flock.wirespec.compiler.core.tokenize.WsString
import community.flock.wirespec.compiler.core.tokenize.WsTypeDef

interface LanguageSpec {
    val matchers: List<Pair<Regex, Token.Type>>
}

object WireSpec : LanguageSpec {
    @Suppress("RegExpRedundantEscape")
    override val matchers = listOf(
        Regex("^type") to WsTypeDef,
        Regex("^[^\\S\\r\\n]+") to WhiteSpaceExceptNewLine,
        Regex("^[\\r\\n]") to NewLine,
        Regex("^\\{") to LeftCurly,
        Regex("^:") to Colon,
        Regex("^,") to Comma,
        Regex("^String") to WsString,
        Regex("^Integer") to WsInteger,
        Regex("^Boolean") to WsBoolean,
        Regex("^\\}") to RightCurly, // "^\\}" Redundant Escape in regex is needed for js compatibility
        Regex("^[a-z][a-zA-Z]*") to CustomValue,
        Regex("^[A-Z][a-zA-Z]*") to CustomType,
    )
}
