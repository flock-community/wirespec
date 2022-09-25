package community.flock.wirespec.plugin.maven.shared

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import java.io.BufferedReader
import java.io.File

object Shared {

    fun compile(sourceDirectory: String, logger: Logger, emitter: Emitter) = File(sourceDirectory).listFiles()
        .let { it ?: arrayOf<File>() }
        .map { it.name.split(".").first() to it.bufferedReader(Charsets.UTF_8) }
        .map { (name, reader) -> name to WireSpec.compile(reader.collectToString())(logger)(emitter) }
        .map { (name, result) ->
            name to when (result) {
                is Either.Right -> result.value
                else -> error("Cannot compile")
            }
        }

    private fun BufferedReader.collectToString() =
        lines().collect(java.util.stream.Collectors.joining())
}
