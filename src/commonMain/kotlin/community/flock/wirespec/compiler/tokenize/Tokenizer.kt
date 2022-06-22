package community.flock.wirespec.compiler.tokenize

import community.flock.wirespec.WireSpecException.CompilerException
import community.flock.wirespec.compiler.LanguageSpec

infix fun LanguageSpec.tokenize(source: String): List<Token> = source
    .tokenize(this) + Token(type = EndOfProgram, value = "EOP", index = source.length + 1L)

private fun String.tokenize(languageSpec: LanguageSpec, index: Long = 1L): List<Token> =
    when (val token = findToken(index, languageSpec)) {
        null -> throw noTokenFoundException(index)
        else -> with(removePrefix(token.value)) {
            if (isEmpty()) listOf(token) else listOf(token) + tokenize(languageSpec, index + token.value.length)
        }
    }

private fun String.findToken(index: Long, languageSpec: LanguageSpec): Token? = languageSpec.matchers
    .findPossibleMatches(this)
    .filterOnlyMatched()
    .mapToTokens(index)
    .firstOrNull()

private fun String.noTokenFoundException(index: Long) =
    CompilerException.TokenizerException("At position $index, character: '${first()}' in '${partial()}' is not expected")

private fun String.partial() = substring(0 until minOf(25, length)) + if (length > 24) "..." else ""

private fun List<Pair<Regex, Token.Type>>.findPossibleMatches(string: String) =
    map { (regex, tokenType) -> regex.find(string)?.value to tokenType }

private fun List<Pair<String?, Token.Type>>.filterOnlyMatched() =
    mapNotNull { (matched, tokenType) -> matched?.run { this to tokenType } }

private fun List<Pair<String, Token.Type>>.mapToTokens(index: Long) =
    map { (matched, tokenType) -> Token(tokenType, matched, index) }
