package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name = "typescript", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class TypescriptMojo : WirespecMojo() {

    @Parameter(required = true)
    private lateinit var input: String

    @Parameter(required = true)
    private lateinit var output: String

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    override fun execute() {
        val emitter = TypeScriptEmitter(logger)
        File(output).mkdirs()
        compile(input, logger, emitter)
            .forEach { (name, result) ->
                File("$output/$name.ts").writeText(result)
            }
    }

}
