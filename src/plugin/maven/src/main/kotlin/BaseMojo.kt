package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.FilesContent
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

    protected val logger = object : Logger() {
        override fun warn(s: String) = log.warn(s)
        override fun info(s: String) = log.info(s)
        override fun debug(s: String) = log.debug(s)
    }

    protected fun getFilesContent(): FilesContent = (File(input).listFiles() ?: arrayOf<File>())
        .map { it.name.split(".").first() to it }
        .map { (name, reader) -> name to reader.readText(Charsets.UTF_8) }
}
