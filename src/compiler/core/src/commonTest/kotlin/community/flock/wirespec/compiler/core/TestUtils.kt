package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.noLogger

fun compile(source: String) = { emitter: Emitter ->
    WirespecSpec.compile(source)(noLogger)(emitter)
        .map { it.first().result }
        .onLeft(::println)
}
