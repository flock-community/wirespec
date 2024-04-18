package community.flock.wirespec.plugin.cli

import arrow.core.Either
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.component1
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Console
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.FullDirPath
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Language.Jvm.Java
import community.flock.wirespec.plugin.Language.Jvm.Kotlin
import community.flock.wirespec.plugin.Language.Jvm.Scala
import community.flock.wirespec.plugin.Language.Script.TypeScript
import community.flock.wirespec.plugin.Language.Spec.Wirespec
import community.flock.wirespec.plugin.Operation
import community.flock.wirespec.plugin.Output
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.Reader
import community.flock.wirespec.plugin.cli.io.Directory
import community.flock.wirespec.plugin.cli.io.File
import community.flock.wirespec.plugin.cli.io.JavaFile
import community.flock.wirespec.plugin.cli.io.JsonFile
import community.flock.wirespec.plugin.cli.io.KotlinFile
import community.flock.wirespec.plugin.cli.io.ScalaFile
import community.flock.wirespec.plugin.cli.io.TypeScriptFile
import community.flock.wirespec.plugin.cli.io.WirespecFile
import community.flock.wirespec.plugin.utils.orNull
import community.flock.wirespec.compiler.core.Wirespec as WirespecSpec
import community.flock.wirespec.openapi.v2.OpenApiParser as OpenApiParserV2
import community.flock.wirespec.openapi.v3.OpenApiParser as OpenApiParserV3

fun main(args: Array<String>) {
    (0..20)
        .mapNotNull(args::orNull)
        .toTypedArray()
        .let(WirespecCli.provide(::compile, ::convert))
}

fun convert(arguments: CompilerArguments) = compile(arguments)

fun compile(arguments: CompilerArguments) {

    val input = arguments.input
    val output = arguments.output

    val languages = arguments.languages
    val packageName = arguments.packageName

    val logger = Logger(arguments.debug)

    when (val operation = arguments.operation) {
        is Operation.Convert -> {

            val fullPath = input as FullFilePath
            val file = JsonFile(fullPath)
            val strict = arguments.strict
            val format = operation.format
            val ast = when (format) {
                Format.OpenApiV2 -> OpenApiParserV2.parse(file.read(), !strict)
                Format.OpenApiV3 -> OpenApiParserV3.parse(file.read(), !strict)
            }
            val path = fullPath.out(packageName, output)
            languages.emitters(packageName, path, logger).map { (emitter, file) ->
                val results = emitter.emit(ast)
                if (!emitter.split) listOf(
                    Emitted(
                        fullPath.fileName.replaceFirstChar(Char::uppercase),
                        results.first().result
                    )
                ) to file
                else results to file
            }.map { (results, file) -> write(results, file) }
        }

        is Operation.Compile -> when (input) {
            is Console -> input
                .wirespec(languages, packageName, { FullFilePath(output!!.value, "console", it) }, logger)

            is FullDirPath -> Directory(input.path)
                .wirespecFiles()
                .forEach { it.wirespec(languages, packageName, it.path.out(packageName, output), logger) }

            is FullFilePath ->
                if (input.extension == FileExtension.Wirespec) WirespecFile(input)
                    .let { it.wirespec(languages, packageName, it.path.out(packageName, output), logger) }
                else error("Path $input is not a Wirespec file")
        }
    }
}

private fun Reader.wirespec(
    languages: Set<Language>,
    packageName: PackageName,
    path: (FileExtension) -> FullFilePath,
    logger: Logger
) {
    read()
        .let(WirespecSpec::compile)(logger)
        .let { compiler ->
            languages.emitters(packageName, path, logger).map { (emitter, file) ->
                val results = compiler(emitter)
                if (!emitter.split) results.map {
                    listOf(
                        Emitted(
                            path(community.flock.wirespec.plugin.FileExtension.Wirespec).fileName.firstToUpper(),
                            it.first().result
                        )
                    )
                } to file
                else results to file
            }
        }
        .map { (results, file) ->
            when (results) {
                is Either.Right -> write(results.value, file)
                is Either.Left -> println(results.value)
            }
        }
}

private fun Set<Language>.emitters(packageName: PackageName, path: ((FileExtension) -> FullFilePath)?, logger: Logger) =
    map {
        val (packageString) = packageName
        when (it) {
            Java -> JavaEmitter(packageString, logger) to path?.let { JavaFile(it(FileExtension.Java)) }
            Kotlin -> KotlinEmitter(packageString, logger) to path?.let { KotlinFile(it(FileExtension.Kotlin)) }
            Scala -> ScalaEmitter(packageString, logger) to path?.let { ScalaFile(it(FileExtension.Scala)) }
            TypeScript -> TypeScriptEmitter(logger) to path?.let { TypeScriptFile(it(FileExtension.TypeScript)) }
            Wirespec -> WirespecEmitter(logger) to path?.let { WirespecFile(it(FileExtension.Wirespec)) }
        }
    }

private fun write(output: List<Emitted>, file: File?) {
    output.forEach { (name, result) -> file?.copy(name)?.write(result) ?: print(result) }
}

private fun FullFilePath.out(packageName: PackageName, output: Output?) = { extension: FileExtension ->
    val dir = output ?: "$directory/out/${extension.name.lowercase()}"
    copy(
        directory = "$dir/${packageName.value.split('.').joinToString("/")}",
        extension = extension
    )
}
