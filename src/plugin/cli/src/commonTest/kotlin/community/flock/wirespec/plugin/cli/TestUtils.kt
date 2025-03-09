package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.cli.io.File

fun noopCompiler(block: (CompilerArguments) -> Unit): (CompilerArguments) -> WirespecResults = {
    block(it)
    emptyList()
}

fun noopConverter(block: (ConverterArguments) -> Unit): (ConverterArguments) -> WirespecResults = {
    block(it)
    emptyList()
}

val noopWriter: (File, List<Emitted>) -> Unit = { _, _ -> }

fun noopCli() = WirespecCli.provide(noopCompiler { }, noopConverter { }, noopWriter)
