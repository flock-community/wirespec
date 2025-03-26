package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import community.flock.wirespec.plugin.Format
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
        val packageNameValue = packageName.map { PackageName(it) }.get()
        val fileName = input.get().asFile.name.split("/")
            .last()
            .substringBeforeLast(".")
            .firstToUpper()

        val json = input.asFile.get().readText()

        val ast = when (format.get()) {
            Format.OpenAPIV2 -> OpenAPIV2Parser.parse(json, strict.getOrElse(false))
            Format.OpenAPIV3 -> OpenAPIV3Parser.parse(json, strict.getOrElse(false))
            Format.Avro -> AvroParser.parse(json)
        }

        languages.get()
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
