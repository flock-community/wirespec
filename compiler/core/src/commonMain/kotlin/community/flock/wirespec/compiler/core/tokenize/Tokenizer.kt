package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.LanguageSpec
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException.TokenizerException
import community.flock.wirespec.compiler.core.tokenize.Token.Index
import community.flock.wirespec.compiler.core.tokenize.Token.Index.Companion.plus

fun LanguageSpec.tokenize(source: String): Either<CompilerException, List<Token>> = either {
    with(tokenize(source, listOf())) {
        this + Token(type = EndOfProgram, value = "EOP", index = lastIndex()).run { copy(index = newIndex()) }
    }
}

private tailrec fun LanguageSpec.tokenize(source: String, tokens: List<Token>): List<Token> {
    val token = findToken(tokens.lastIndex(), source)
    val newSource = source.removePrefix(token.value)
    return if (newSource.isEmpty()) tokens + token
    else tokenize(newSource, tokens + token)
}

private fun LanguageSpec.findToken(oldIdx: Index, source: String): Token = matchers
    .firstNotNullOfOrNull { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, oldIdx) }
    ?: throw TokenizerException(
        oldIdx,
        "At line ${oldIdx.line}, position ${oldIdx.position}, character: '${source.first()}' in '\n${source.partial()}\n' is not expected"
    )

private fun MatchResult.toToken(type: Token.Type, oldIdx: Index) = Token(type, value, oldIdx)
    .run { copy(index = newIndex()) }

private fun List<Token>.lastIndex() = lastOrNull()?.index ?: Index()

private fun Token.newIndex() =
    if (type is NewLine) Index(line = index.line + 1, idxAndLength = index.idxAndLength + value.length)
    else index.copy(position = index.position + value.length, idxAndLength = index.idxAndLength + value.length)

private fun String.partial() = take(24) + if (length > 24) "..." else ""
