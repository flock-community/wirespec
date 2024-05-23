package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.shared.JavaShared
import community.flock.wirespec.compiler.core.emit.shared.KotlinShared
import community.flock.wirespec.compiler.core.emit.shared.ScalaShared
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Format.OpenApiV2
import community.flock.wirespec.plugin.Format.OpenApiV3
import community.flock.wirespec.plugin.Language.Java
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.Scala
import community.flock.wirespec.plugin.Language.TypeScript
import community.flock.wirespec.plugin.Language.Wirespec
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
            OpenApiV2 -> OpenApiV2Parser.parse(json, !strict)
            OpenApiV3 -> community.flock.wirespec.openapi.v3.OpenApiV3Parser.parse(json, !strict)
        }

        languages?.map {
            when (it) {
                Java -> JavaEmitter(packageName, decorators, logger)
                    .apply { emit(ast).writeToJvmFiles(FileExtension.Java, JavaShared) }

                Kotlin -> KotlinEmitter(packageName, decorators, logger)
                    .apply { emit(ast).writeToJvmFiles(FileExtension.Kotlin, KotlinShared, fileName) }

                Scala -> ScalaEmitter(packageName, logger)
                    .apply { emit(ast).writeToJvmFiles(FileExtension.Scala, ScalaShared, fileName) }

                TypeScript -> TypeScriptEmitter(logger).emit(ast).writeToFiles(FileExtension.TypeScript, fileName)
                Wirespec -> WirespecEmitter(logger).emit(ast).writeToFiles(FileExtension.Wirespec, fileName)
            }
        } ?: WirespecEmitter(logger).emit(ast).writeToFiles(FileExtension.Wirespec, fileName)
    }
}
