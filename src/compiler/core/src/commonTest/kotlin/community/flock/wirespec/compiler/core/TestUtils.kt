package community.flock.wirespec.compiler.core

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.NoLogger

fun compile(source: String) = { emitter: () -> Emitter ->
    object : CompilationContext, NoLogger {
        override val spec = WirespecSpec
        override val emitters = nonEmptySetOf(emitter())
    }.compile(nonEmptyListOf(ModuleContent("", source)))
        .map { it.first().result }
        .onLeft(::println)
}
