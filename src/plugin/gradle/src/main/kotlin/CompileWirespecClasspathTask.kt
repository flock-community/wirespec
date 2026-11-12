package community.flock.wirespec.plugin.gradle

import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.getOutPutPath
import community.flock.wirespec.plugin.io.or
import community.flock.wirespec.plugin.io.wirespecSources
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Extension classes are only tracked by class name, not by implementation")
public abstract class CompileWirespecClasspathTask : BaseWirespecTask() {

    @get:Input
    @get:Option(option = "classpathInput", description = "input file or folder on the classpath")
    public abstract val classpathInput: Property<String>

    @get:Classpath
    public abstract val classpath: ConfigurableFileCollection

    @TaskAction
    public fun action() {
        val inputPath = ClassPath(classpathInput.get())
        val sources = inputPath.wirespecSources(classpath.files)
            .also { logger.info("Found ${it.size} wirespec file(s) to process on the classpath: $inputPath") }
            .toNonEmptySetOrNull()
            ?: throw NotOnClasspath(inputPath)

        val outputDir = Directory(getOutPutPath(inputPath, output.get().asFile.absolutePath).or(::handleError))
        val testOutputDir = testOutput.orNull?.asFile?.absolutePath?.let { Directory(getOutPutPath(inputPath, it).or(::handleError)) }
        CompilerArguments(
            input = sources,
            emitter = emitter(),
            writer = writer(outputDir, testOutputDir),
            error = { throw RuntimeException(it) },
            packageName = packageNameValue(),
            logger = wirespecLogger,
            shared = shared.getOrElse(true),
            strict = strict.getOrElse(false),
            ignoreDefaults = ignoreDefaults.getOrElse(false),
        ).let(::compile)
    }
}
