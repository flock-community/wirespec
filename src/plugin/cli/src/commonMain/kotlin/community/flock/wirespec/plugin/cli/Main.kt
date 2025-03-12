package community.flock.wirespec.plugin.cli

import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.convert

fun main(args: Array<String>) {
    (0..20)
        .mapNotNull(args::orNull)
        .toTypedArray()
        .let(WirespecCli.provide(::compile, ::convert)::main)
}
