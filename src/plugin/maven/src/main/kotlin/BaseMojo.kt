package community.flock.wirespec.plugin.maven

import arrow.core.Either
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import org.apache.maven.plugin.AbstractMojo
import java.io.BufferedReader
import java.io.File

abstract class BaseMojo : AbstractMojo() {

    val logger = object : Logger() {
        override fun warn(s: String) = log.warn(s)
        override fun log(s: String) = log.info(s)
    }

    fun compile(input: String, logger: Logger, emitter: Emitter) =
        (File(input).listFiles() ?: arrayOf<File>())
            .map { it.name.split(".").first() to it.bufferedReader(Charsets.UTF_8) }
            .map { (name, reader) -> name to Wirespec.compile(reader.collectToString())(logger)(emitter) }
            .map { (name, result) ->
                name to when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> error("compile error")
                }
            }
            .flatMap { (name, result) ->
                if (!emitter.split) listOf(name to result.first().second)
                else result
            }

    private fun BufferedReader.collectToString() =
        lines().collect(java.util.stream.Collectors.joining())
}
