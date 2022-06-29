package community.flock.wirespec.compiler.tokenize

import community.flock.wirespec.WireSpecException.CompilerException.TokenizerException
import community.flock.wirespec.compiler.LanguageSpec
import community.flock.wirespec.compiler.tokenize.Token.Index

fun LanguageSpec.tokenize(source: String): List<Token> = with(tokenize(source, listOf())) {
    this + Token(type = EndOfProgram, value = "EOP", index = Index(count { it.type is NewLine } + 1))
}

private tailrec fun LanguageSpec.tokenize(source: String, tokens: List<Token>): List<Token> {
    val token = findToken(tokens.lastIndex(), source).run { copy(index = newIndex()) }
    val newSource = source.removePrefix(token.value)
    return if (newSource.isEmpty()) tokens + token
    else tokenize(newSource, tokens + token)
}

private fun LanguageSpec.findToken(index: Index, source: String): Token = matchers
    .firstNotNullOfOrNull { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, index) }
    ?: throw TokenizerException("At line ${index.line}, position ${index.position}, character: '${source.first()}' in '\n${source.partial()}\n' is not expected")

private fun MatchResult.toToken(type: Token.Type, index: Index) = Token(type, value, index)

private fun List<Token>.lastIndex() = lastOrNull()?.index ?: Index()

private fun Token.newIndex() =
    if (type is NewLine) Index(line = index.line + 1)
    else index.copy(position = index.position + value.length)

private fun String.partial() = take(24) + if (length > 24) "..." else ""
