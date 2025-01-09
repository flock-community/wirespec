package community.flock.wirespec.compiler.core.tokenize

import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.LanguageSpec
import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates

typealias Tokens = NonEmptyList<Token>

fun Tokens.removeWhiteSpace(): Tokens = filterNot { it.type is WhiteSpace }.toNonEmptyListOrNull() ?: endToken().nel()

fun LanguageSpec.tokenize(source: String): Tokens =
    tokenize(source, nonEmptyListOf(Token(type = StartOfProgram, value = "", coordinates = Coordinates())))

private tailrec fun LanguageSpec.tokenize(source: String, incompleteTokens: Tokens): Tokens {
    val (token, remaining) = extractToken(source, incompleteTokens.last().coordinates)
    val tokens = incompleteTokens + token
    return if (token.type is EndOfProgram) tokens
    else tokenize(remaining, tokens)
}

private fun LanguageSpec.extractToken(source: String, previousTokenCoordinates: Coordinates) = orderedMatchers
    .firstNotNullOfOrNull { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, previousTokenCoordinates) }
    ?.let { it to source.removePrefix(it.value) }
    ?: Pair(endToken(previousTokenCoordinates), "")

private fun MatchResult.toToken(type: TokenType, previousTokenCoordinates: Coordinates) =
    Token(value, type, previousTokenCoordinates.nextCoordinates(type, value))

private fun Coordinates.nextCoordinates(type: TokenType, value: String) = when (type) {
    is NewLine -> Coordinates(
        line = line + 1,
        idxAndLength = idxAndLength + value.length
    )

    else -> this + value.length
}

private fun endToken(previousTokenCoordinates: Coordinates = Coordinates()) = Token(
    type = EndOfProgram,
    value = EndOfProgram.VALUE,
    coordinates = previousTokenCoordinates.nextCoordinates(EndOfProgram, EndOfProgram.VALUE)
)
