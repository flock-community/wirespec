package community.flock.wirespec.plugin.gradle

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Avro
import community.flock.wirespec.compiler.core.emit.common.FileExtension.JSON
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.convert
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.DirectoryPath
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.Source.Type.JSON
import community.flock.wirespec.plugin.io.getFullPath
import community.flock.wirespec.plugin.io.getOutPutPath
import community.flock.wirespec.plugin.io.or
import community.flock.wirespec.plugin.io.read
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class ConvertWirespecTask : BaseWirespecTask() {

    @get:InputFile
    @get:Option(option = "input", description = "input directory")
    abstract val input: RegularFileProperty

    @get:Input
    @get:Option(option = "format", description = "formats list")
    abstract val format: Property<Format>

    @get:Input
    @get:Option(option = "preProcessor", description = "pre-processor")
    abstract val preProcessor: Property<(String) -> String>

    @TaskAction
    fun convert() {
        val preProcessorFunction = preProcessor.getOrElse({ it })
        val inputPath = getFullPath(input.get().asFile.absolutePath).or(::handleError)
        val outputPath = output.get().asFile.absolutePath
        val sources: Source<JSON> = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is ClassPath -> inputPath.readFromClasspath(preProcessorFunction)
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (inputPath.extension) {
                JSON -> Source(inputPath.name, preProcessorFunction(inputPath.read()))
                Avro -> Source(inputPath.name, preProcessorFunction(inputPath.read()))
                else -> throw JSONFileError()
            }
        }

        val outputDir = Directory(getOutPutPath(inputPath, outputPath).or(::handleError))
        ConverterArguments(
            format = format.get(),
            input = nonEmptySetOf(sources),
            emitters = emitters(),
            writer = writer(outputDir),
            error = { throw RuntimeException(it) },
            packageName = packageNameValue(),
            logger = wirespecLogger,
            shared = shared.getOrElse(true),
            strict = strict.getOrElse(false),
        ).let(::convert)
    }
}
