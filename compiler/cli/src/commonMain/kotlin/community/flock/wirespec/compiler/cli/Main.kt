package community.flock.wirespec.compiler.cli

import arrow.core.Either
import community.flock.wirespec.compiler.cli.Language.Jvm.Java
import community.flock.wirespec.compiler.cli.Language.Jvm.Kotlin
import community.flock.wirespec.compiler.cli.Language.Jvm.Scala
import community.flock.wirespec.compiler.cli.Language.Script.TypeScript
import community.flock.wirespec.compiler.cli.Language.Script.Wirespec
import community.flock.wirespec.compiler.cli.io.Directory
import community.flock.wirespec.compiler.cli.io.Extension
import community.flock.wirespec.compiler.cli.io.FullFilePath
import community.flock.wirespec.compiler.cli.io.JavaFile
import community.flock.wirespec.compiler.cli.io.KotlinFile
import community.flock.wirespec.compiler.cli.io.ScalaFile
import community.flock.wirespec.compiler.cli.io.TypeScriptFile
import community.flock.wirespec.compiler.cli.io.WirespecFile
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.getEnvVar
import community.flock.wirespec.compiler.utils.orNull
import community.flock.wirespec.compiler.core.Wirespec as WirespecSpec
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.multiple

private sealed interface Language {
    enum class Jvm : Language { Java, Kotlin, Scala }
    enum class Script : Language { TypeScript, Wirespec }
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
    (0..20)
        .mapNotNull { args.orNull(it) }
        .toTypedArray()
        .let{ cli(it) }
}

fun cli(args: Array<String>) {

    val parser = ArgParser("wirespec")
    val input by parser.argument(ArgType.String, description = "Input file")
    val output by parser.option(ArgType.String, shortName = "o", description = "Output directory")
    val language by parser.option(ArgType.Choice(Language.values().map { it.name }.mapNotNull(logger::from), { Language.valueOf(it) ?: error("Language not found") }), shortName = "l", description = "Language type").default(Language.Jvm.Kotlin).multiple()
    val packageName by parser.option(ArgType.String, shortName = "p", description = "Package name").default(DEFAULT_PACKAGE_NAME)

    parser.parse(args)

    compile(language.toSet(), input, output, packageName)

}

private fun compile(languages: Set<Language>, inputDir: String, outputDir: String?, packageName: String) =
    Directory(inputDir)
        .wirespecFiles()
        .forEach { wsFile ->
            val path = wsFile.path.out(packageName, outputDir)
            wsFile.read()
                .let(WirespecSpec::compile)(logger)
                .let { compiler ->
                    languages.map {
                        when (it) {
                            Java -> JavaEmitter(packageName, logger) to JavaFile(path(Extension.Java))
                            Kotlin -> KotlinEmitter(packageName, logger) to KotlinFile(path(Extension.Kotlin))
                            Scala -> ScalaEmitter(packageName, logger) to ScalaFile(path(Extension.Scala))
                            TypeScript -> TypeScriptEmitter(logger) to TypeScriptFile(path(Extension.TypeScript))
                        Wirespec -> WirespecEmitter(logger) to WirespecFile(path("")(Extension.Wirespec))
                        }.let { (emitter, file) -> compiler(emitter) to file }
                    }
                }
                .map { (results, file) ->
                    when (results) {
                        is Either.Right -> results.value.forEach { (name, result) -> file.copy(name).write(result) }
                        is Either.Left -> println(results.value)
                    }
                }
        }

fun FullFilePath.out(packageName: String, outputDir: String?) = { extension: Extension ->
    val dir = outputDir ?: "$directory/out/${extension.name.lowercase()}"
    copy(
        directory = "$dir/${packageName.split('.').joinToString("/")}",
        extension = extension
    )
}
