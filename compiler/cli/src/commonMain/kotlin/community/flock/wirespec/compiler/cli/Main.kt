package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.Language.Kotlin
import community.flock.wirespec.compiler.cli.Language.TypeScript
import community.flock.wirespec.compiler.cli.io.Directory
import community.flock.wirespec.compiler.cli.io.Extension
import community.flock.wirespec.compiler.cli.io.KotlinFile
import community.flock.wirespec.compiler.cli.io.Path
import community.flock.wirespec.compiler.cli.io.TypeScriptFile
import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.getEnvVar
import community.flock.wirespec.compiler.utils.getFirst
import community.flock.wirespec.compiler.utils.getSecond

private enum class Language { Kotlin, TypeScript }

private val enableLogging = getEnvVar("WIRE_SPEC_LOGGING_ENABLED").toBoolean()

private val logger = object : Logger(enableLogging) {}

private fun Logger.from(s: String): Language? = runCatching { Language.valueOf(s) }.getOrNull().also {
    if (it == null) warn("'$s' is not known to WireSpec. Choose from ${Language.values().joinToString(",")}")
}

fun main(args: Array<String>) {


    val basePath = getFirst(args) ?: ""
    val languages = getSecond(args)
        ?.split(",")
        ?.mapNotNull(logger::from)
        ?.toSet()
        ?: setOf(Kotlin)

    compile(languages, basePath)


}

private fun compile(languages: Set<Language>, inputDir: String) = Directory(inputDir)
    .wireSpecFiles()
    .forEach { wsFile ->
        wsFile.read()
            .let(WireSpec::compile)(logger)
            .let { it to wsFile.path.out() }
            .let { (compiler, path) ->
                languages.map {
                    when (it) {
                        Kotlin -> KotlinEmitter(logger) to KotlinFile(path(Extension.Kotlin))
                        TypeScript -> TypeScriptEmitter(logger) to TypeScriptFile(path(Extension.TypeScript))
                    }.let { (emitter, file) -> compiler(emitter) to file }
                }
            }
            .map { (result, file) -> result.map(file::write) }
    }

fun Path.out() = { extension: Extension ->
    copy(
        directory = "$directory/out",
        extension = extension
    )
}
