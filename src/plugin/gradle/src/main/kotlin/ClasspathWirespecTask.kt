package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.readFile
import community.flock.wirespec.plugin.io.wirespecSources
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Copying files is not worth caching")
public abstract class ClasspathWirespecTask : DefaultTask() {

    @get:Input
    @get:Option(option = "classpathInput", description = "file or folder on the classpath")
    public abstract val classpathInput: Property<String>

    @get:Classpath
    public abstract val classpath: ConfigurableFileCollection

    @get:OutputDirectory
    @get:Option(option = "output", description = "output directory")
    public abstract val output: DirectoryProperty

    @get:Internal
    public val outputFile: Provider<RegularFile>
        get() = output.file(classpathInput.map { it.fileName() })

    @TaskAction
    public fun extract() {
        val path = ClassPath(classpathInput.get())
        val directory = output.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }
        (path.readFile(classpath.files)?.let { mapOf(path.value.fileName() to it) } ?: path.wirespecFiles(classpath.files))
            .ifEmpty { throw NotOnClasspath(path) }
            .onEach { (name, content) -> directory.resolve(name).writeText(content) }
            .also { logger.info("Extracted ${it.size} file(s) from the classpath at $path to $directory") }
    }

    private fun ClassPath.wirespecFiles(classpath: Collection<File>) = wirespecSources(classpath)
        .groupBy({ "${it.name.value}.${FileExtension.Wirespec.value}" }, { it.content })
        .mapValues { (name, contents) -> contents.singleOrNull() ?: throw SameFileName(name, this) }
}

private fun String.fileName() = trim('/').substringAfterLast('/')
