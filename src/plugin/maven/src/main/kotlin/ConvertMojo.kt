package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Format.OpenApiV2
import community.flock.wirespec.plugin.Format.OpenApiV3
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
        val fileName = input.split("/")
            .last()
            .substringBeforeLast(".")
            .firstToUpper()

        val json = File(input).readText()
        val ast = when (format) {
            OpenApiV2 -> OpenApiV2Parser.parse(json, !strict)
            OpenApiV3 -> community.flock.wirespec.openapi.v3.OpenApiV3Parser.parse(json, !strict)
        }

        languages
            ?.map { it.mapEmitter(packageNameValue, logger) }
            ?.forEach { (emitter, sharedData, ext) ->
                emitter.emit(ast).forEach {
                    it.writeToFiles(
                        outputFile,
                        packageNameValue,
                        if (shared) sharedData else null,
                        fileName,
                        ext
                    )
                }
            }


    }


}
