package community.flock.wirespec.compiler.core.tokenize

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

fun Iterable<Token>.shouldBeSound(source: String): Iterable<Token> {
    with(filter { it.type != EndOfProgram }) {
        windowed(2, 1, false) { (a, b) ->
            a.coordinates.idxAndLength.idx + b.coordinates.idxAndLength.length shouldBe b.coordinates.idxAndLength.idx
            a.getEnd() shouldBe b.getStart()
            a.value.length shouldBe a.coordinates.idxAndLength.length
            b.value.length shouldBe b.coordinates.idxAndLength.length
        }
        source.length shouldBe sumOf { it.coordinates.idxAndLength.length }
    }
    return this
}

private fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length
private fun Token.getStart() = coordinates.getStartPos()
private fun Token.getEnd() = coordinates.idxAndLength.idx