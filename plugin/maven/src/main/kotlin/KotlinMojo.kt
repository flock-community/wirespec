package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.openapi.v2.OpenApiParser as OpenApiParserV2
import community.flock.wirespec.openapi.v3.OpenApiParser as OpenApiParserV3
import community.flock.wirespec.plugin.maven.utils.JvmUtil
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name = "kotlin", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class KotlinMojo : WirespecMojo() {

    @Parameter(required = true)
    private lateinit var input: String

    @Parameter(required = true)
    private lateinit var output: String

    @Parameter
    private var packageName: String = DEFAULT_PACKAGE_NAME

    @Parameter
    private var openapi: String? = null

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    override fun execute() {
        val emitter = KotlinEmitter(packageName, logger)
        if (openapi != null) {
            val fileName = input.split("/")
                .last()
                .substringBeforeLast(".")
                .replaceFirstChar(Char::uppercase)
            val json = File(input).readText()
            val ast = when(openapi){
                "v2" -> OpenApiParserV2.parse(json)
                "v3" -> OpenApiParserV3.parse(json)
                else -> error("Api version not found")
            }
            val result = emitter.emit(ast).joinToString("\n"){ it.second }
            JvmUtil.emitJvm(packageName, output, fileName, "kt").writeText(result)
        } else {
            compile(input, logger, emitter)
                .forEach { (name, result) ->
                    JvmUtil.emitJvm(packageName, output, name, "kt").writeText(result)
                }
        }
    }
}
