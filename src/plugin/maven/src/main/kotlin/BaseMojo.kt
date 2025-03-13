package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.plugin.FileContent
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.FullPath
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

abstract class BaseMojo : AbstractMojo() {

    /**
     * Specifies the input files or directories.
     * Multiple paths can be provided, separated by commas.
     * Files can also be loaded from the classpath using the 'classpath:' prefix (e.g., 'classpath:wirespec/petstore.ws').
     */

    @Parameter(required = true)
    protected lateinit var input: String

    /**
     * Specifies the output directories to process.
     */
    @Parameter(required = true)
    protected lateinit var output: String

    /**
     * Specifies package name default 'community.flock.wirespec.generated'
     */
    @Parameter
    protected var packageName: String = DEFAULT_GENERATED_PACKAGE_STRING

    @Parameter
    protected var strict: Boolean = true

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    protected lateinit var project: MavenProject

    protected val logger = object : Logger(ERROR) {
        override fun debug(string: String) = log.debug(string)
        override fun info(string: String) = log.info(string)
        override fun warn(string: String) = log.warn(string)
        override fun error(string: String) = log.error(string)
    }

    fun getFullPath(input: String?, createIfNotExists: Boolean = false): FullPath? = input?.let {
        val file = File(it).createIfNotExists(createIfNotExists)
        when {
            file.isDirectory -> DirectoryPath(file.absolutePath)
            else -> FilePath(file.absolutePath)
        }
    }

    protected fun getFilesContent() = input.split(",")
        .flatMap {
            when {
                it.startsWith("classpath:") -> readFromClasspath(it.substringAfter("classpath:"))
                else -> readFromFile(it)
            }
        }.map(::FileContent)

    private fun readFromClasspath(input: String): List<Pair<String, String>> = input
        .let {
            val file = File(it)
            val classLoader = javaClass.classLoader
            val inputStream = classLoader.getResourceAsStream(input)
                ?: error("Could not find file: $it on the classpath.")
            val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val name = file.name.split(".").first()
            return listOf(name to content)
        }

    private fun readFromFile(input: String) = File(input)
        .let {
            when {
                it.isDirectory -> it.listFiles().orEmpty().toList()
                else -> listOf(it)
            }
        }
        .map { it.name.split(".").first() to it }
        .map { (name, reader) -> name to reader.readText(Charsets.UTF_8) }
}
