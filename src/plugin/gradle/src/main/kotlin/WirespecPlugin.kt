package community.flock.wirespec.plugin.gradle

import arrow.core.Either
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.shared.JavaShared
import community.flock.wirespec.compiler.core.emit.shared.KotlinShared
import community.flock.wirespec.compiler.core.emit.shared.ScalaShared
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileExtension.Java
import community.flock.wirespec.plugin.FileExtension.Kotlin
import community.flock.wirespec.plugin.FileExtension.Scala
import community.flock.wirespec.plugin.FileExtension.TypeScript
import community.flock.wirespec.plugin.FileExtension.Wirespec
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.toDirectory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.BufferedReader
import java.io.File
import kotlin.streams.asSequence

class WirespecPlugin : Plugin<Project> {

    private val logger = noLogger

    private fun String.readFiles() = (File(this).listFiles() ?: arrayOf())
    private fun Array<File>.parse() = this
        .map { it.name.split(".").first() to it.bufferedReader(Charsets.UTF_8) }
        .map { (name, reader) -> name to WirespecSpec.parse(reader.collectToString())(logger) }

    private fun compile(input: String, emitter: Emitter) =
        input
            .readFiles()
            .parse()
            .map { (name, ast) -> name to ast.map { emitter.emit(it) } }
            .map { (name, result) ->
                name to when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> error("compile error: ${result.value}")
                }
            }
            .flatMap { (name, result) ->
                if (emitter.split) result
                else listOf(Emitted(name, result.first().result))
            }

    private fun BufferedReader.collectToString() = lines().asSequence().joinToString("\n")

    override fun apply(project: Project) {
        val extension = project.extensions.create("wirespec", WirespecPluginExtension::class.java)

        fun Emitter.emit(
            input: String,
            output: String,
            ext: FileExtension,
            packageName: PackageName? = null,
            shared: Shared? = null
        ) {
            compile(input, this)
                .also { project.file("$output${packageName.toDirectory()}").mkdirs() }
                .forEach {
                    shared?.run {
                        project.file("$output/community/flock/wirespec").mkdirs()
                        project.file("$output/community/flock/wirespec/Wirespec.${ext.value}").writeText(source)
                    }
                    project.file("$output${packageName.toDirectory()}${it.typeName}.${ext.value}").writeText(it.result)
                }
        }

        project.task("wirespec").doLast { _: Task? ->
            extension.custom?.apply {
                val ext = FileExtension.values().find { it.value == this.extention }?: error("No file extension")
                emitter?.emit(
                    input,
                    output,
                    ext,
                    PackageName(packageName),
                    shared
                )

            }
            extension.compile?.apply {
                if (Language.Kotlin in languages) {
                    KotlinEmitter(packageName, logger).emit(
                        input,
                        output,
                        Kotlin,
                        PackageName(packageName),
                        KotlinShared
                    )
                }
                if (Language.Java in languages) {
                    JavaEmitter(packageName, logger).emit(
                        input,
                        output,
                        Java,
                        PackageName(packageName),
                        JavaShared
                    )
                }
                if (Language.Scala in languages) {
                    ScalaEmitter(packageName, logger).emit(
                        input,
                        output,
                        Scala,
                        PackageName(packageName),
                        ScalaShared
                    )
                }
                if (Language.TypeScript in languages) {
                    TypeScriptEmitter(logger).emit(
                        input,
                        output,
                        TypeScript,
                        PackageName(packageName),
                        null
                    )
                }
                if (Language.Wirespec in languages) {
                    WirespecEmitter(logger)
                        .emit(
                            input,
                            output,
                            Wirespec,
                            PackageName(packageName),
                            null
                        )
                }
            }
        }
    }
}
