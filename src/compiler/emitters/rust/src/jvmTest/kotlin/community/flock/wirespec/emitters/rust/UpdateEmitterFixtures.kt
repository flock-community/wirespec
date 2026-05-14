package community.flock.wirespec.emitters.rust

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.Fixture
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.ir.core.RawElement
import java.io.File

private const val LANGUAGE = "rust"

private val emitContext = object : EmitContext, NoLogger {
    override val emitters = nonEmptySetOf(RustIrEmitter())
}

private fun emitNode(node: Definition): String {
    val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(node))))
    return emitContext.emitters.first().emit(ast, emitContext.logger).first().result
}

private fun compile(fixture: Fixture): String = fixture.compiler { RustIrEmitter() }.fold(
    { error("Failed to compile fixture: $it") },
    { it },
)

private fun shared(): String = RustIrEmitter(emitShared = EmitShared(true)).emitShared()?.elements?.filterIsInstance<RawElement>()?.joinToString("") { it.code } ?: error("Shared emit returned null")

fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Usage: UpdateEmitterFixtures <fixturesRootDir>" }
    val root = File(args[0])
    root.mkdirs()

    val fixtures: Map<String, () -> String> = linkedMapOf(
        "compileFullEndpointTest" to { compile(CompileFullEndpointTest) },
        "compileChannelTest" to { compile(CompileChannelTest) },
        "compileEnumTest" to { compile(CompileEnumTest) },
        "compileMinimalEndpointTest" to { compile(CompileMinimalEndpointTest) },
        "compileRefinedTest" to { compile(CompileRefinedTest) },
        "compileUnionTest" to { compile(CompileUnionTest) },
        "compileTypeTest" to { compile(CompileTypeTest) },
        "compileNestedTypeTest" to { compile(CompileNestedTypeTest) },
        "compileComplexModelTest" to { compile(CompileComplexModelTest) },
        "sharedOutputTest" to { shared() },
    )

    fixtures.forEach { (name, produce) ->
        val target = root.resolve(name).resolve("$LANGUAGE.txt")
        target.parentFile.mkdirs()
        target.writeText(produce())
        println("[$LANGUAGE] wrote ${target.absolutePath}")
    }
}
