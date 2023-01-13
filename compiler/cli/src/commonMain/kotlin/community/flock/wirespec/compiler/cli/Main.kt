package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.Language.Jvm.Java
import community.flock.wirespec.compiler.cli.Language.Jvm.Kotlin
import community.flock.wirespec.compiler.cli.Language.Jvm.Scala
import community.flock.wirespec.compiler.cli.Language.Script.TypeScript
import community.flock.wirespec.compiler.cli.io.Directory
import community.flock.wirespec.compiler.cli.io.Extension
import community.flock.wirespec.compiler.cli.io.FullFilePath
import community.flock.wirespec.compiler.cli.io.JavaFile
import community.flock.wirespec.compiler.cli.io.KotlinFile
import community.flock.wirespec.compiler.cli.io.ScalaFile
import community.flock.wirespec.compiler.cli.io.TypeScriptFile
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.getEnvVar
import community.flock.wirespec.compiler.utils.orNull

private sealed interface Language {
    enum class Jvm : Language { Java, Kotlin, Scala }
    enum class Script : Language { TypeScript }
    companion object {
        fun values(): List<Enum<*>> = Jvm.values().toList() + Script.values().toList()
        fun valueOf(s: String): Language? = values().find { it.name == s } as Language?
    }
}

private val enableLogging = getEnvVar("WIRESPEC_LOGGING_ENABLED").toBoolean()

private val logger = object : Logger(enableLogging) {}

private fun Logger.from(s: String): Language? = Language.valueOf(s).also {
    if (it == null) warn("'$s' is not known to Wirespec. Choose from ${Language.values().joinToString(",")}")
}

fun main(args: Array<String>) {

    val inputDir = args.orNull(0) ?: ""
    val languages = args.orNull(1)
        ?.split(",")
        ?.mapNotNull(logger::from)
        ?.toSet()
        ?: setOf(Kotlin)
    val packageName = args.orNull(2) ?: DEFAULT_PACKAGE_NAME

    compile(languages, inputDir, packageName)

}

private fun compile(languages: Set<Language>, inputDir: String, packageName: String) = Directory(inputDir)
    .wirespecFiles()
    .forEach { wsFile ->
        wsFile.read()
            .let(Wirespec::compile)(logger)
            .let { it to wsFile.path::out }
            .let { (compiler, path) ->
                languages.map {
                    when (it) {
                        Java -> JavaEmitter(packageName, logger) to JavaFile(path(packageName)(Extension.Java))
                        Kotlin -> KotlinEmitter(packageName, logger) to KotlinFile(path(packageName)(Extension.Kotlin))
                        Scala -> ScalaEmitter(packageName, logger) to ScalaFile(path(packageName)(Extension.Scala))
                        TypeScript -> TypeScriptEmitter(logger) to TypeScriptFile(path("")(Extension.TypeScript))
                    }.let { (emitter, file) -> compiler(emitter) to file }
                }
            }
            .map { (results, file) ->
                results.map {
                    it.map { (name, result) -> file.copy(name).write(result) }
                }
            }
            .forEach { it.mapLeft { error -> throw error } }
    }

fun FullFilePath.out(packageName: String) = { extension: Extension ->
    copy(
        directory = "$directory/out/${extension.name.lowercase()}/${packageName.split('.').joinToString("/")}",
        extension = extension
    )
}
