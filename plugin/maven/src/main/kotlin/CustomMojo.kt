package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.plugin.maven.utils.JvmUtil.emitJvm
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.net.URLClassLoader

import community.flock.wirespec.compiler.utils.Logger
import org.apache.maven.plugin.AbstractMojo

import java.io.File

@Mojo(name = "custom", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
class CustomMojo : BaseMojo() {

    @Parameter(required = true)
    private lateinit var input: String

    @Parameter(required = true)
    private lateinit var output: String

    @Parameter
    private var packageName: String = DEFAULT_PACKAGE_NAME

    @Parameter(required = true)
    private lateinit var emitterClass: String

    @Parameter(required = true)
    private lateinit var extention: String

    @Parameter(required = false)
    private var split: Boolean = false

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject


    override fun execute() {
        val emitter = try {
            val clazz = getClassLoader(project).loadClass(emitterClass)
            val constructor = clazz.getConstructor(Logger::class.java, Boolean::class.java)
            val instance = constructor.newInstance(logger, split)
            instance as Emitter
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        compile(input, logger, emitter)
            .also { File(output).mkdirs() }
            .forEach { (name, result) ->
                File("$output/$name.$extention").writeText(result)
            }
    }

    fun getClassLoader(project: org.apache.maven.project.MavenProject): java.lang.ClassLoader {
        try {
            var classpathElements = project.getCompileClasspathElements()
            classpathElements.add(project.getBuild().getOutputDirectory())
            classpathElements.add(project.getBuild().getTestOutputDirectory())
            var urls = classpathElements.indices.map{i ->
                java.io.File(classpathElements.get(i) as kotlin.String).toURL()
            }
                .toTypedArray()
            return java.net.URLClassLoader(urls, this.javaClass.getClassLoader())
        } catch (e: java.lang.Exception) {
            getLog().debug("Couldn't get the classloader.")
            return this.javaClass.getClassLoader()
        }
    }
}
