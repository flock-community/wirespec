package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.FullPath
import community.flock.wirespec.plugin.files.Name
import community.flock.wirespec.plugin.files.Source
import community.flock.wirespec.plugin.files.SourcePath
import community.flock.wirespec.plugin.files.path
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.options.Option
import java.io.File

abstract class BaseWirespecTask : DefaultTask() {

    @get:InputDirectory
    @get:Option(option = "input", description = "input directory")
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    @get:Option(option = "output", description = "output directory")
    abstract val output: DirectoryProperty

    @get:Input
    @get:Optional
    @get:Option(option = "languages", description = "languages list")
    abstract val languages: ListProperty<Language>

    @get:Input
    @get:Optional
    @get:Option(option = "shared", description = "emit shared code")
    abstract val shared: Property<Boolean>

    @get:Input
    @get:Optional
    @get:Option(option = "emitterClass", description = "custom emitter class")
    abstract val emitterClass: Property<Class<*>>

    @get:Input
    @get:Optional
    @get:Option(option = "packageName", description = "package name")
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "strict", description = "strict parsing mode")
    abstract val strict: Property<Boolean>

    @Internal
    val wirespecLogger = object : Logger(ERROR) {
        override fun debug(string: String) = logger.debug(string)
        override fun info(string: String) = logger.info(string)
        override fun warn(string: String) = logger.warn(string)
        override fun error(string: String) = logger.error(string)
    }

    protected fun getFullPath(input: String?, createIfNotExists: Boolean = false) = when {
        input == null -> null
        input.startsWith("classpath:") -> SourcePath(input.substringAfter("classpath:"))
        else -> {
            val file = File(input).createIfNotExists(createIfNotExists)
            when {
                file.isDirectory -> DirectoryPath(file.absolutePath)
                file.isFile -> FilePath(file.absolutePath)
                else -> throw IsNotAFileOrDirectory(input)
            }
        }
    }

    fun getOutPutPath(inputPath: FullPath) = when (val it = getFullPath(output.get().asFile.absolutePath, true)) {
        null -> DirectoryPath("${inputPath.path()}/out")
        is DirectoryPath -> it
        is FilePath, is SourcePath -> throw OutputShouldBeADirectory()
    }

    inline fun <reified E : Source.Type> SourcePath.readFromClasspath(): Source<E> {
        val file = File(value)
        val classLoader = javaClass.classLoader
        val inputStream =
            classLoader.getResourceAsStream(value) ?: error("Could not find file: $value on the classpath.")
        val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val name = file.name.split(".").first()
        return Source<E>(name = Name(name), content = content)
    }
}
