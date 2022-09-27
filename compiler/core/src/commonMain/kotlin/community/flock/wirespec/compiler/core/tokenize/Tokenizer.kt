package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.LanguageSpec
import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.NewLine
import community.flock.wirespec.compiler.core.tokenize.types.TokenType

fun LanguageSpec.tokenize(source: String): List<Token> = with(tokenize(source, listOf())) {
    this + Token(
        type = EndOfProgram,
        value = "EOP",
        coordinates = lastIndex()
    ).run { copy(coordinates = newIndex()) }
}

private tailrec fun LanguageSpec.tokenize(source: String, tokens: List<Token>): List<Token> {
    val token = findToken(tokens.lastIndex(), source)
    val newSource = source.removePrefix(token.value)
    return if (newSource.isEmpty()) tokens + token
    else tokenize(newSource, tokens + token)
}

private fun LanguageSpec.findToken(oldIdx: Coordinates, source: String): Token = matchers
    .firstNotNullOf { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, oldIdx) }

private fun MatchResult.toToken(type: TokenType, oldIdx: Coordinates) = Token(type, value, oldIdx)
    .run { copy(coordinates = newIndex()) }

private fun List<Token>.lastIndex() = lastOrNull()?.coordinates ?: Coordinates()

private fun Token.newIndex() =
    if (type is NewLine) Coordinates(
        line = coordinates.line + 1,
        idxAndLength = coordinates.idxAndLength + value.length
    )
    else coordinates.copy(
        position = coordinates.position + value.length,
        idxAndLength = coordinates.idxAndLength + value.length
    )

private fun String.partial(numberOfChars: Int = 24) = take(numberOfChars) + if (length > numberOfChars) "..." else ""
