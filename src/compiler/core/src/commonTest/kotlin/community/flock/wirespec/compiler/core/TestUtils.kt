package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

fun compile(source: String) = { emitter: (Logger) -> Emitter ->
    object : CompilationContext {
        override val spec = WirespecSpec
        override val logger = noLogger
        override val emitter = emitter(logger)
    }.compile(source)
        .map { it.first().result }
        .onLeft(::println)
}
