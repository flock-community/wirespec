package community.flock.wirespec.compiler.tokenize

import community.flock.wirespec.WireSpecException.CompilerException.TokenizerException
import community.flock.wirespec.compiler.LanguageSpec

infix fun LanguageSpec.tokenize(source: String): List<Token> =
    tokenize(source, 1L, listOf()) + Token(type = EndOfProgram, value = "EOP", index = source.length + 1L)

private tailrec fun LanguageSpec.tokenize(source: String, index: Long, tokens: List<Token>): List<Token> {
    val token = findToken(index, source)
    val newSource = source.removePrefix(token.value)
    return if (newSource.isEmpty()) tokens + token
    else tokenize(newSource, index + token.value.length, tokens + token)
}

private fun LanguageSpec.findToken(index: Long, source: String): Token = matchers
    .firstNotNullOfOrNull { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, index) }
    ?: throw TokenizerException("At position $index, character: '${source.first()}' in '${source.partial()}' is not expected")


private fun MatchResult.toToken(type: Token.Type, index: Long) = Token(type, value, index)

private fun String.partial() = take(24) + if (length > 24) "..." else ""
