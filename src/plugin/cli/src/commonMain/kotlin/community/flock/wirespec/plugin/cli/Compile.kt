package community.flock.wirespec.plugin.cli

import arrow.core.left
import arrow.core.nel
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.exceptions.FileReadException
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Console
import community.flock.wirespec.plugin.DirectoryPath
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.Reader
import community.flock.wirespec.plugin.cli.io.Directory
import community.flock.wirespec.plugin.cli.io.WirespecFile

fun compile(arguments: CompilerArguments): WirespecResults {
    val input = arguments.input
    val output = arguments.output

    val languages = arguments.languages
    val packageName = arguments.packageName

    val logger = Logger(arguments.logLevel)

    return when (input) {
        is Console ->
            input
                .wirespec(languages, packageName, { FilePath(DirectoryPath(output!!.value), FileName("console"), it) }, logger)

        is DirectoryPath -> Directory(input)
            .wirespecFiles()
            .flatMap { it.wirespec(languages, packageName, it.path.out(packageName, output), logger) }

        is FilePath ->
            when (input.extension) {
                FileExtension.Wirespec -> WirespecFile(input)
                    .let { it.wirespec(languages, packageName, it.path.out(packageName, output), logger) }

                else -> FileReadException("Path $input is not a Wirespec file").nel().left().nel()
            }
    }
}

private fun Reader.wirespec(
    languages: Set<Language>,
    packageName: PackageName,
    path: (FileExtension) -> FilePath,
    logger: Logger,
): WirespecResults = read()
    .let { source ->
        languages.emitters(packageName, path).map { (emitter, file) ->
            val errorOrResults = object : CompilationContext {
                override val logger = logger
                override val emitter = emitter
            }.compile(source)
            when {
                emitter.split -> errorOrResults.map { file to it }
                else -> errorOrResults.map {
                    file to listOf(
                        Emitted(
                            path(FileExtension.Wirespec).fileName.value.firstToUpper(),
                            it.first().result,
                        ),
                    )
                }
            }.map(::WirespecResult)
        }
    }
