package community.flock.wirespec.plugin.cli

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.component1
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.IOException.FileReadException
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenApiV2Emitter
import community.flock.wirespec.openapi.v3.OpenApiV3Emitter
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Console
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.FullDirPath
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Language.Java
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.OpenAPIV2
import community.flock.wirespec.plugin.Language.OpenAPIV3
import community.flock.wirespec.plugin.Language.Scala
import community.flock.wirespec.plugin.Language.TypeScript
import community.flock.wirespec.plugin.Language.Wirespec
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
import community.flock.wirespec.openapi.v2.OpenApiV2Parser as OpenApiParserV2
import community.flock.wirespec.openapi.v3.OpenApiV3Parser as OpenApiParserV3

fun main(args: Array<String>) {
    (0..20)
        .mapNotNull(args::orNull)
        .toTypedArray()
        .let(WirespecCli.provide(::compile, ::convert, ::write))
}

fun convert(arguments: CompilerArguments): List<Either<NonEmptyList<WirespecException>, Pair<List<Emitted>, File?>>> = compile(arguments)

fun compile(arguments: CompilerArguments): List<Either<NonEmptyList<WirespecException>, Pair<List<Emitted>, File?>>> {
    val input = arguments.input
    val output = arguments.output

    val languages = arguments.languages
    val packageName = arguments.packageName

    val logger = Logger(arguments.logLevel)

    return when (val operation = arguments.operation) {
        is Operation.Convert -> {
            val fullPath = input as FullFilePath
            val file = JsonFile(fullPath)
            val strict = arguments.strict
            val format = operation.format
            val ast = when (format) {
                Format.OpenApiV2 -> OpenApiParserV2.parse(file.read(), !strict)
                Format.OpenApiV3 -> OpenApiParserV3.parse(file.read(), !strict)
                Format.Avro -> AvroParser.parse(file.read())
            }
            val path = fullPath.out(packageName, output)
            languages.emitters(packageName, path, logger).map { (emitter, file) ->
                val results = emitter.emit(ast)
                if (!emitter.split) {
                    listOf(
                        Emitted(
                            fullPath.fileName.value.replaceFirstChar(Char::uppercase),
                            results.first().result,
                        ),
                    ) to file
                } else {
                    results to file
                }
            }.map { it.right() }
        }

        is Operation.Compile -> when (input) {
            is Console ->
                input
                    .wirespec(languages, packageName, { FullFilePath(output!!.value, FileName("console"), it) }, logger)

            is FullDirPath -> Directory(input.path)
                .wirespecFiles()
                .flatMap { it.wirespec(languages, packageName, it.path.out(packageName, output), logger) }

            is FullFilePath ->
                if (input.extension == FileExtension.Wirespec) {
                    WirespecFile(input)
                        .let { it.wirespec(languages, packageName, it.path.out(packageName, output), logger) }
                } else {
                    FileReadException("Path $input is not a Wirespec file").nel().left().nel()
                }
        }
    }
}

fun write(output: List<Emitted>, file: File?) {
    output.forEach { (name, result) -> file?.copy(FileName(name))?.write(result) ?: print(result) }
}

private fun Reader.wirespec(
    languages: Set<Language>,
    packageName: PackageName,
    path: (FileExtension) -> FullFilePath,
    logger: Logger,
) = read()
    .let { source ->
        languages.emitters(packageName, path, logger).map { (emitter, file) ->
            val results = object : CompilationContext {
                override val logger = logger
                override val emitter = emitter
            }.compile(source)
            if (!emitter.split) {
                results.map {
                    listOf(
                        Emitted(
                            path(FileExtension.Wirespec).fileName.value.firstToUpper(),
                            it.first().result,
                        ),
                    )
                } to file
            } else {
                results to file
            }
        }
    }
    .map { (results, file) -> results.map { it to file } }

private fun Set<Language>.emitters(
    packageName: PackageName,
    path: ((FileExtension) -> FullFilePath)?,
    logger: Logger,
) = map {
    val (packageString) = packageName
    when (it) {
        Java -> JavaEmitter(packageString, logger) to path?.let { JavaFile(it(FileExtension.Java)) }
        Kotlin -> KotlinEmitter(packageString, logger) to path?.let { KotlinFile(it(FileExtension.Kotlin)) }
        Scala -> ScalaEmitter(packageString, logger) to path?.let { ScalaFile(it(FileExtension.Scala)) }
        TypeScript -> TypeScriptEmitter(logger) to path?.let { TypeScriptFile(it(FileExtension.TypeScript)) }
        Wirespec -> WirespecEmitter(logger) to path?.let { WirespecFile(it(FileExtension.Wirespec)) }
        OpenAPIV2 -> OpenApiV2Emitter to path?.let { JsonFile(it(FileExtension.Json)) }
        OpenAPIV3 -> OpenApiV3Emitter to path?.let { JsonFile(it(FileExtension.Json)) }
    }
}

private fun FullFilePath.out(packageName: PackageName, output: Output?) = { extension: FileExtension ->
    val dir = output ?: "$directory/out/${extension.name.lowercase()}"
    copy(
        directory = "$dir/${packageName.value.split('.').joinToString("/")}",
        extension = extension,
    )
}
