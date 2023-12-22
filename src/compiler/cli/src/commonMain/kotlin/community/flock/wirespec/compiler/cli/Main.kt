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
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.orNull
import community.flock.wirespec.compiler.core.Wirespec as WirespecSpec
import community.flock.wirespec.openapi.v2.OpenApiParser as OpenApiParserV2
import community.flock.wirespec.openapi.v3.OpenApiParser as OpenApiParserV3

@OptIn(ExperimentalStdlibApi::class)
enum class Format {
    OpenApiV2, OpenApiV3;

    companion object {
        override fun toString() = entries.joinToString()
    }
}

@OptIn(ExperimentalStdlibApi::class)
sealed interface Language {
    enum class Jvm : Language { Java, Kotlin, Scala }
    enum class Script : Language { TypeScript }
    enum class Spec : Language { Wirespec }

    companion object {
        fun toMap() = values().associateBy { it.name }.mapValues { (_, v) -> v as Language }
        override fun toString() = values().joinToString()

        private fun values(): List<Enum<*>> = Jvm.entries + Script.entries + Spec.entries
    }
}

fun main(args: Array<String>) {
    (0..20)
        .mapNotNull(args::orNull)
        .toTypedArray()
        .let(WirespecCli.provide(::compile, ::convert))
}

fun convert(arguments: Arguments) = compile(arguments)

fun compile(arguments: Arguments) {

    val input = arguments.input
    val output = arguments.output

    val languages = arguments.languages
    val packageName = arguments.packageName

    val logger = Logger(arguments.debug)

    when (arguments.operation) {
        is Operation.Convert -> {

            val fullPath = input as FullFilePath
            val file = JsonFile(fullPath)
            val strict = arguments.strict
            val ast = when (arguments.operation.format) {
                Format.OpenApiV2 -> OpenApiParserV2.parse(file.read(), !strict)
                Format.OpenApiV3 -> OpenApiParserV3.parse(file.read(), !strict)
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

        is Operation.Compile -> when (input) {
            is Console -> input
                .wirespec(languages, packageName, { FullFilePath(output!!, "console", it) }, logger)

            is FullDirPath -> Directory(input.path)
                .wirespecFiles()
                .forEach { it.wirespec(languages, packageName, it.path.out(packageName, output), logger) }

            is FullFilePath ->
                if (input.extension == Extension.Wirespec) WirespecFile(input)
                    .let { it.wirespec(languages, packageName, it.path.out(packageName, output), logger) }
                else error("Path $input is not a Wirespec file")
        }
    }
}

private fun Reader.wirespec(
    languages: Set<Language>,
    packageName: String,
    path: (Extension) -> FullFilePath,
    logger: Logger
) {
    read()
        .let(WirespecSpec::compile)(logger)
        .let { compiler ->
            emit(languages, packageName, path, logger)
                .map { (emitter, file) ->
                    val result = compiler(emitter)
                    if (!emitter.split) result.map { listOf(path(Extension.Wirespec).fileName.firstToUpper() to it.first().second) } to file
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

private fun emit(languages: Set<Language>, packageName: String, path: (Extension) -> FullFilePath, logger: Logger) =
    languages.map {
        when (it) {
            Java -> JavaEmitter(packageName, logger) to JavaFile(path(Extension.Java))
            Kotlin -> KotlinEmitter(packageName, logger) to KotlinFile(path(Extension.Kotlin))
            Scala -> ScalaEmitter(packageName, logger) to ScalaFile(path(Extension.Scala))
            TypeScript -> TypeScriptEmitter(logger) to TypeScriptFile(path(Extension.TypeScript))
            Wirespec -> WirespecEmitter(logger) to WirespecFile(path(Extension.Wirespec))
        }
    }

private fun write(output: List<Pair<String, String>>, file: File) {
    output.forEach { (name, result) -> file.copy(name).write(result) }
}

private fun FullFilePath.out(packageName: String, output: String?) = { extension: Extension ->
    val dir = output ?: "$directory/out/${extension.name.lowercase()}"
    copy(
        directory = "$dir/${packageName.split('.').joinToString("/")}",
        extension = extension
    )
}
