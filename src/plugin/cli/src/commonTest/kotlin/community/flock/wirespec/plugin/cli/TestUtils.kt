package community.flock.wirespec.plugin.cli

import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.ConverterArguments

fun noopCompiler(block: (CompilerArguments) -> Unit): (CompilerArguments) -> Unit = {
    block(it)
}

fun noopConverter(block: (ConverterArguments) -> Unit): (ConverterArguments) -> Unit = {
    block(it)
}

fun noopCli() = WirespecCli.provide(noopCompiler { }, noopConverter { })
