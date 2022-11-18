package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.plugin.maven.utils.JvmUtil
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

@Mojo(name = "java", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class JavaMojo : WirespecMojo() {

    @Parameter(required = true)
    private lateinit var sourceDirectory: String

    @Parameter(required = true)
    private lateinit var targetDirectory: String

    @Parameter
    private var packageName: String = DEFAULT_PACKAGE_NAME

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    private val emitter = JavaEmitter(packageName, logger)

    override fun execute() {
        compile(sourceDirectory, logger, emitter)
            .forEach { (name, result) -> JvmUtil.emitJvm(packageName, targetDirectory, name, "java").writeText(result) }
    }
}
