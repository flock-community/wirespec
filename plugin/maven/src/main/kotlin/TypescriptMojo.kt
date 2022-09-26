package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.utils.Logger
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name = "typescript", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class TypescriptMojo : WirespecMojo() {

    @Parameter(required = true)
    private lateinit var sourceDirectory: String

    @Parameter(required = true)
    private lateinit var targetDirectory: String

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject



    private val emitter = TypeScriptEmitter(logger)

    override fun execute() {
        File(targetDirectory).mkdirs()
        compile(sourceDirectory, logger, emitter)
            .forEach { (name, result) ->
                File("$targetDirectory/$name.ts").writeText(result)
            }
    }

}
