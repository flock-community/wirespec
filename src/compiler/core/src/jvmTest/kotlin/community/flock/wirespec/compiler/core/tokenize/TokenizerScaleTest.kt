package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlin.test.Test
import kotlin.time.measureTime

/**
 * Size and shape limits, kept JVM-only because the growth check reads a wall clock.
 *
 * Tokenizing is linear work in principle — every character belongs to exactly one token —
 * so none of these inputs is unreasonable. They are here because each one used to fail:
 * regex literals and long block comments exhausted the stack, and the matcher loop grew
 * quadratically with the size of the source.
 */
class TokenizerScaleTest {

    @Test
    fun tokenizesManyRegexConstrainedDefinitions() {
        val source = (0 until 1_000).joinToString("\n") { "type Model$it = String(/[a-z]+/)" }
        WirespecSpec.tokenize(source).size shouldBeGreaterThan 1_000
    }

    @Test
    fun tokenizesManyAnnotatedDefinitions() {
        val source = (0 until 5_000).joinToString("\n") { "@Tagged(env: \"dev\")\ntype Model$it {\n  field$it: String\n}" }
        WirespecSpec.tokenize(source).size shouldBeGreaterThan 5_000
    }

    @Test
    fun tokenizesALongBlockComment() {
        val source = "/* ${"documentation ".repeat(5_000)} */\ntype Model { field: String }"
        WirespecSpec.tokenize(source).size shouldBeGreaterThan 1
    }

    @Test
    fun tokenizesALongLineComment() {
        val source = "// ${"documentation ".repeat(5_000)}\ntype Model { field: String }"
        WirespecSpec.tokenize(source).size shouldBeGreaterThan 1
    }

    /**
     * Half a megabyte of source, on a budget only a linear tokenizer can meet.
     *
     * A ratio between two sizes would read more directly, but wall-clock ratios swing wildly
     * when the whole build runs in parallel. An absolute budget with a large margin survives a
     * loaded CI box and still fails a quadratic implementation by a mile: when this was written
     * the tokenizer took ~73ms here, and the substring-slicing version it replaced needed ~3.7s.
     */
    @Test
    fun tokenizesHalfAMegabyteOfSourceQuickly() {
        tokenize(spec(250))
        fastest(spec(8_000)) shouldBeLessThan 2_000_000.0
    }

    private fun spec(definitions: Int) = (0 until definitions).joinToString("\n") {
        "type Model$it {\n  fieldA$it: String,\n  fieldB$it: Integer\n}"
    }

    private fun tokenize(source: String) = WirespecSpec.tokenize(source)

    /** Microseconds for the fastest of three runs, so a single JIT or GC hiccup cannot fail it. */
    private fun fastest(source: String) = (0 until 3)
        .minOf { measureTime { tokenize(source) }.inWholeMicroseconds }
        .toDouble()
}
