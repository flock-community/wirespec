package community.flock.wirespec.compiler.testupdater

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.utils.NoLogger

fun runNodeFixture(emitter: Emitter, node: Definition): List<String> {
    val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(emitter)
    }
    val ast = AST(
        nonEmptyListOf(
            Module(
                FileUri(""),
                nonEmptyListOf(node),
            ),
        ),
    )
    return emitContext.emitters.map {
        it.emit(ast, emitContext.logger).first().result
    }
}

fun runCompile(source: String, emitter: Emitter): String {
    val context = object : CompilationContext, NoLogger {
        override val spec = WirespecSpec
        override val emitters = nonEmptySetOf(emitter)
    }
    return context.compile(nonEmptyListOf(ModuleContent(FileUri("N/A"), source)))
        .map { emitted -> emitted.filter { !it.file.contains("Wirespec") } }
        .map { it.joinToString("\n") { it.result } }
        .fold(
            { error("Compilation failed: $it") },
            { it }
        )
}
