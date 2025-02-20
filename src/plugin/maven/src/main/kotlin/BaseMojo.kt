package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.plugin.FilesContent
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

abstract class BaseMojo : AbstractMojo() {

    @Parameter(required = true)
    protected lateinit var input: String

    @Parameter(required = true)
    protected lateinit var output: String

    @Parameter
    protected var packageName: String = DEFAULT_GENERATED_PACKAGE_STRING

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    protected lateinit var project: MavenProject

    protected val logger = object : Logger(ERROR) {
        override fun debug(string: String) = log.debug(string)
        override fun info(string: String) = log.info(string)
        override fun warn(string: String) = log.warn(string)
        override fun error(string: String) = log.error(string)
    }

    protected fun getFilesContent(): FilesContent = File(input)
        .let {
            if (it.isDirectory) {
                it.listFiles() ?: arrayOf<File>()
            } else {
                arrayOf(it)
            }
        }
        .map { it.name.split(".").first() to it }
        .map { (name, reader) -> name to reader.readText(Charsets.UTF_8) }
}
