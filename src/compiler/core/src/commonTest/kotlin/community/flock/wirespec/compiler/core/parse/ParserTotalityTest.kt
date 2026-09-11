package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.test.WirespecFeatures
import community.flock.wirespec.compiler.test.WirespecSourceArb
import community.flock.wirespec.compiler.test.forEachSample
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldNotBeBlank
import kotlin.test.Test
import kotlin.test.fail

/**
 * `parse` reports failure through `EitherNel`, so no input may make it throw — not truncated
 * source, not random bytes, not a well-formed spec mutated halfway through a definition.
 */
class ParserTotalityTest {

    @Test
    fun generatedSourceParses() {
        WirespecSourceArb.source(WirespecFeatures.all).forEachSample { source ->
            parse(source).fold(
                { errors -> fail("expected a parse, got: ${errors.joinToString { it.message }}") },
                { ast -> ast.modules.flatMap(Module::statements).shouldNotBeEmpty() },
            )
        }
    }

    @Test
    fun arbitraryTextYieldsErrorsNeverExceptions() {
        WirespecSourceArb.noise().forEachSample { source ->
            parse(source).fold(
                { errors -> errors.forEach { it.message.shouldNotBeBlank() } },
                { ast -> ast.modules.flatMap(Module::statements).shouldNotBeEmpty() },
            )
        }
    }

    @Test
    fun everyReportedErrorCarriesAPosition() {
        WirespecSourceArb.noise().forEachSample { source ->
            parse(source).onLeft { errors ->
                errors.forEach {
                    check(it.coordinates.line >= 1) { "line ${it.coordinates.line} for: ${it.message}" }
                    check(it.coordinates.position >= 1) { "position ${it.coordinates.position} for: ${it.message}" }
                }
            }
        }
    }
}

private fun parse(source: String) = object : ParseContext, NoLogger {
    override val spec = WirespecSpec
}.parse(nonEmptyListOf(ModuleContent(FileUri("property.ws"), source)))
