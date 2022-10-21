package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.LanguageSpec
import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.NewLine
import community.flock.wirespec.compiler.core.tokenize.types.TokenType

fun LanguageSpec.tokenize(source: String): List<Token> = tokenize(source, listOf()).let {
    it + Token(
        type = EndOfProgram,
        value = EndOfProgram.value,
        coordinates = it.lastCoordinates().nextCoordinates(EndOfProgram, EndOfProgram.value)
    )
}

private tailrec fun LanguageSpec.tokenize(source: String, tokens: List<Token>): List<Token> {
    val token = findToken(tokens.lastCoordinates(), source)
    val newSource = source.removePrefix(token.value)
    val newTokens = tokens + token
    return if (newSource.isEmpty()) newTokens
    else tokenize(newSource, newTokens)
}

private fun LanguageSpec.findToken(previousTokenCoordinates: Coordinates, source: String): Token = orderedMatchers
    .firstNotNullOf { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, previousTokenCoordinates) }

private fun MatchResult.toToken(type: TokenType, previousTokenCoordinates: Coordinates) =
    Token(type, value, previousTokenCoordinates.nextCoordinates(type, value))

private fun List<Token>.lastCoordinates() = lastOrNull()?.coordinates ?: Coordinates()

private fun Coordinates.nextCoordinates(type: TokenType, value: String) = when (type) {
    is NewLine -> Coordinates(
        line = line + 1,
        idxAndLength = idxAndLength + value.length
    )
    else -> this + value.length
}
