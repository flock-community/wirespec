package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.openapi.v2.OpenApiParser
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Format.OpenApiV2
import community.flock.wirespec.plugin.Format.OpenApiV3
import community.flock.wirespec.plugin.Language.Java
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.Scala
import community.flock.wirespec.plugin.Language.TypeScript
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.maven.utils.JvmUtil
import java.io.File
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

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
            OpenApiV2 -> OpenApiParser.parse(json, !strict)
            OpenApiV3 -> community.flock.wirespec.openapi.v3.OpenApiParser.parse(json, !strict)
        }

        languages?.map {
            when (it) {
                Java -> JavaEmitter(packageName, logger)
                    .apply { emit(ast).writeToJvmFiles(FileExtension.Java, shared) }

                Kotlin -> KotlinEmitter(packageName, logger)
                    .apply { emit(ast).writeToJvmFiles(FileExtension.Kotlin, shared, fileName) }

                Scala -> ScalaEmitter(packageName, logger)
                    .apply { emit(ast).writeToJvmFiles(FileExtension.Scala, shared, fileName) }

                TypeScript -> TypeScriptEmitter(logger)
                    .apply { emit(ast).writeToFiles(FileExtension.TypeScript, fileName) }

                Wirespec -> WirespecEmitter(logger)
                    .apply { emit(ast).writeToFiles(FileExtension.Wirespec, fileName) }
            }
        } ?: WirespecEmitter(logger).emit(ast)
            .writeToFiles(FileExtension.Wirespec, fileName)
    }

    private fun List<Emitted>.writeToJvmFiles(fe: FileExtension, sharedSource: String, fileName: String? = null) =
        forEach { (name, result) ->
            if (shared) {
                JvmUtil.emitJvm("community.flock.wirespec", output, Wirespec.name, fe.value).writeText(sharedSource)
            }
            JvmUtil.emitJvm(packageName, output, fileName ?: name, fe.value).writeText(result)
        }

    private fun List<Emitted>.writeToFiles(fe: FileExtension, filename: String) = forEach { (_, result) ->
        File(output).mkdirs()
        File("$output/$filename.${fe.value}").writeText(result)
    }
}
