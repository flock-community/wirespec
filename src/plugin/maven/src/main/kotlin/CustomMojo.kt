package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.shared.JavaShared
import community.flock.wirespec.compiler.core.emit.shared.KotlinShared
import community.flock.wirespec.compiler.core.emit.shared.ScalaShared
import community.flock.wirespec.compiler.core.emit.shared.TypeScriptShared
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.validate.validate
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Parser
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.parse
import community.flock.wirespec.plugin.toDirectory
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(
    name = "custom",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
)
class CustomMojo : BaseMojo() {

    @Parameter(required = true)
    private lateinit var emitterClass: String

    @Parameter(required = true)
    private lateinit var extension: String

    @Parameter(required = false)
    private var split: Boolean = false

    @Parameter
    private var shared: Language? = null

    @Parameter
    private var format: Format? = null

    @Parameter
    private var strict: Boolean = true

    override fun execute() {
        project.addCompileSourceRoot(output)
        val ext = extension
        val emitterPkg = PackageName(packageName)

        val emitter = try {
            val clazz = getClassLoader(project).loadClass(emitterClass)
            val constructor = clazz.constructors.first()
            val args = constructor.parameters
                .map {
                    when (it.type) {
                        String::class.java -> packageName
                        Boolean::class.java -> split
                        Logger::class.java -> logger
                        else -> error("Cannot map constructor parameter")
                    }
                }
            constructor.newInstance(*args.toTypedArray()) as Emitter
        } catch (e: Exception) {
            throw e.also { it.printStackTrace() }
        }
        shared?.let {
            when (it) {
                Language.Java -> JavaShared
                Language.Kotlin -> KotlinShared
                Language.Scala -> ScalaShared
                Language.TypeScript -> TypeScriptShared
                Language.Wirespec -> null
                Language.OpenAPIV2 -> null
                Language.OpenAPIV3 -> null
            }
        }?.also {
            File(output).resolve(PackageName(it.packageString).toDirectory()).apply {
                mkdirs()
                resolve("Wirespec.$ext").writeText(it.source)
            }
        }

        val fileContents = getFilesContent()
        val ast: List<Pair<String, AST>> =
            when (format) {
                Format.OpenApiV2 -> fileContents.map { it.first to OpenApiV2Parser.parse(it.second, !strict).validate() }
                Format.OpenApiV3 -> fileContents.map { it.first to OpenApiV3Parser.parse(it.second, !strict).validate() }
                Format.Avro -> fileContents.map { it.first to AvroParser.parse(it.second, !strict).validate() }
                null -> fileContents.parse(logger)
            }

        ast.map { (name, ast) -> name to ast.validate().let { emitter.emit(it) } }
            .flatMap { (name, results) ->
                if (emitter.split) {
                    results
                } else {
                    listOf(Emitted(name, results.first().result))
                }
            }
            .also { File(output).resolve(emitterPkg.toDirectory()).mkdirs() }
            .forEach { (name, result) ->
                File(output).resolve(emitterPkg.toDirectory()).resolve("$name.$extension").writeText(result)
            }
    }

    private fun getClassLoader(project: MavenProject): ClassLoader {
        try {
            val classpathElements = project.compileClasspathElements.apply {
                add(project.build.outputDirectory)
                add(project.build.testOutputDirectory)
            }
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
