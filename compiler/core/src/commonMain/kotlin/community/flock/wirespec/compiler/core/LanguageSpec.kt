package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.NewLine
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.TokenType
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpaceExceptNewLine
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
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
        Regex("^[^\\S\\r\\n]+") to WhiteSpaceExceptNewLine,
        Regex("^[\\r\\n]") to NewLine,
        Regex("^\\{") to LeftCurly,
        Regex("^:") to Colon,
        Regex("^,") to Comma,
        Regex("^String\\?") to WsString(nullable = true),
        Regex("^String\\[\\]\\?") to WsString(iterable = true, nullable = true),
        Regex("^String\\[\\]") to WsString(iterable = true),
        Regex("^String") to WsString(),
        Regex("^Integer\\?") to WsInteger(nullable = true),
        Regex("^Integer\\[\\]\\?") to WsInteger(iterable = true, nullable = true),
        Regex("^Integer\\[\\]") to WsInteger(iterable = true),
        Regex("^Integer") to WsInteger(),
        Regex("^Boolean\\?") to WsBoolean(nullable = true),
        Regex("^Boolean\\[\\]\\?") to WsBoolean(iterable = true, nullable = true),
        Regex("^Boolean\\[\\]") to WsBoolean(iterable = true),
        Regex("^Boolean") to WsBoolean(),
        Regex("^\\}") to RightCurly,
        Regex("^[a-z][a-zA-Z]*") to CustomValue,
        Regex("^[A-Z][a-zA-Z]*\\?") to CustomType(nullable = true),
        Regex("^[A-Z][a-zA-Z]*\\[\\]\\?") to CustomType(iterable = true, nullable = true),
        Regex("^[A-Z][a-zA-Z]*\\[\\]") to CustomType(iterable = true),
        Regex("^[A-Z][a-zA-Z]*") to CustomType(),
    )
}
