package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.validate.validate
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Parser
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.mapEmitter
import community.flock.wirespec.plugin.writeToFiles
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(name = "convert", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class ConvertMojo : CompileMojo() {

    @Parameter(required = true)
    private lateinit var format: Format

    @Parameter
    private var strict: Boolean = true

    override fun execute() {
        project.addCompileSourceRoot(output)
        val outputFile = File(output)
        val packageNameValue = PackageName(packageName)
        val fileName = input.split("/")
            .last()
            .substringBeforeLast(".")
            .firstToUpper()

        val json = File(input).readText()
        val ast = when (format) {
            Format.OpenApiV2 -> OpenApiV2Parser.parse(json, !strict).validate()
            Format.OpenApiV3 -> OpenApiV3Parser.parse(json, !strict).validate()
            Format.Avro -> AvroParser.parse(json).validate()
        }

        languages
            ?.map { it.mapEmitter(packageNameValue, logger) }
            ?.forEach { (emitter, ext, sharedData) ->
                emitter.emit(ast).forEach {
                    it.writeToFiles(
                        output = outputFile,
                        packageName = packageNameValue,
                        shared = if (shared) sharedData else null,
                        fileName = if (emitter.split) null else fileName,
                        ext = ext,
                    )
                }
            }
    }
}
