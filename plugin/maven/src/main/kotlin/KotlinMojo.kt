package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.openapi.OpenApiParser
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

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    override fun execute() {
        val emitter = KotlinEmitter(packageName, logger)
        if(input.endsWith(".json")){
            val fileName = input.split("/")
                .last()
                .substringBeforeLast(".")
                .replaceFirstChar(Char::uppercase)
            val json = File(input).readText()
            val ast = OpenApiParser.parse(json)
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
