package community.flock.wirespec.plugin.maven

import arrow.core.Either
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import java.io.BufferedReader
import java.io.File
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

abstract class BaseMojo : AbstractMojo() {

    @Parameter(required = true)
    protected lateinit var input: String

    @Parameter(required = true)
    protected lateinit var output: String

    @Parameter
    protected var packageName: String = DEFAULT_PACKAGE_STRING

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    protected lateinit var project: MavenProject

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
            .flatMap { (name, results) ->
                if (!emitter.split) listOf(Emitted(name, results.first().result))
                else results
            }

    private fun BufferedReader.collectToString() =
        lines().collect(java.util.stream.Collectors.joining())
}
