package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.LanguageSpec
import community.flock.wirespec.compiler.core.exceptions.WireSpecException
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException.TokenizerException
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.tokenize.Token.Index

fun LanguageSpec.tokenize(source: String): Either<WireSpecException, List<Token>> = either {
    with(tokenize(source, listOf())) {
        this + Token(type = EndOfProgram, value = "EOP", index = Index(count { it.type is NewLine } + 1))
    }
}

private tailrec fun LanguageSpec.tokenize(source: String, tokens: List<Token>): List<Token> {
    val token = findToken(tokens.lastIndex(), source).run { copy(index = newIndex()) }
    val newSource = source.removePrefix(token.value)
    return if (newSource.isEmpty()) tokens + token
    else tokenize(newSource, tokens + token)
}

private fun LanguageSpec.findToken(index: Index, source: String): Token = matchers
    .firstNotNullOfOrNull { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, index) }
    ?: throw TokenizerException(index, "At line ${index.line}, position ${index.position}, character: '${source.first()}' in '\n${source.partial()}\n' is not expected")

private fun MatchResult.toToken(type: Token.Type, index: Index) = Token(type, value, index)

private fun List<Token>.lastIndex() = lastOrNull()?.index ?: Index()

private fun Token.newIndex() =
    if (type is NewLine) Index(line = index.line + 1)
    else index.copy(position = index.position + value.length)

private fun String.partial() = take(24) + if (length > 24) "..." else ""
