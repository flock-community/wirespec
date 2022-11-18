package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.plugin.maven.utils.JvmUtil.emitJvm
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

@Mojo(name = "scala", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class ScalaMojo : WirespecMojo() {

    @Parameter(required = true)
    private lateinit var sourceDirectory: String

    @Parameter(required = true)
    private lateinit var targetDirectory: String

    @Parameter
    private var packageName: String = DEFAULT_PACKAGE_NAME

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    private val emitter = ScalaEmitter(packageName, logger)

    override fun execute() {
        compile(sourceDirectory, logger, emitter)
            .forEach { (name, result) -> emitJvm(packageName, targetDirectory, name, "scala").writeText(result) }
    }
}
