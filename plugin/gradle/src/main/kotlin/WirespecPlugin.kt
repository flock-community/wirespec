package community.flock.wirespec.plugin.gradle

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
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


open class WirespecPluginExtension @Inject constructor(val objectFactory: ObjectFactory) {

    var sourceDirectory: String = ""

    var kotlin: Kotlin? = null
    fun kotlin(action: Action<in Kotlin>) {
        val kotlin = Kotlin()
        action.execute(kotlin)
        this.kotlin = kotlin

    }
    class Kotlin() {
        var targetDirectory: String = ""
    }

    var typescript: Typescript? = null
    fun typescript(action: Action<in Typescript>) {
        val typescript = Typescript()
        action.execute(typescript)
        this.typescript = typescript

    }
    class Typescript() {
        var targetDirectory: String = ""
    }
}

class WirespecPlugin : Plugin<Project> {

    private val logger = object : Logger(true) {}

    private fun compile(sourceDirectory: String, logger: Logger, emitter: Emitter) =
        (File(sourceDirectory).listFiles() ?: arrayOf<File>())
            .map { it.name.split(".").first() to it.bufferedReader(Charsets.UTF_8) }
            .map { (name, reader) -> name to WireSpec.compile(reader.collectToString())(logger)(emitter) }
            .map { (name, result) ->
                name to when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> {
                        error("compile error")
                    }
                }
            }

    private fun BufferedReader.collectToString() =
        lines().collect(java.util.stream.Collectors.joining())

    override fun apply(project: Project) {
        val extension: WirespecPluginExtension =
            project.extensions.create("wirespec", WirespecPluginExtension::class.java)

        project.task("wirespec")
            .doLast { task: Task? ->
                extension.kotlin?.run {
                    val emitter = KotlinEmitter(logger)
                    File(targetDirectory).mkdirs()
                    compile(extension.sourceDirectory, logger, emitter)
                        .forEach { (name, result) ->
                            File("${targetDirectory}/$name.kt").writeText(result)
                        }
                }
                extension.typescript?.run {
                    val emitter = TypeScriptEmitter(logger)
                    File(targetDirectory).mkdirs()
                    compile(extension.sourceDirectory, logger, emitter)
                        .forEach { (name, result) ->
                            File("${targetDirectory}/$name.kt").writeText(result)
                        }
                }
            }
    }
}