package community.flock.wirespec.plugin.gradle

import arrow.core.Validated
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
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

    var sourceDirectory: String = ""

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


    companion object {
        class Typescript {
            var targetDirectory: String = ""
        }

        class Java {
            var packageName: String = DEFAULT_PACKAGE_NAME
            var targetDirectory: String = ""
        }

        class Scala {
            var packageName: String = DEFAULT_PACKAGE_NAME
            var targetDirectory: String = ""
        }

        class Kotlin {
            var packageName: String = DEFAULT_PACKAGE_NAME
            var targetDirectory: String = ""
        }
    }
}

class WirespecPlugin : Plugin<Project> {

    private val logger = object : Logger(true) {}

    private fun compile(sourceDirectory: String, logger: Logger, emitter: Emitter) =
        (File(sourceDirectory).listFiles() ?: arrayOf())
            .map { it.name.split(".").first() to it.bufferedReader(Charsets.UTF_8) }
            .map { (name, reader) -> name to Wirespec.compile(reader.collectToString())(logger)(emitter) }
            .map { (name, result) ->
                name to when (result) {
                    is Validated.Valid -> result.value
                    is Validated.Invalid -> error("compile error")
                }
            }
            .flatMap { (name, result) ->
                if (emitter.split) result
                else listOf(name to result.first().second)
            }

    private fun BufferedReader.collectToString() = lines().asSequence().joinToString("")

    override fun apply(project: Project) {
        val extension: WirespecPluginExtension =
            project.extensions.create("wirespec", WirespecPluginExtension::class.java)

        fun emit(targetDirectory: String, emitter: Emitter, ext: String) {
            File(targetDirectory).mkdirs()
            compile(extension.sourceDirectory, logger, emitter).forEach { (name, result) ->
                File("$targetDirectory/$name.$ext").writeText(result)
            }
        }

        project.task("wirespec")
            .doFirst { _: Task? ->
                extension.typescript?.run {
                    TypeScriptEmitter(logger)
                        .apply { emit(targetDirectory, this, "ts") }
                }
                extension.java?.run {
                    JavaEmitter(packageName, logger)
                        .apply { emit(targetDirectory, this, "java") }
                }
                extension.scala?.run {
                    ScalaEmitter(packageName, logger)
                        .apply { emit(targetDirectory, this, "scala") }
                }
                extension.kotlin?.run {
                    KotlinEmitter(packageName, logger)
                        .apply { emit(targetDirectory, this, "kt") }
                }
            }
    }
}
