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
