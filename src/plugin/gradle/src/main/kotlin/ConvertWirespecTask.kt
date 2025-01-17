package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Parser
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.mapEmitter
import community.flock.wirespec.plugin.writeToFiles
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class ConvertWirespecTask : BaseWirespecTask() {

    @get:InputFile
    @get:Option(option = "input", description = "input directory")
    abstract val input: RegularFileProperty

    @get:Input
    @get:Option(option = "languages", description = "languages list")
    abstract val format: Property<Format>

    @get:Input
    @get:Option(option = "languages", description = "languages list")
    abstract val languages: ListProperty<Language>

    @get:Optional
    @get:Input
    @get:Option(option = "shared", description = "emit shared class")
    abstract val shared: Property<Boolean>

    @get:Optional
    @get:Input
    @get:Option(option = "shared", description = "emit shared class")
    abstract val strict: Property<Boolean>

    @TaskAction
    fun convert() {
        val packageNameValue = packageName.map { PackageName(it) }.get()
        val fileName = input.get().asFile.name.split("/")
            .last()
            .substringBeforeLast(".")
            .firstToUpper()

        val json = input.asFile.get().readText()

        val ast = when (format.get()) {
            Format.OpenApiV2 -> OpenApiV2Parser.parse(json, strict.getOrElse(false))
            Format.OpenApiV3 -> OpenApiV3Parser.parse(json, strict.getOrElse(false))
            Format.Avro -> AvroParser.parse(json)
        }

        languages.get()
            .map { it.mapEmitter(packageNameValue, wirespecLogger) }
            .forEach { (emitter, ext, sharedData) ->
                emitter.emit(ast).forEach {
                    it.writeToFiles(
                        output = output.asFile.get(),
                        packageName = packageNameValue,
                        shared = if (shared.getOrElse(true)) sharedData else null,
                        fileName = if (emitter.split) null else fileName,
                        ext = ext,
                    )
                }
            }
    }
}
