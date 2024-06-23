package community.flock.wirespec.plugin.maven

import arrow.core.Either
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.PackageName
import java.io.BufferedReader
import java.io.File
import java.util.stream.Collectors
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

typealias FilesContent = List<Pair<String, String>>

abstract class BaseMojo : AbstractMojo() {

    @Parameter(required = true)
    protected lateinit var input: String

    @Parameter(required = true)
    protected lateinit var output: String

    @Parameter
    protected var packageName: String = DEFAULT_PACKAGE_STRING

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    protected lateinit var project: MavenProject

    val outputFile = File(output)
    val packageNameValue = PackageName(packageName)

    protected val logger = object : Logger() {
        override fun warn(s: String) = log.warn(s)
        override fun log(s: String) = log.info(s)
    }

    protected fun getFilesContent(): FilesContent = (File(input).listFiles() ?: arrayOf<File>())
        .map { it.name.split(".").first() to it }
        .map { (name, reader) -> name to reader.readText(Charsets.UTF_8) }


}
