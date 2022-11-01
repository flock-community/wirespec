package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.Language.Java
import community.flock.wirespec.compiler.cli.Language.Kotlin
import community.flock.wirespec.compiler.cli.Language.Scala
import community.flock.wirespec.compiler.cli.Language.TypeScript
import community.flock.wirespec.compiler.cli.io.Directory
import community.flock.wirespec.compiler.cli.io.Extension
import community.flock.wirespec.compiler.cli.io.KotlinFile
import community.flock.wirespec.compiler.cli.io.DirPath
import community.flock.wirespec.compiler.cli.io.JavaFile
import community.flock.wirespec.compiler.cli.io.ScalaFile
import community.flock.wirespec.compiler.cli.io.TypeScriptFile
import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.getOrHandle
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.getEnvVar
import community.flock.wirespec.compiler.utils.orNull

private enum class Language { Java, Kotlin, Scala, TypeScript }

private val enableLogging = getEnvVar("WIRE_SPEC_LOGGING_ENABLED").toBoolean()

private val logger = object : Logger(enableLogging) {}

private fun Logger.from(s: String): Language? = runCatching { Language.valueOf(s) }.getOrNull().also {
    if (it == null) warn("'$s' is not known to WireSpec. Choose from ${Language.values().joinToString(",")}")
}

fun main(args: Array<String>) {

    val basePath = args.orNull(0) ?: ""
    val languages = args.orNull(1)
        ?.split(",")
        ?.mapNotNull(logger::from)
        ?.toSet()
        ?: setOf(Kotlin)
    val packageName = args.orNull(2) ?: DEFAULT_PACKAGE_NAME

    compile(languages, basePath, packageName)

}

private fun compile(languages: Set<Language>, inputDir: String, packageName: String) = Directory(inputDir)
    .wireSpecFiles()
    .forEach { wsFile ->
        wsFile.read()
            .let(WireSpec::compile)(logger)
            .let { it to wsFile.path.out(packageName) }
            .let { (compiler, path) ->
                languages.map {
                    when (it) {
                        Java -> JavaEmitter(logger, packageName) to JavaFile(path(Extension.Java))
                        Kotlin -> KotlinEmitter(logger, packageName) to KotlinFile(path(Extension.Kotlin))
                        Scala -> ScalaEmitter(logger, packageName) to ScalaFile(path(Extension.Scala))
                        TypeScript -> TypeScriptEmitter(logger) to TypeScriptFile(path(Extension.TypeScript))
                    }.let { (emitter, file) -> compiler(emitter) to file }
                }
            }
            .map { (result, file) -> result.map(file::write) }
            .forEach { it.getOrHandle { error -> throw error } }
    }

fun DirPath.out(packageName: String) = { extension: Extension ->
    copy(
        directory = "$directory/out/${packageName.split('.').joinToString("/")}",
        extension = extension
    )
}
