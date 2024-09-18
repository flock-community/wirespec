package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.types.TokenType
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

fun testTokenizer(
    source: String,
    vararg expected: TokenType,
    removeWhiteSpace: Boolean = true,
) {
    WirespecSpec.tokenize(source)
        .run { if (removeWhiteSpace) removeWhiteSpace() else this }
        .shouldNotBeEmpty()
        .apply { size shouldBe expected.size }
        .map { it.type }
        .onEachIndexed { index, tokenType -> tokenType shouldBe expected[index] }
}
