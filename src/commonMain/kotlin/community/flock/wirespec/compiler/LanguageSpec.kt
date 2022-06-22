package community.flock.wirespec.compiler

import community.flock.wirespec.compiler.tokenize.Colon
import community.flock.wirespec.compiler.tokenize.Comma
import community.flock.wirespec.compiler.tokenize.Identifier
import community.flock.wirespec.compiler.tokenize.LeftCurly
import community.flock.wirespec.compiler.tokenize.RightCurly
import community.flock.wirespec.compiler.tokenize.Token
import community.flock.wirespec.compiler.tokenize.Whitespace
import community.flock.wirespec.compiler.tokenize.WsInteger
import community.flock.wirespec.compiler.tokenize.WsString
import community.flock.wirespec.compiler.tokenize.WsType

interface LanguageSpec {
    val matchers: List<Pair<Regex, Token.Type>>
}

object WireSpec : LanguageSpec {
    override val matchers = listOf(
        Regex("^type") to WsType,
        Regex("^\\s+") to Whitespace,
        Regex("^\\{") to LeftCurly,
        Regex("^:") to Colon,
        Regex("^,") to Comma,
        Regex("^String") to WsString,
        Regex("^Integer") to WsInteger,
        Regex("^}") to RightCurly,
        Regex("^[a-zA-Z]+") to Identifier,
        )
}
