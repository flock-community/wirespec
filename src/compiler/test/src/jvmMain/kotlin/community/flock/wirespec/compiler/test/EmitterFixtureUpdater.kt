package community.flock.wirespec.compiler.test

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.utils.NoLogger
import java.io.File

/**
 * Shared runtime for the per-emitter `UpdateEmitterFixtures` mains. Each emitter
 * supplies a factory for its emitter class plus its language-specific
 * `sharedOutputTest` body; everything else is identical and lives here.
 */
object EmitterFixtureUpdater {

    fun run(
        args: Array<String>,
        language: String,
        fixtures: Map<String, () -> String>,
    ) {
        require(args.isNotEmpty()) { "Usage: UpdateEmitterFixtures <fixturesRootDir>" }
        val root = File(args[0])
        root.mkdirs()
        fixtures.forEach { (name, produce) ->
            val target = root.resolve(name).resolve("$language.txt")
            target.parentFile.mkdirs()
            target.writeText(produce())
            println("[$language] wrote ${target.absolutePath}")
        }
    }

    /** The nine standard `Compile*Test` fixtures shared by every emitter. */
    fun compileFixtures(emitterFactory: () -> Emitter): Map<String, () -> String> = linkedMapOf(
        "compileFullEndpointTest" to { compile(CompileFullEndpointTest, emitterFactory) },
        "compileChannelTest" to { compile(CompileChannelTest, emitterFactory) },
        "compileEnumTest" to { compile(CompileEnumTest, emitterFactory) },
        "compileMinimalEndpointTest" to { compile(CompileMinimalEndpointTest, emitterFactory) },
        "compileRefinedTest" to { compile(CompileRefinedTest, emitterFactory) },
        "compileUnionTest" to { compile(CompileUnionTest, emitterFactory) },
        "compileTypeTest" to { compile(CompileTypeTest, emitterFactory) },
        "compileNestedTypeTest" to { compile(CompileNestedTypeTest, emitterFactory) },
        "compileComplexModelTest" to { compile(CompileComplexModelTest, emitterFactory) },
    )

    /** The four NodeFixtures-driven tests included by emitters that have them. */
    fun nodeFixtures(emitterFactory: () -> Emitter): Map<String, () -> String> {
        val ctx = object : EmitContext, NoLogger {
            override val emitters = nonEmptySetOf(emitterFactory())
        }
        fun emit(node: Definition): String {
            val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(node))))
            return ctx.emitters.first().emit(ast, ctx.logger).first().result
        }
        return linkedMapOf(
            "testEmitterType" to { emit(NodeFixtures.type) },
            "testEmitterEmptyType" to { emit(NodeFixtures.emptyType) },
            "testEmitterRefined" to { emit(NodeFixtures.refined) },
            "testEmitterEnum" to { emit(NodeFixtures.enum) },
        )
    }

    private fun compile(fixture: Fixture, emitterFactory: () -> Emitter): String = fixture.compiler(emitterFactory).fold(
        { error("Failed to compile fixture: $it") },
        { it },
    )
}
