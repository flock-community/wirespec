package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.test.WirespecFeatures
import community.flock.wirespec.compiler.test.WirespecSourceArb
import community.flock.wirespec.compiler.test.forEachSample
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The example-based tokenizer tests pin down the token types of a handful of hand-written
 * sources. These pin down the invariants that have to hold for *every* source, reusing the
 * same [shouldBeSound] check the example tests already use.
 */
class TokenizerPropertyTest {

    private val keepWhitespace = TokenizeOptions(removeWhitespace = false)

    @Test
    fun generatedSourceTokenizesToASoundCoordinateChain() {
        WirespecSourceArb.source(WirespecFeatures.all).forEachSample { source ->
            WirespecSpec.tokenize(source, keepWhitespace).shouldBeSound(source)
        }
    }

    @Test
    fun arbitraryTextTokenizesToASoundCoordinateChain() {
        WirespecSourceArb.noise().forEachSample { source ->
            WirespecSpec.tokenize(source, keepWhitespace).shouldBeSound(source)
        }
    }

    @Test
    fun tokenizingIsLossless() {
        WirespecSourceArb.noise().forEachSample { source ->
            WirespecSpec.tokenize(source, keepWhitespace)
                .dropLast(1)
                .joinToString("") { it.value } shouldBe source
        }
    }

    @Test
    fun everySourceEndsInExactlyOneEndOfProgram() {
        WirespecSourceArb.noise().forEachSample { source ->
            val tokens = WirespecSpec.tokenize(source, keepWhitespace)
            tokens.last().type shouldBe EndOfProgram
            tokens.count { it.type is EndOfProgram } shouldBe 1
        }
    }

    @Test
    fun droppingWhitespaceDropsNothingElse() {
        WirespecSourceArb.noise().forEachSample { source ->
            WirespecSpec.tokenize(source, TokenizeOptions(removeWhitespace = true)).map { it.type } shouldBe
                WirespecSpec.tokenize(source, keepWhitespace).map { it.type }.filterNot { it is WhiteSpace }
        }
    }
}
