package community.flock.wirespec.plugin.gradle

import arrow.core.Either
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Decorators
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.shared.JavaShared
import community.flock.wirespec.compiler.core.emit.shared.KotlinShared
import community.flock.wirespec.compiler.core.emit.shared.ScalaShared
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileExtension.Java
import community.flock.wirespec.plugin.FileExtension.Kotlin
import community.flock.wirespec.plugin.FileExtension.Scala
import community.flock.wirespec.plugin.FileExtension.TypeScript
import community.flock.wirespec.plugin.FileExtension.Wirespec
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

    private fun compile(input: String, logger: Logger, emitter: Emitter) =
        (File(input).listFiles() ?: arrayOf())
            .map { it.name.split(".").first() to it.bufferedReader(Charsets.UTF_8) }
            .map { (name, reader) -> name to WirespecSpec.compile(reader.collectToString())(logger)(emitter) }
            .map { (name, result) ->
                name to when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> error("compile error")
                }
            }
            .flatMap { (name, result) ->
                if (emitter.split) result
                else listOf(Emitted(name, result.first().result))
            }

    private fun BufferedReader.collectToString() = lines().asSequence().joinToString("")

    override fun apply(project: Project) {
        val extension = project.extensions.create("wirespec", WirespecPluginExtension::class.java)

        fun Emitter.emit(output: String, ext: FileExtension, packageName: PackageName? = null, shared: Shared? = null) {
            compile(extension.input, logger, this)
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
            extension.kotlin?.apply {
                KotlinEmitter(packageName, Decorators(decorators.declaration, decorators.endpoint), logger).emit(output, Kotlin, PackageName(packageName), KotlinShared)
            }
            extension.java?.apply {
                JavaEmitter(packageName, Decorators(decorators.declaration, decorators.endpoint), logger).emit(output, Java, PackageName(packageName), JavaShared)
            }
            extension.scala?.apply {
                ScalaEmitter(packageName, logger).emit(output, Scala, PackageName(packageName), ScalaShared)
            }
            extension.typescript?.apply {
                TypeScriptEmitter(logger).emit(output, TypeScript)
            }
            extension.wirespec?.apply {
                WirespecEmitter(logger).emit(output, Wirespec)
            }
        }
    }
}
