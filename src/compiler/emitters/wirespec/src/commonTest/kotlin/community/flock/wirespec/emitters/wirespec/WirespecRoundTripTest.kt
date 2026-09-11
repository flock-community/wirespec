package community.flock.wirespec.emitters.wirespec

import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.test.CompileAnyTest
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRpcTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.Fixture
import community.flock.wirespec.compiler.test.WirespecFeatures
import community.flock.wirespec.compiler.test.WirespecSourceArb
import community.flock.wirespec.compiler.test.forEachSample
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import kotlin.test.Test
import kotlin.test.fail

/**
 * `source -> AST -> source -> AST` has to reach the same AST twice. It is the only check that
 * the Wirespec emitter still speaks the language the parser reads; every other test in this
 * module compares emitter output against a fixture, which moves whenever the emitter does.
 *
 * The generated half runs on [WirespecFeatures.roundTrippable] rather than the whole grammar,
 * because the emitter drops constructs the parser accepts — see that constant for the list.
 */
class WirespecRoundTripTest {

    @Test
    fun generatedSourceSurvivesARoundTrip() {
        WirespecSourceArb.source(WirespecFeatures.roundTrippable).forEachSample(count = 300) { it.shouldRoundTrip() }
    }

    @Test
    fun fixturesSurviveARoundTrip() {
        listOf<Fixture>(
            CompileAnyTest,
            CompileChannelTest,
            CompileComplexModelTest,
            CompileEnumTest,
            CompileFullEndpointTest,
            CompileMinimalEndpointTest,
            CompileNestedTypeTest,
            CompileRpcTest,
            CompileTypeTest,
            CompileUnionTest,
        ).forEach { it.source.shouldRoundTrip() }
    }
}

private fun String.shouldRoundTrip() {
    val before = parse(this).orFail { "source did not parse: $it\n\n$this" }
    val emitted = WirespecEmitter().emit(before, noLogger).joinToString("\n") { it.result }
    val after = parse(emitted).orFail { "emitted source did not parse: $it\n\n--- emitted ---\n$emitted" }
    if (after.statements() != before.statements()) {
        fail(
            "round trip changed the AST\n--- source ---\n$this\n--- emitted ---\n$emitted\n" +
                "--- before ---\n${before.statements().joinToString("\n")}\n--- after ---\n${after.statements().joinToString("\n")}",
        )
    }
}

private fun EitherNel<WirespecException, AST>.orFail(message: (String) -> String): AST = fold(
    { errors -> fail(message(errors.joinToString { it.message })) },
    { it },
)

private fun AST.statements(): List<Definition> = modules.flatMap(Module::statements)

private fun parse(source: String) = object : ParseContext, NoLogger {
    override val spec = WirespecSpec
}.parse(nonEmptyListOf(ModuleContent(FileUri("roundtrip.ws"), source)))
