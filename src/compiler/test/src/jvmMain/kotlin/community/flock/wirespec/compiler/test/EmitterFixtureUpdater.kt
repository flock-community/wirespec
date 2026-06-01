@file:JvmName("EmitterFixtureUpdater")

package community.flock.wirespec.compiler.test

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.ir.emit.IrEmitter
import java.io.File
import kotlin.reflect.full.primaryConstructor

/**
 * Single shared updater entry point used by every emitter's `updateEmitterFixtures`
 * Gradle task. The emitter's fully-qualified class name is passed as the second arg,
 * and we reflectively build a factory that supplies `EmitShared(true)` to its primary
 * constructor when applicable, so the generator can produce the shared file.
 *
 * Only fixtures whose `<name>.txt` already exists in the target directory are
 * regenerated. New fixtures must be seeded with an empty file first — this keeps each
 * emitter's set of fixtures explicit on disk instead of buried in code.
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "Usage: EmitterFixtureUpdater <fixturesDir> <emitterFqcn>" }
    val (fixturesDir, fqcn) = args
    val root = File(fixturesDir).apply { mkdirs() }
    val existing = root.listFiles { f -> f.isFile && f.extension == "txt" }
        ?.map { it.nameWithoutExtension }
        ?.toSet()
        .orEmpty()
    require(existing.isNotEmpty()) {
        "No existing fixtures in $fixturesDir — seed an empty <name>.txt before running."
    }

    val ctor = Class.forName(fqcn).kotlin.primaryConstructor
        ?: error("No primary constructor on $fqcn")
    val emitSharedParam = ctor.parameters.firstOrNull { it.type.classifier == EmitShared::class }

    val defaultFactory: () -> IrEmitter = { ctor.callBy(emptyMap()).asIrEmitter(fqcn) }
    val sharedFactory: () -> IrEmitter = {
        val args = emitSharedParam?.let { mapOf(it to EmitShared(value = true)) }.orEmpty()
        ctor.callBy(args).asIrEmitter(fqcn)
    }

    val allFixtures = nodeFixtures(defaultFactory) +
        compileFixtures(defaultFactory) +
        mapOf("sharedOutputTest" to { sharedFactory().sharedOutputAsString() })

    allFixtures
        .filterKeys { it in existing }
        .forEach { (name, produce) ->
            val target = root.resolve("$name.txt")
            target.writeText(produce())
            println("[fixtures] wrote ${target.absolutePath}")
        }
}

private fun Any.asIrEmitter(fqcn: String): IrEmitter {
    require(this is IrEmitter) { "$fqcn is not an IrEmitter" }
    return this
}

private fun IrEmitter.sharedOutputAsString(): String = emitShared()
    ?.let(generator::generate)
    ?: error("Shared emit returned null")

private fun compileFixtures(emitterFactory: () -> Emitter): Map<String, () -> String> = linkedMapOf(
    "compileFullEndpointTest" to { compile(CompileFullEndpointTest, emitterFactory) },
    "compileChannelTest" to { compile(CompileChannelTest, emitterFactory) },
    "compileRpcTest" to { compile(CompileRpcTest, emitterFactory) },
    "compileEnumTest" to { compile(CompileEnumTest, emitterFactory) },
    "compileMinimalEndpointTest" to { compile(CompileMinimalEndpointTest, emitterFactory) },
    "compileRefinedTest" to { compile(CompileRefinedTest, emitterFactory) },
    "compileUnionTest" to { compile(CompileUnionTest, emitterFactory) },
    "compileTypeTest" to { compile(CompileTypeTest, emitterFactory) },
    "compileNestedTypeTest" to { compile(CompileNestedTypeTest, emitterFactory) },
    "compileComplexModelTest" to { compile(CompileComplexModelTest, emitterFactory) },
)

private fun nodeFixtures(emitterFactory: () -> Emitter): Map<String, () -> String> {
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

private fun compile(fixture: Fixture, emitterFactory: () -> Emitter): String = fixture
    .compiler(emitterFactory)
    .fold({ error("Failed to compile fixture: $it") }, { it })
