package community.flock.wirespec.compiler.core.tokenize

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.LanguageSpec
import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.NewLine
import community.flock.wirespec.compiler.core.tokenize.types.TokenType

fun LanguageSpec.tokenize(source: String): NonEmptyList<Token> = extractToken(source, Coordinates())
    .let { (token, remaining) -> tokenize(remaining, nonEmptyListOf(token)) }
    .let {
        it + Token(
            type = EndOfProgram,
            value = EndOfProgram.VALUE,
            coordinates = it.last().coordinates.nextCoordinates(EndOfProgram, EndOfProgram.VALUE)
        )
    }

private tailrec fun LanguageSpec.tokenize(source: String, incompleteTokens: NonEmptyList<Token>): NonEmptyList<Token> {
    val (token, remaining) = extractToken(source, incompleteTokens.last().coordinates)
    val tokens = incompleteTokens + token
    return if (remaining.isEmpty()) tokens
    else tokenize(remaining, tokens)
}

private fun LanguageSpec.extractToken(source: String, previousTokenCoordinates: Coordinates) = orderedMatchers
    .firstNotNullOf { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, previousTokenCoordinates) }
    .let { it to source.removePrefix(it.value) }

private fun MatchResult.toToken(type: TokenType, previousTokenCoordinates: Coordinates) =
    Token(type, value, previousTokenCoordinates.nextCoordinates(type, value))

private fun Coordinates.nextCoordinates(type: TokenType, value: String) = when (type) {
    is NewLine -> Coordinates(
        line = line + 1,
        idxAndLength = idxAndLength + value.length
    )

    else -> this + value.length
}
