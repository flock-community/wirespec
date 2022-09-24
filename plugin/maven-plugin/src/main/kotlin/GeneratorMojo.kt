package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.exceptions.WireSpecException
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.maven.Language.All
import community.flock.wirespec.plugin.maven.Language.Java
import community.flock.wirespec.plugin.maven.Language.Kotlin
import community.flock.wirespec.plugin.maven.Language.Scala
import community.flock.wirespec.plugin.maven.Language.TypeScript
import community.flock.wirespec.plugin.maven.generators.KotlinGenerator
import community.flock.wirespec.plugin.maven.generators.TypeScriptGenerator

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File
import java.util.stream.Collectors

typealias FileName = String
typealias Documents = List<Pair<FileName, (Emitter) -> Either<WireSpecException.CompilerException, String>>>

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class GeneratorMojo : AbstractMojo() {

    @Parameter
    private var language: Language = Kotlin

    @Parameter(required = true)
    private lateinit var sourceDirectory: String

    @Parameter(required = false)
    private var enableOpenApiAnnotations: Boolean = false

    @Parameter(required = true)
    private lateinit var targetDirectory: String

    @Parameter
    private var packageName: String? = null

    @Parameter
    private var scalarsKotlin: Map<String, String> = mapOf()

    @Parameter
    private var scalarsScala: Map<String, String> = mapOf()

    @Parameter
    private var scalarsJava: Map<String, String> = mapOf()

    @Parameter
    private var scalarsTypeScript: Map<String, String> = mapOf()

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    private val logger = object : Logger(true) {
        override fun warn(s: String) = this@GeneratorMojo.log.warn(s)
        override fun log(s: String) = this@GeneratorMojo.log.warn(s)
    }

    override fun execute() {
        File(targetDirectory).mkdirs()
        (File(sourceDirectory).listFiles() ?: arrayOf())
            .map { it.name.split(".").first() to it.bufferedReader(Charsets.UTF_8) }
            .map { (name, reader) -> name to WireSpec.compile(reader.lines().collect(Collectors.joining()))(logger) }
            .generate()

    }

    private fun Documents.generate() = when (language) {
        Kotlin -> generateKotlin()
        TypeScript -> generateTypeScript()
        Scala -> TODO()
        Java -> TODO()
        All -> generateAll()
    }.also { log.info("Generating language: ${language.name}") }

    private fun Documents.generateAll() {
        generateKotlin()
        generateTypeScript()
    }

    private fun Documents.generateKotlin() = packageName
        ?.let { KotlinGenerator(targetDirectory, "$it.kotlin", scalarsKotlin, enableOpenApiAnnotations, logger).generate(this) }
        ?: throw RuntimeException("Configure packageName to generate Kotlin data classes")


    private fun Documents.generateTypeScript() =
        TypeScriptGenerator(targetDirectory, project.version, scalarsTypeScript, logger).generate(this)

}
