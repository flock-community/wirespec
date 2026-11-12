package community.flock.wirespec.plugin.gradle

import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.DirectoryPath
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.getOutPutPath
import community.flock.wirespec.plugin.io.or
import community.flock.wirespec.plugin.io.read
import community.flock.wirespec.plugin.io.wirespecSources
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Extension classes are only tracked by class name, not by implementation")
public abstract class CompileWirespecTask : BaseWirespecTask() {

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Option(option = "input", description = "input directory")
    public abstract val input: DirectoryProperty

    @TaskAction
    public fun action() {
        val inputPath = inputPath(input.orNull?.asFile)
        val outputPath = output.get().asFile.absolutePath
        val testOutputPath = testOutput.orNull?.asFile?.absolutePath
        val sources = when (inputPath) {
            is ClassPath -> inputPath.wirespecSources(classpath.files)
                .also { logger.info("Found ${it.size} wirespec file(s) to process on the classpath: $inputPath") }
                .toNonEmptySetOrNull()
                ?: throw NotOnClasspath(inputPath)
            is DirectoryPath -> Directory(inputPath).wirespecSources(wirespecLogger).or(::handleError)
            is FilePath -> when (inputPath.extension) {
                FileExtension.Wirespec -> nonEmptySetOf<Source<Source.Type.Wirespec>>(Source(inputPath.name, inputPath.read()))
                    .also { logger.info("Found 1 wirespec file to process: $inputPath") }
                else -> throw WirespecFileError()
            }
        }

        val outputDir = Directory(getOutPutPath(inputPath, outputPath).or(::handleError))
        val testOutputDir = testOutputPath?.let { Directory(getOutPutPath(inputPath, it).or(::handleError)) }
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
