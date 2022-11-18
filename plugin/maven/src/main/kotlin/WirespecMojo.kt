package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import org.apache.maven.plugin.AbstractMojo
import java.io.BufferedReader
import java.io.File

abstract class WirespecMojo : AbstractMojo() {

    val logger = object : Logger(true) {
        override fun warn(s: String) = log.warn(s)
        override fun log(s: String) = log.info(s)
    }

    fun compile(sourceDirectory: String, logger: Logger, emitter: Emitter) =
        (File(sourceDirectory).listFiles() ?: arrayOf<File>())
            .map { it.name.split(".").first() to it.bufferedReader(Charsets.UTF_8) }
            .map { (name, reader) -> name to WireSpec.compile(reader.collectToString())(logger)(emitter) }
            .map { (name, result) ->
                name to when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> error("compile error")
                }
            }
            .flatMap { (name, result) ->
                if (!emitter.split) {
                    listOf(name to result.first().second)
                } else {
                    result
                }
            }

    private fun BufferedReader.collectToString() =
        lines().collect(java.util.stream.Collectors.joining())
}
