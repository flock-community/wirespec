package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.openapi.v2.OpenApiParser
import community.flock.wirespec.plugin.maven.Language.Java
import community.flock.wirespec.plugin.maven.Language.Kotlin
import community.flock.wirespec.plugin.maven.Language.Scala
import community.flock.wirespec.plugin.maven.Language.TypeScript
import community.flock.wirespec.plugin.maven.utils.JvmUtil
import java.io.File
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

private enum class Format { OpenApiV2, OpenApiV3 }

@Mojo(name = "convert", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class ConvertMojo : CompileMojo() {

    @Parameter(required = true)
    private lateinit var format: Format

    @Parameter
    private var strict: Boolean = true

    override fun execute() {
        val fileName = input.split("/")
            .last()
            .substringBeforeLast(".")
            .replaceFirstChar(Char::uppercase)

        val json = File(input).readText()

        val ast = when (format) {
            Format.OpenApiV2 -> OpenApiParser.parse(json, !strict)
            Format.OpenApiV3 -> community.flock.wirespec.openapi.v3.OpenApiParser.parse(json, !strict)
        }

        languages?.map {
            when (it) {
                Java -> JavaEmitter(packageName, logger)
                    .apply { emit(ast).writeToJvmFiles(ext = "java", sharedSource = shared) }

                Kotlin -> KotlinEmitter(packageName, logger)
                    .apply { emit(ast).writeToJvmFiles(fileName, "kt", shared) }

                Scala -> ScalaEmitter(packageName, logger)
                    .apply { emit(ast).writeToJvmFiles(fileName, "scala", shared) }

                TypeScript -> TypeScriptEmitter(logger)
                    .apply { emit(ast).writeToFiles(fileName, "ts") }

            }
        } ?: File(output).mkdirs().also {
            WirespecEmitter(logger).emit(ast)
                .writeToFiles(fileName, "ws")
        }
    }

    private fun List<Emitted>.writeToJvmFiles(fileName: String? = null, ext: String, sharedSource: String) =
        forEach { (name, result) ->
            if (shared) {
                JvmUtil.emitJvm("community.flock.wirespec", output, "Wirespec", ext).writeText(sharedSource)
            }
            JvmUtil.emitJvm(packageName, output, fileName ?: name, ext).writeText(result)
        }

    private fun List<Emitted>.writeToFiles(filename: String, ext: String) = forEach { (name, result) ->
        File("$output/$filename.$ext").writeText(result)
    }
}
