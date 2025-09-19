package community.flock.wirespec.compiler.core.tokenize

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.WirespecSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

fun testTokenizer(
    source: String,
    vararg expected: TokenType,
    removeWhiteSpace: Boolean = true,
) {
    WirespecSpec
        .tokenize(source, TokenizeOptions(removeWhitespace = removeWhiteSpace))
        .shouldNotBeEmpty()
        .map { it.type }
        .also { println(it) }
        .let { it shouldBe expected }
}

fun NonEmptyList<Token>.shouldBeSound(source: String) = apply {
    dropLast(1)
        .also { tokens -> source.length shouldBe tokens.sumOf { it.coordinates.idxAndLength.length } }
        .windowed(2) { (a, b) ->
            a.coordinates.idxAndLength.idx + b.coordinates.idxAndLength.length shouldBe b.coordinates.idxAndLength.idx
            a.coordinates.idxAndLength.idx shouldBe b.coordinates.getStartPos()
            a.value.length shouldBe a.coordinates.idxAndLength.length
            b.value.length shouldBe b.coordinates.idxAndLength.length
        }
}

private fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length
