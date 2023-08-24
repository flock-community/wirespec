package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.plugin.maven.utils.JvmUtil
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.BufferedReader
import java.io.File
import community.flock.wirespec.openapi.v2.OpenApiParser as OpenApiParserV2
import community.flock.wirespec.openapi.v3.OpenApiParser as OpenApiParserV3

private enum class Language { Java, Kotlin, Scala, TypeScript, Wirespec }

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class GenerateMojo : BaseMojo() {

    @Parameter(required = true)
    private lateinit var input: String

    @Parameter(required = true)
    private lateinit var output: String

    @Parameter
    private var packageName: String = DEFAULT_PACKAGE_NAME

    @Parameter
    private var openapi: String? = null

    @Parameter
    private var languages: List<Language>? = null

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    override fun execute() {
        languages?.forEach {
            when (it) {
                Language.Java -> executeJava()
                Language.Kotlin -> executeKotlin()
                Language.Scala -> executeScala()
                Language.TypeScript -> executeTypeScript()
                Language.Wirespec -> TODO()
            }
        }
    }

    fun executeKotlin() {
        val emitter = KotlinEmitter(packageName, logger)
        if (openapi != null) {
            val fileName = input.split("/")
                .last()
                .substringBeforeLast(".")
                .replaceFirstChar(Char::uppercase)
            val json = File(input).readText()
            val ast = when (openapi) {
                "v2" -> OpenApiParserV2.parse(json)
                "v3" -> OpenApiParserV3.parse(json)
                else -> error("Api version not found")
            }
            val result = emitter.emit(ast).joinToString("\n") { it.second }
            JvmUtil.emitJvm(packageName, output, fileName, "kt").writeText(result)
        } else {
            compile(input, logger, emitter)
                .forEach { (name, result) ->
                    JvmUtil.emitJvm(packageName, output, name, "kt").writeText(result)
                }
        }
    }

    fun executeJava() {
        val emitter = JavaEmitter(packageName, logger)

        if (openapi != null) {
            val json = File(input).readText()
            val ast = when (openapi) {
                "v2" -> OpenApiParserV2.parse(json)
                "v3" -> OpenApiParserV3.parse(json)
                else -> error("Api version not found")
            }
            emitter.emit(ast).forEach { (name, result) ->
                JvmUtil.emitJvm(packageName, output, name, "java").writeText(result)
            }
        } else {
            compile(input, logger, emitter)
                .forEach { (name, result) ->
                    JvmUtil.emitJvm(packageName, output, name, "java").writeText(result)
                }
        }
    }

    fun executeScala() {
        val emitter = ScalaEmitter(packageName, logger)
        compile(input, logger, emitter)
            .forEach { (name, result) -> JvmUtil.emitJvm(packageName, output, name, "scala").writeText(result) }
    }

    fun executeTypeScript() {
        val emitter = TypeScriptEmitter(logger)
        File(output).mkdirs()
        compile(input, logger, emitter)
            .forEach { (name, result) ->
                File("$output/$name.ts").writeText(result)
            }
    }
}
