package community.flock.wirespec.plugin.maven

import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
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
            Format.OpenAPIV2 -> fileContents.map { (name, content) -> name to OpenAPIV2Parser.parse(content, !strict) }
            Format.OpenAPIV3 -> fileContents.map { (name, content) -> name to OpenAPIV3Parser.parse(content, !strict) }
            Format.Avro -> fileContents.map { (name, content) -> name to AvroParser.parse(content) }
        }

        emit(packageNameValue, asts, outputDirectory)
    }
}
