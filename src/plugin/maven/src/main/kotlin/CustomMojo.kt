package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.compile
import java.io.File
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject

@Mojo(
    name = "custom",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE
)
class CustomMojo : BaseMojo() {

    @Parameter(required = true)
    private lateinit var emitterClass: String

    @Parameter(required = true)
    private lateinit var extension: String

    @Parameter(required = false)
    private var split: Boolean = false

    override fun execute() {
        val emitter = try {
            val clazz = getClassLoader(project).loadClass(emitterClass)
            val constructor = clazz.getConstructor(Logger::class.java, Boolean::class.java)
            constructor.newInstance(logger, split) as Emitter
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        getFilesContent().compile(logger, emitter)
            .also { File(output).mkdirs() }
            .forEach { (name, result) -> File(output).resolve("$name.$extension").writeText(result) }
    }

    private fun getClassLoader(project: MavenProject): ClassLoader {
        try {
            val classpathElements = project.compileClasspathElements
            classpathElements.add(project.build.outputDirectory)
            classpathElements.add(project.build.testOutputDirectory)
            val urls = classpathElements.indices
                .map { File(classpathElements[it] as String).toURI().toURL() }
                .toTypedArray()
            return java.net.URLClassLoader(urls, javaClass.getClassLoader())
        } catch (e: Exception) {
            log.debug("Couldn't get the classloader.")
            return javaClass.getClassLoader()
        }
    }
}
