package community.flock.wirespec.compiler.core

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.NoLogger

fun compile(source: String) = { emitter: () -> Emitter ->
    object : CompilationContext, NoLogger {
        override val spec = WirespecSpec
        override val emitter = emitter()
    }.compile(nonEmptyListOf(source))
        .map { it.first().result }
        .onLeft(::println)
}
