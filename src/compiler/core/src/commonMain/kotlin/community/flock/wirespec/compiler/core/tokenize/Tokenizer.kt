package community.flock.wirespec.compiler.core.tokenize

import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.LanguageSpec
import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates

typealias Tokens = NonEmptyList<Token>

data class OptimizeOptions(
    val removeWhitespace: Boolean = true,
    val specify: Boolean = true,
)

fun LanguageSpec.tokenize(source: String, options: OptimizeOptions = OptimizeOptions()): Tokens = tokenize(source, nonEmptyListOf(Token(type = StartOfProgram, value = "", coordinates = Coordinates())))
    .let(optimize(options))

private tailrec fun LanguageSpec.tokenize(source: String, incompleteTokens: Tokens): Tokens {
    val (token, remaining) = extractToken(source, incompleteTokens.last().coordinates)
    val tokens = incompleteTokens + token
    return if (token.type is EndOfProgram) {
        tokens
    } else {
        tokenize(remaining, tokens)
    }
}

private fun LanguageSpec.extractToken(source: String, previousTokenCoordinates: Coordinates) = orderedMatchers
    .firstNotNullOfOrNull { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, previousTokenCoordinates) }
    ?.let { it to source.removePrefix(it.value) }
    ?: Pair(endToken(previousTokenCoordinates), "")

private fun MatchResult.toToken(type: TokenType, previousTokenCoordinates: Coordinates) = Token(value, type, previousTokenCoordinates.nextCoordinates(type, value))

private fun Coordinates.nextCoordinates(type: TokenType, value: String) = when (type) {
    is NewLine -> Coordinates(
        line = line + 1,
        idxAndLength = idxAndLength + value.length,
    )

    else -> this + value.length
}

private fun endToken(previousTokenCoordinates: Coordinates = Coordinates()) = Token(
    type = EndOfProgram,
    value = EndOfProgram.VALUE,
    coordinates = previousTokenCoordinates.nextCoordinates(EndOfProgram, EndOfProgram.VALUE),
)

private fun LanguageSpec.optimize(options: OptimizeOptions) = { tokens: Tokens ->
    tokens
        .runOption(options.removeWhitespace) { removeWhiteSpace() }
        .runOption(options.specify) { map { it.specify(customType.types) } }
}

private fun Tokens.runOption(bool: Boolean, block: Tokens.() -> Tokens) = if (bool) block() else this

private fun Tokens.removeWhiteSpace(): Tokens = filterNot { it.type is WhiteSpace }.toNonEmptyListOrNull() ?: endToken().nel()

private fun Token.specify(entries: Map<String, SpecificType>) = when (type) {
    is CustomType -> entries[value]
        ?.let { copy(type = it) }
        ?: this

    else -> this
}
