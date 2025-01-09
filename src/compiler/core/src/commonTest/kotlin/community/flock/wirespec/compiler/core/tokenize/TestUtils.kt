package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.WsCustomType
import community.flock.wirespec.compiler.core.optimize.optimize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

fun testTokenizer(
    source: String,
    vararg expected: TokenType,
    removeWhiteSpace: Boolean = true,
) {
    WirespecSpec.tokenize(source)
        .optimize(WsCustomType.types)
        .run { if (removeWhiteSpace) removeWhiteSpace() else this }
        .shouldNotBeEmpty()
        .apply { size shouldBe expected.size }
        .map { it.type }
        .onEachIndexed { index, tokenType -> tokenType shouldBe expected[index] }
}
