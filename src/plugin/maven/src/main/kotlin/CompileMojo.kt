package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.plugin.maven.utils.JvmUtil
import java.io.File
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import community.flock.wirespec.openapi.v2.OpenApiParser as OpenApiParserV2
import community.flock.wirespec.openapi.v3.OpenApiParser as OpenApiParserV3

private enum class Language { Java, Kotlin, Scala, TypeScript, Wirespec }
private enum class Format { Wirespec, OpenApiV2, OpenApiV3 }

@Mojo(name = "compile", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class CompileMojo : BaseMojo() {

    @Parameter
    private var languages: List<Language>? = null

    @Parameter
    private var format: Format? = null

    @Parameter
    private var strict: Boolean = true

    @Parameter
    private var shared: Boolean = true

    override fun execute() {
        languages?.forEach {
            when (it) {
                Language.Java -> executeJava()
                Language.Kotlin -> executeKotlin()
                Language.Scala -> executeScala()
                Language.TypeScript -> executeTypeScript()
                Language.Wirespec -> TODO("Not yet implemented")
            }
        }
        project.addCompileSourceRoot(output);
    }

    fun executeKotlin() {
        val emitter = KotlinEmitter(packageName, logger)
        if (shared) {
            JvmUtil.emitJvm("community.flock.wirespec", output, "Wirespec", "kt").writeText(emitter.shared)
        }
        if (format == Format.OpenApiV2 || format == Format.OpenApiV3) {
            val fileName = input.split("/")
                .last()
                .substringBeforeLast(".")
                .replaceFirstChar(Char::uppercase)
            val json = File(input).readText()
            val ast = when (format) {
                Format.OpenApiV2 -> OpenApiParserV2.parse(json, !strict)
                Format.OpenApiV3 -> OpenApiParserV3.parse(json, !strict)
                else -> error("Format not found")
            }
            val result = emitter.emit(ast).joinToString("\n") { it.result }
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
        if (shared) {
            JvmUtil.emitJvm("community.flock.wirespec", output, "Wirespec", "java").writeText(emitter.shared)
        }
        if (format == Format.OpenApiV2 || format == Format.OpenApiV3) {
            val json = File(input).readText()
            val ast = when (format) {
                Format.OpenApiV2 -> OpenApiParserV2.parse(json, strict)
                Format.OpenApiV3 -> OpenApiParserV3.parse(json, strict)
                else -> error("Format not found")
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
        if (shared) {
            JvmUtil.emitJvm("community.flock.wirespec", output, "Wirespec", "scala").writeText(emitter.shared)
        }
        compile(input, logger, emitter)
            .forEach { JvmUtil.emitJvm(packageName, output, it.typeName, "scala").writeText(it.result) }
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
