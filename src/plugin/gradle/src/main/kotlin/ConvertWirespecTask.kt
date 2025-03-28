package community.flock.wirespec.plugin.gradle

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Avro
import community.flock.wirespec.compiler.core.emit.common.FileExtension.JSON
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.convert
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.Source
import community.flock.wirespec.plugin.files.Source.Type.JSON
import community.flock.wirespec.plugin.files.SourcePath
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class ConvertWirespecTask : BaseWirespecTask() {

    @get:Input
    @get:Option(option = "format", description = "formats list")
    abstract val format: Property<Format>

    @TaskAction
    fun convert() {
        val inputPath = getFullPath(input.get().asFile.absolutePath)
        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> inputPath.readFromClasspath()
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (inputPath.extension) {
                JSON -> Source<JSON>(inputPath.name, inputPath.read())
                Avro -> Source<JSON>(inputPath.name, inputPath.read())
                else -> throw JSONFileError()
            }
        }
        ConverterArguments(
            format = format.get(),
            input = nonEmptySetOf(sources),
            output = Directory(getOutPutPath(inputPath)),
            emitters = emitters(),
            writer = { filePath, string -> filePath.write(string) },
            error = { throw RuntimeException(it) },
            packageName = packageNameValue(),
            logger = wirespecLogger,
            shared = shared.getOrElse(true),
            strict = strict.getOrElse(false),
        ).let(::convert)
    }
}
