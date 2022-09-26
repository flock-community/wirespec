package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.utils.Logger
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name = "kotlin", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class KotlinMojo : WirespecMojo() {

    @Parameter(required = true)
    private lateinit var sourceDirectory: String

    @Parameter(required = true)
    private lateinit var targetDirectory: String

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    private val logger = object : Logger(true) {
        override fun warn(s: String) = log.warn(s)
        override fun log(s: String) = log.info(s)
    }

    private val emitter = KotlinEmitter(logger)

    override fun execute() {
        File(targetDirectory).mkdirs()
        compile(sourceDirectory, logger, emitter)
            .forEach { (name, result) ->
                File("$targetDirectory/$name.kt").writeText(result)
            }

    }

}
