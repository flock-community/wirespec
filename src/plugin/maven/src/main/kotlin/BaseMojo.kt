package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
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

    protected fun getFilesContent() = input.split(",")
        .flatMap {
            when {
                it.startsWith("classpath:") -> readFromClasspath(it.substringAfter("classpath:"))
                else -> readFromFile(it)
            }
        }

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
            if (it.isDirectory) {
                it.listFiles()?.toList() ?: emptyList()
            } else {
                listOf(it)
            }
        }
        .map { it.name.split(".").first() to it }
        .map { (name, reader) -> name to reader.readText(Charsets.UTF_8) }
}
