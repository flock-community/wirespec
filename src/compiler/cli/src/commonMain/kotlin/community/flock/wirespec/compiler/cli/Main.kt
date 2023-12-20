package community.flock.wirespec.compiler.cli

import arrow.core.Either
import community.flock.wirespec.compiler.cli.Language.Jvm.Java
import community.flock.wirespec.compiler.cli.Language.Jvm.Kotlin
import community.flock.wirespec.compiler.cli.Language.Jvm.Scala
import community.flock.wirespec.compiler.cli.Language.Script.TypeScript
import community.flock.wirespec.compiler.cli.Language.Spec.Wirespec
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
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.orNull
import community.flock.wirespec.compiler.core.Wirespec as WirespecSpec
import community.flock.wirespec.openapi.v2.OpenApiParser as OpenApiParserV2
import community.flock.wirespec.openapi.v3.OpenApiParser as OpenApiParserV3

enum class Format {
    OPEN_API_V2, OPEN_API_V3;

    companion object {
        override fun toString() = entries.joinToString()
    }
}

sealed interface Language {
    enum class Jvm : Language { Java, Kotlin, Scala }
    enum class Script : Language { TypeScript }
    enum class Spec : Language { Wirespec }

    companion object {
        fun values(): List<Enum<*>> = Jvm.entries + Script.entries + Spec.entries
        fun valueOf(s: String): Language? = values().find { it.name == s } as Language?

        override fun toString() = values().joinToString()
    }
}

fun main(args: Array<String>) {
    (0..20)
        .mapNotNull(args::orNull)
        .toTypedArray()
        .let(::CommandLineEntitiesParser)
        .parse()
        .let(::compile)
}

fun compile(operation: Operation) {

    val input = operation.command.input
    val output = operation.output

    val languages = operation.languages
    val packageName = operation.packageName

    val logger = Logger(operation.debug)

    when (operation.command) {
        is Convert -> {
            val fullPath = FullFilePath.parse(input)
            val file = JsonFile(fullPath)
            val strict = operation.strict
            val ast = when (operation.command.format) {
                Format.OPEN_API_V2 -> OpenApiParserV2.parse(file.read(), !strict)
                Format.OPEN_API_V3 -> OpenApiParserV3.parse(file.read(), !strict)
            }
            val path = fullPath.out(packageName, output)
            emit(languages, packageName, path, logger)
                .map { (emitter, file) ->
                    val result = emitter.emit(ast)
                    if (!emitter.split) listOf(fullPath.fileName.replaceFirstChar(Char::uppercase) to result.first().second) to file
                    else result to file
                }
                .map { (results, file) -> write(results, file) }
        }

        is Compile -> {
            if (input.endsWith(".ws")) WirespecFile(FullFilePath.parse(input))
                .wirespec(languages, packageName, output, logger)
            else Directory(input)
                .wirespecFiles()
                .forEach { it.wirespec(languages, packageName, output, logger) }
        }
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
