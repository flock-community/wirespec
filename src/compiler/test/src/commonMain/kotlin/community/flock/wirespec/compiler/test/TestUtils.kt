package community.flock.wirespec.compiler.test

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.utils.NoLogger

fun compile(source: String) = { emitter: () -> Emitter ->
    object : CompilationContext, NoLogger {
        override val spec = WirespecSpec
        override val emitters = nonEmptySetOf(emitter())
    }.compile(nonEmptyListOf(ModuleContent("N/A", source)))
        .map { emitted -> emitted.filter { !it.file.contains("Wirespec") } }
        .map { it.joinToString("\n") { it.result } }
        .onLeft(::println)
}
