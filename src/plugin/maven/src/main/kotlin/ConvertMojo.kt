package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.validate.validate
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Parser
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.PackageName
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
        val outputDirectory = File(output)
        val packageNameValue = PackageName(packageName)

        val fileContents = getFilesContent()
        val asts = when (format) {
            Format.OpenApiV2 -> fileContents.map { it.first to OpenApiV2Parser.parse(it.second, !strict).validate() }
            Format.OpenApiV3 -> fileContents.map { it.first to OpenApiV3Parser.parse(it.second, !strict).validate() }
            Format.Avro -> fileContents.map { it.first to AvroParser.parse(it.second).validate() }
        }

        emit(packageNameValue, asts, outputDirectory)
    }
}
