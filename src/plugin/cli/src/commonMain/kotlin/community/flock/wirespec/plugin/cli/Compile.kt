package community.flock.wirespec.plugin.cli

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nel
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.IOException.FileReadException
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Console
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullDirPath
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.Reader
import community.flock.wirespec.plugin.cli.io.Directory
import community.flock.wirespec.plugin.cli.io.File
import community.flock.wirespec.plugin.cli.io.WirespecFile

fun compile(arguments: CompilerArguments): List<Either<NonEmptyList<WirespecException>, Pair<List<Emitted>, File?>>> {
    val input = arguments.input
    val output = arguments.output

    val languages = arguments.languages
    val packageName = arguments.packageName

    val logger = Logger(arguments.logLevel)

    return when (input) {
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
