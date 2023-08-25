package community.flock.wirespec.compiler.cli

import arrow.core.Either
import community.flock.wirespec.compiler.cli.Language.Jvm.Java
import community.flock.wirespec.compiler.cli.Language.Jvm.Kotlin
import community.flock.wirespec.compiler.cli.Language.Jvm.Scala
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

private enum class OpenapiVersion {
    V2, V3
}

private sealed interface Language {
    enum class Jvm : Language { Java, Kotlin, Scala }
    enum class Script : Language { TypeScript, Wirespec }
    companion object {
        fun values(): List<Enum<*>> = Jvm.values().toList() + Script.values().toList()
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
    val debug by parser.option(ArgType.Boolean, shortName = "d", description = "Debug mode").default(false)
    val input by parser.argument(ArgType.String, description = "Input file")
    val output by parser.option(ArgType.String, shortName = "o", description = "Output directory")
    val language by parser.option(
        ArgType.Choice(
            Language.values().map { it.name }.map { Language.valueOf(it) ?: error("Language not found") },
            { Language.valueOf(it) ?: error("Language not found") }), shortName = "l", description = "Language type"
    ).default(Language.Jvm.Kotlin).multiple()
    val openapi by parser.option(
        ArgType.Choice<OpenapiVersion>(),
        shortName = "a",
        description = "Use openapi as input"
    )
    val packageName by parser.option(ArgType.String, shortName = "p", description = "Package name")
        .default(DEFAULT_PACKAGE_NAME)

    parser.parse(args)

    val logger = object : Logger(debug) {}
    compile(language.toSet(), input, output, packageName, openapi, logger)

}

private fun compile(
    languages: Set<Language>,
    input: String,
    output: String?,
    packageName: String,
    openapi: OpenapiVersion?,
    logger: Logger
) {
    if (openapi != null) {
        val fullPath = FullFilePath.parse(input)
        val file = JsonFile(fullPath)
        val ast = when (openapi) {
            OpenapiVersion.V2 -> OpenApiParserV2.parse(file.read())
            OpenapiVersion.V3 -> OpenApiParserV3.parse(file.read())
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
        Directory(input)
            .wirespecFiles()
            .forEach { wsFile ->
                val path = wsFile.path.out(packageName, output)
                wsFile.read()
                    .let(WirespecSpec::compile)(logger)
                    .let { compiler ->
                        emit(languages, packageName, path, logger)
                            .map { (emitter, file) ->
                                val result = compiler(emitter)
                                if (!emitter.split) result.map { listOf(wsFile.path.fileName.replaceFirstChar(Char::uppercase) to it.first().second) } to file
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
