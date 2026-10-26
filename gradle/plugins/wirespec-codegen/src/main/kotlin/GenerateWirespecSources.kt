import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/** Runs the Wirespec CLI from this build once per language and per conversion. */
abstract class GenerateWirespecSources @Inject constructor(private val exec: ExecOperations) : DefaultTask() {

    @get:Classpath
    abstract val cliClasspath: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: DirectoryProperty

    @get:Input
    abstract val conversions: MapProperty<String, String>

    @get:Input
    abstract val basePackage: Property<String>

    @get:Input
    abstract val languages: ListProperty<String>

    @get:Input
    abstract val irExtensions: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        outputDir.get().asFile.run { deleteRecursively(); mkdirs() }
        languages.get().forEach { language ->
            val emit = listOf(
                "--language", language,
                "--package", "${basePackage.get()}.${language.lowercase()}.generated",
                "--output", outputDir.dir(language.lowercase()).get().asFile.path,
            ) + irExtensions.get().flatMap { listOf("--extension", it) }
            // Each run writes its own aggregate files (e.g. `Client`) at the same path; the compiled sources win.
            conversions.get().forEach { (file, format) ->
                wirespec(listOf("convert", format, "--input", sources.file(file).get().asFile.path) + emit)
            }
            wirespec(listOf("compile", "--input", sources.get().asFile.path) + emit)
        }
    }

    private fun wirespec(arguments: List<String>) {
        exec.javaexec {
            classpath = cliClasspath
            mainClass.set("community.flock.wirespec.plugin.cli.MainKt")
            args(arguments + listOf("--log-level", "WARN"))
        }
    }
}
