package community.flock.wirespec.plugin.gradle

import arrow.core.Either
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import java.io.BufferedReader
import java.io.File
import javax.inject.Inject
import kotlin.streams.asSequence

open class WirespecPluginExtension @Inject constructor(val objectFactory: ObjectFactory) {

    var input: String = ""

    var typescript: Typescript? = null
    fun typescript(action: Action<in Typescript>) {
        typescript = Typescript().apply(action::execute)
    }

    var kotlin: Kotlin? = null
    fun kotlin(action: Action<in Kotlin>) {
        kotlin = Kotlin().apply(action::execute)
    }

    var java: Java? = null
    fun java(action: Action<in Java>) {
        java = Java().apply(action::execute)
    }

    var scala: Scala? = null
    fun scala(action: Action<in Scala>) {
        scala = Scala().apply(action::execute)
    }

    var wirespec: Wirespec? = null
    fun wirespec(action: Action<in Wirespec>) {
        wirespec = Wirespec().apply(action::execute)
    }

    companion object {
        abstract class HasTargetDirectory {
            var output: String = ""
        }

        abstract class JvmLanguage : HasTargetDirectory() {
            var packageName: String = DEFAULT_PACKAGE_NAME
        }

        class Typescript : HasTargetDirectory()
        class Java : JvmLanguage()
        class Scala : JvmLanguage()
        class Kotlin : JvmLanguage()
        class Wirespec : HasTargetDirectory()
    }
}

class WirespecPlugin : Plugin<Project> {

    private val logger = object : Logger() {}

    private fun compile(input: String, logger: Logger, emitter: Emitter) =
        (File(input).listFiles() ?: arrayOf())
            .map { it.name.split(".").first() to it.bufferedReader(Charsets.UTF_8) }
            .map { (name, reader) -> name to Wirespec.compile(reader.collectToString())(logger)(emitter) }
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
        val extension: WirespecPluginExtension = project.extensions
            .create("wirespec", WirespecPluginExtension::class.java)

        fun Emitter.emit(output: String, ext: String) {
            compile(extension.input, logger, this)
                .also { project.file(output).mkdirs() }
                .forEach {
                    shared?.run {
                        if(listOf("java", "kt").contains(ext)){
                            project.file("$output/community/flock/wirespec").mkdirs()
                            project.file("$output/community/flock/wirespec/Wirespec.$ext").writeText(this)
                        }
                    }
                    project.file("$output/${it.typeName}.$ext").writeText(it.result)
                }
        }

        project.task("wirespec").doFirst { _: Task? ->
            extension.typescript?.apply { TypeScriptEmitter(logger).emit(output, "ts") }
            extension.java?.apply { JavaEmitter(packageName, logger).emit(output, "java") }
            extension.scala?.apply { ScalaEmitter(packageName, logger).emit(output, "scala") }
            extension.kotlin?.apply { KotlinEmitter(packageName, logger).emit(output, "kt") }
            extension.wirespec?.apply { WirespecEmitter(logger).emit(output, "kt") }
        }
    }
}
