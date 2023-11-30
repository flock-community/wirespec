package community.flock.wirespec.compiler.cli

import arrow.core.Either
import community.flock.wirespec.compiler.cli.Language.Jvm
import community.flock.wirespec.compiler.cli.Language.Jvm.Java
import community.flock.wirespec.compiler.cli.Language.Jvm.Kotlin
import community.flock.wirespec.compiler.cli.Language.Jvm.Scala
import community.flock.wirespec.compiler.cli.Language.OpenApi.OpenApiV2
import community.flock.wirespec.compiler.cli.Language.OpenApi.OpenApiV3
import community.flock.wirespec.compiler.cli.Language.OpenApi.entries
import community.flock.wirespec.compiler.cli.Language.Script.TypeScript
import community.flock.wirespec.compiler.cli.Language.Script.Wirespec
import community.flock.wirespec.compiler.cli.io.Directory
import community.flock.wirespec.compiler.cli.io.Extension
import community.flock.wirespec.compiler.cli.io.File
import community.flock.wirespec.compiler.cli.io.FullFilePath
import community.flock.wirespec.compiler.cli.io.JavaFile
import community.flock.wirespec.compiler.cli.io.JsonFile
import community.flock.wirespec.compiler.cli.io.KotlinFile
import community.flock.wirespec.compiler.cli.io.ScalaFile
import community.flock.wirespec.compiler.cli.io.TypeScriptFile
import community.flock.wirespec.compiler.cli.io.WirespecFile
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.OpenApiV2Emitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.orNull
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.multiple
import community.flock.wirespec.compiler.core.Wirespec as WirespecSpec
import community.flock.wirespec.openapi.v2.OpenApiParser as OpenApiParserV2
import community.flock.wirespec.openapi.v3.OpenApiParser as OpenApiParserV3

private enum class Format {
    Wirespec, OpenApiV2, OpenApiV3
}

private sealed interface Language {
    enum class Jvm : Language { Java, Kotlin, Scala }
    enum class Script : Language { TypeScript, Wirespec }
    enum class OpenApi : Language { OpenApiV2, OpenApiV3 }
    companion object {
        fun values(): List<Enum<*>> = Jvm.entries + Script.entries + entries
        fun valueOf(s: String): Language? = values().find { it.name == s } as Language?
    }
}

fun main(args: Array<String>) {
    (0..20)
        .mapNotNull(args::orNull)
        .toTypedArray()
        .let(::cli)
}

fun cli(args: Array<String>) {

    val parser = ArgParser("wirespec")
    val debug by parser
        .option(
            type = ArgType.Boolean,
            shortName = "d",
            description = "Debug mode"
        )
        .default(false)
    val input by parser
        .argument(
            type = ArgType.String,
            description = "Input file"
        )
    val output by parser
        .option(
            type = ArgType.String,
            shortName = "o",
            description = "Output directory"
        )
    val language by parser
        .option(
            type = ArgType.Choice(
                Language.values().map { it.name }.map { Language.valueOf(it) ?: error("Language not found") },
                { Language.valueOf(it) ?: error("Language not found") }), shortName = "l", description = "Language type"
        )
        .default(Jvm.Kotlin).multiple()
    val format by parser
        .option(
            type = ArgType.Choice<Format>(),
            shortName = "f",
            description = "Input format"
        )
        .default(Format.Wirespec)
    val packageName by parser
        .option(
            type = ArgType.String,
            shortName = "p",
            description = "Package name"
        )
        .default(DEFAULT_PACKAGE_NAME)
    val strict by parser
        .option(
            type = ArgType.Boolean,
            shortName = "s",
            description = "Strict mode"
        )
        .default(true)
    parser.parse(args)

    val logger = object : Logger(debug) {}
    compile(language.toSet(), input, output, packageName, format, strict, logger)

}

private fun compile(
    languages: Set<Language>,
    input: String,
    output: String?,
    packageName: String,
    format: Format,
    strict: Boolean,
    logger: Logger
) {
    if (listOf(Format.OpenApiV2, Format.OpenApiV3).contains(format)) {
        val fullPath = FullFilePath.parse(input)
        val file = JsonFile(fullPath)
        val ast = when (format) {
            Format.OpenApiV2 -> OpenApiParserV2.parse(file.read(), !strict)
            Format.OpenApiV3 -> OpenApiParserV3.parse(file.read(), !strict)
            Format.Wirespec -> error("Wirespec is not parsed here")
        }
        val path = fullPath.out(packageName, output)
        emit(languages, packageName, path, logger)
            .map { (emitter, file) ->
                val result = emitter.emit(ast)
                if (!emitter.split) listOf(fullPath.fileName.replaceFirstChar(Char::uppercase) to result.first().second) to file
                else result to file
            }
            .map { (results, file) -> write(results, file) }
    } else {
        if (input.endsWith(".ws")) {
            return WirespecFile(FullFilePath.parse(input))
                .wirespec(languages, packageName, output, logger)

        }
        Directory(input)
            .wirespecFiles()
            .forEach { it.wirespec(languages, packageName, output, logger) }
    }
}

private fun WirespecFile.wirespec(
    languages: Set<Language>,
    packageName: String,
    output: String?,
    logger: Logger
) {
    val path = this.path.out(packageName, output)
    read()
        .let(WirespecSpec::compile)(logger)
        .let { compiler ->
            emit(languages, packageName, path, logger)
                .map { (emitter, file) ->
                    val result = compiler(emitter)
                    if (!emitter.split) result.map { listOf(this.path.fileName.replaceFirstChar(Char::uppercase) to it.first().second) } to file
                    else result to file
                }
        }
        .map { (results, file) ->
            when (results) {
                is Either.Right -> write(results.value, file)
                is Either.Left -> println(results.value)
            }
        }
}

private fun emit(
    languages: Set<Language>,
    packageName: String,
    path: (Extension) -> FullFilePath,
    logger: Logger
): List<Pair<Emitter, File>> {
    return languages
        .map {
            when (it) {
                Java -> JavaEmitter(packageName, logger) to JavaFile(path(Extension.Java))
                Kotlin -> KotlinEmitter(packageName, logger) to KotlinFile(path(Extension.Kotlin))
                Scala -> ScalaEmitter(packageName, logger) to ScalaFile(path(Extension.Scala))
                TypeScript -> TypeScriptEmitter(logger) to TypeScriptFile(path(Extension.TypeScript))
                Wirespec -> WirespecEmitter(logger) to WirespecFile(path(Extension.Wirespec))
                OpenApiV2 -> OpenApiV2Emitter(logger) to JsonFile(path(Extension.Json))
                OpenApiV3 -> OpenApiV2Emitter(logger) to JsonFile(path(Extension.Json))
            }
        }
}

private fun write(output: List<Pair<String, String>>, file: File) {
    output.forEach { (name, result) ->
        file.copy(name).write(result)
    }
}

fun FullFilePath.out(packageName: String, output: String?) = { extension: Extension ->
    val dir = output ?: "$directory/out/${extension.name.lowercase()}"
    copy(
        directory = "$dir/${packageName.split('.').joinToString("/")}",
        extension = extension
    )
}
