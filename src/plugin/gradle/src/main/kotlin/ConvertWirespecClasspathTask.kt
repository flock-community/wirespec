package community.flock.wirespec.plugin.gradle

import arrow.core.nonEmptySetOf
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.convert
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.Source.Type.JSON
import community.flock.wirespec.plugin.io.getOutPutPath
import community.flock.wirespec.plugin.io.or
import community.flock.wirespec.plugin.io.readSource
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Extension classes and the pre-processor function are only tracked by class name, not by implementation")
public abstract class ConvertWirespecClasspathTask : BaseWirespecTask() {

    @get:Input
    @get:Option(option = "classpathInput", description = "input file on the classpath")
    public abstract val classpathInput: Property<String>

    @get:Classpath
    public abstract val classpath: ConfigurableFileCollection

    @get:Input
    @get:Option(option = "format", description = "formats list")
    public abstract val format: Property<Format>

    @get:Input
    @get:Optional
    public abstract val preProcessor: Property<(String) -> String>

    @TaskAction
    public fun convert() {
        val inputPath = ClassPath(classpathInput.get())
        val source = inputPath.readSource<JSON>(classpath.files)
            ?.map(preProcessor.getOrElse { it })
            ?.also { logger.info("Found 1 file to process on the classpath: $inputPath") }
            ?: throw NotOnClasspath(inputPath)

        val outputDir = Directory(getOutPutPath(inputPath, output.get().asFile.absolutePath).or(::handleError))
        val testOutputDir = testOutput.orNull?.asFile?.absolutePath?.let { Directory(getOutPutPath(inputPath, it).or(::handleError)) }
        ConverterArguments(
            format = format.get(),
            input = nonEmptySetOf(source),
            emitter = emitter(),
            writer = writer(outputDir, testOutputDir),
            error = { throw RuntimeException(it) },
            packageName = packageNameValue(),
            logger = wirespecLogger,
            shared = shared.getOrElse(true),
            strict = strict.getOrElse(false),
            ignoreDefaults = ignoreDefaults.getOrElse(false),
        ).let(::convert)
    }
}
