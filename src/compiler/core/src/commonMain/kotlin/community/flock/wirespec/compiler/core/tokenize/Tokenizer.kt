package community.flock.wirespec.compiler.core.tokenize

import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.LanguageSpec
import community.flock.wirespec.compiler.core.removeBackticks
import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates

data class TokenizeOptions(
    val removeWhitespace: Boolean = true,
    val specifyTypes: Boolean = true,
    val specifyFieldIdentifiers: Boolean = true,
)

fun LanguageSpec.tokenize(source: String, options: TokenizeOptions = TokenizeOptions()): NonEmptyList<Token> = tokenize(source, nonEmptyListOf(Token(type = StartOfProgram, value = "", coordinates = Coordinates())))
    .let(optimize(options))

private tailrec fun LanguageSpec.tokenize(source: String, incompleteTokens: NonEmptyList<Token>): NonEmptyList<Token> {
    val (token, remaining) = extractToken(source, incompleteTokens.last().coordinates)
    val tokens = incompleteTokens + token
    return when (token.type) {
        is EndOfProgram -> tokens
        else -> tokenize(remaining, tokens)
    }
}

private fun LanguageSpec.extractToken(source: String, previousTokenCoordinates: Coordinates) = orderedMatchers
    .firstNotNullOfOrNull { (regex, tokenType) -> regex.find(source)?.toToken(tokenType, previousTokenCoordinates) }
    ?.let { it to source.removePrefix(it.value) }
    ?: Pair(endToken(previousTokenCoordinates), "")

private fun MatchResult.toToken(type: TokenType, previousTokenCoordinates: Coordinates) = Token(value, type, previousTokenCoordinates.nextCoordinates(type, value))

private fun Coordinates.nextCoordinates(type: TokenType, value: String) = when (type) {
    is NewLine -> Coordinates(line = line + 1, idxAndLength = idxAndLength + value.length)
    else -> this + value.length
}

private fun endToken(previousTokenCoordinates: Coordinates = Coordinates()) = Token(
    type = EndOfProgram,
    value = EndOfProgram.VALUE,
    coordinates = previousTokenCoordinates.nextCoordinates(EndOfProgram, EndOfProgram.VALUE),
)

private fun LanguageSpec.optimize(options: TokenizeOptions) = { tokens: NonEmptyList<Token> ->
    tokens
        .runOption(options.removeWhitespace) { removeWhiteSpace() }
        .runOption(options.specifyTypes) { map { it.specifyType(typeIdentifier.specificTypes) } }
        .runOption(options.specifyFieldIdentifiers) { map { it.specifyFieldIdentifier(fieldIdentifier.caseVariants) } }
}

private fun NonEmptyList<Token>.runOption(bool: Boolean, block: NonEmptyList<Token>.() -> NonEmptyList<Token>) = if (bool) block() else this

private fun NonEmptyList<Token>.removeWhiteSpace(): NonEmptyList<Token> = filterNot { it.type is WhiteSpace }.toNonEmptyListOrNull() ?: endToken().nel()

private fun Token.specifyType(entries: Map<String, SpecificType>) = when (type) {
    is TypeIdentifier -> entries[value]
        ?.let { copy(type = it) }
        ?: this

    else -> this
}

private fun Token.specifyFieldIdentifier(caseVariants: List<Pair<Regex, CaseVariant>>) = when (type) {
    is FieldIdentifier -> caseVariants.firstNotNullOfOrNull { (regex, variant) ->
        when {
            regex.matches(value.removeBackticks()) -> copy(type = variant)
            else -> null
        }
    }

    else -> null
} ?: this
