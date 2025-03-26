package community.flock.wirespec.plugin.gradle

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Avro
import community.flock.wirespec.compiler.core.emit.common.FileExtension.JSON
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.Source
import community.flock.wirespec.plugin.files.Source.Type.JSON
import community.flock.wirespec.plugin.files.SourcePath
import community.flock.wirespec.plugin.mapEmitter
import community.flock.wirespec.plugin.writeToFiles
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
        val packageNameValue = packageName.getOrElse(DEFAULT_GENERATED_PACKAGE_STRING).let(PackageName::invoke)
        val file = input.get().asFile
        val fileName = file.name.split("/")
            .last()
            .substringBeforeLast(".")
            .firstToUpper()

        val json = file.readText()

        val ast = when (format.get()) {
            Format.OpenAPIV2 -> OpenAPIV2Parser.parse(json, strict.getOrElse(false))
            Format.OpenAPIV3 -> OpenAPIV3Parser.parse(json, strict.getOrElse(false))
            Format.Avro -> AvroParser.parse(json)
        }

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
            emitters = nonEmptySetOf(WirespecEmitter()), // TODO("Change to emitters")
            writer = { filePath, string -> filePath.write(string) },
            error = { throw RuntimeException(it) },
            packageName = packageNameValue,
            logger = wirespecLogger,
            shared = shared.getOrElse(true),
            strict = strict.getOrElse(false),
        )

        languages.getOrElse(emptyList())
            .map { it.mapEmitter(packageNameValue) }
            .forEach { (emitter, ext, sharedData) ->
                emitter.emit(ast, wirespecLogger).writeToFiles(
                    output = output.asFile.get(),
                    packageName = packageNameValue,
                    shared = if (shared.getOrElse(true)) sharedData else null,
                    fileName = if (emitter.split) null else fileName,
                    ext = ext,
                )
            }
    }
}
