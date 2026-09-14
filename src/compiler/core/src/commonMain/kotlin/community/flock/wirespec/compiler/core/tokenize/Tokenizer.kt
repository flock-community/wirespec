package community.flock.wirespec.compiler.core.tokenize

import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.LanguageSpec
import community.flock.wirespec.compiler.core.removeBackticks
import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates

public data class TokenizeOptions(
    val removeWhitespace: Boolean = true,
    val specifyTypes: Boolean = true,
    val specifyFieldIdentifiers: Boolean = true,
)

public fun LanguageSpec.tokenize(source: String, options: TokenizeOptions = TokenizeOptions()): NonEmptyList<Token> = scan(source)
    .let(optimize(options))

/**
 * Walks the source once, carrying a position rather than slicing.
 *
 * Each step yields its tokens to a sequence rather than appending them to an accumulator:
 * appending to a persistent list copied every token that came before it, which made
 * tokenizing a file quadratic in its size.
 */
private fun LanguageSpec.scan(source: String): NonEmptyList<Token> = generateSequence(Token(value = "", type = StartOfProgram, coordinates = Coordinates()).nel() to 0) { (tokens, index) ->
    tokens.last().takeUnless { it.type is EndOfProgram }?.let { scanStep(source, index, it) }
}.flatMap { (tokens, _) -> tokens }.toList().toNonEmptyListOrNull() ?: endToken().nel()

private fun LanguageSpec.scanStep(source: String, index: Int, previous: Token): Pair<NonEmptyList<Token>, Int> = extractToken(source, index, previous.coordinates).let { (token, next) ->
    when (token.type) {
        // A `/` after `(` opens a regex literal, which the matchers cannot describe.
        is LeftParenthesis -> potentialRegex(source, next, token).let { (lookahead, resume) -> (token.nel() + lookahead) to resume }
        else -> token.nel() to next
    }
}

/**
 * Looks past the whitespace that may follow a `(` for the `/` that opens a regex literal.
 * Returns the tokens to append — the whitespace it consumed, plus the regex literal if it
 * found one — and the position to carry on from. When there is no regex the lookahead is
 * given back untouched, so the scan re-reads it.
 */
private tailrec fun LanguageSpec.potentialRegex(
    source: String,
    index: Int,
    previous: Token,
    whitespace: List<Token> = emptyList(),
): Pair<List<Token>, Int> {
    val (token, next) = extractToken(source, index, previous.coordinates)
    return when (token.type) {
        is WhiteSpaceExceptNewLine -> potentialRegex(source, next, token, whitespace + token)
        is ForwardSlash -> extractRegex(source, index, index + 1, previous).let { (regex, resume) -> (whitespace + regex) to resume }
        else -> whitespace to index
    }
}

private val NEW_LINE = Regex("[\\r\\n]")
private val ESCAPED_FORWARD_SLASH = Regex("\\\\/")
private val END_OF_REGEX = Regex("/[gimsuy]*")

/**
 * Reads a regex literal, from the `/` at [start] to its closing `/` and flags. An unterminated
 * literal ends at the newline or at the end of the file, exactly as it did before.
 */
private tailrec fun extractRegex(source: String, start: Int, index: Int, previous: Token): Pair<Token, Int> {
    val end = END_OF_REGEX.matchAt(source, index)
    return when {
        index >= source.length || NEW_LINE.matchesAt(source, index) -> previous.nextToken(RegExp, source.substring(start, index)) to index
        ESCAPED_FORWARD_SLASH.matchesAt(source, index) -> extractRegex(source, start, index + 2, previous)
        end == null -> extractRegex(source, start, index + 1, previous)
        else -> (index + end.value.length).let { previous.nextToken(RegExp, source.substring(start, it)) to it }
    }
}

private fun LanguageSpec.extractToken(source: String, index: Int, previousTokenCoordinates: Coordinates) = orderedMatchers
    .firstNotNullOfOrNull { (regex, tokenType) -> regex.matchAt(source, index)?.toToken(tokenType, previousTokenCoordinates) }
    ?.let { it to index + it.value.length }
    ?: (endToken(previousTokenCoordinates) to index)

private fun MatchResult.toToken(type: TokenType, previousTokenCoordinates: Coordinates) = Token(value, type, previousTokenCoordinates.nextCoordinates(type, value))

private fun Token.nextToken(type: TokenType, value: String): Token = this.copy(
    type = type,
    value = value,
    coordinates = coordinates.nextCoordinates(type, value),
)

private fun Coordinates.nextCoordinates(type: TokenType, value: String) = when (type) {
    is NewLine -> Coordinates(line = line + 1, idxAndLength = idxAndLength + value.length)
    else -> when (val newLineCount = value.count { it == '\n' }) {
        0 -> this + value.length
        // Tokens such as comments may span multiple lines; advance the line counter for each
        // embedded newline and reset the position to the column after the final newline.
        else -> Coordinates(
            line = line + newLineCount,
            position = value.length - value.lastIndexOf('\n'),
            idxAndLength = idxAndLength + value.length,
        )
    }
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
