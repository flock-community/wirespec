package community.flock.wirespec.plugin.cli

import com.github.ajalt.clikt.core.main
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.convert

fun main(args: Array<String>) {
    // Filter platform-specific arguments.
    (0 until 20)
        .mapNotNull(args::orNull)
        .let { WirespecCli(::compile, ::convert).main(it) }
}
