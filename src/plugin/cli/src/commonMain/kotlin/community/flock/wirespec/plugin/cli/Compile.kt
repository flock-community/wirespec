package community.flock.wirespec.plugin.cli

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.exceptions.FileReadException
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Directory
import community.flock.wirespec.plugin.DirectoryPath
import community.flock.wirespec.plugin.File
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.Reader
import community.flock.wirespec.plugin.cli.io.WirespecFile
import community.flock.wirespec.plugin.cli.io.wirespecFiles

fun compile(arguments: CompilerArguments) {
    val input = arguments.input
    val output = arguments.output

    val languages = arguments.languages
    val packageName = arguments.packageName

    val context = {
            file: File,
            path: (FileExtension) -> FilePath,
        ->
        object : WirespecContext {
            override val languages: Set<Language> = languages
            override val packageName: PackageName = packageName
            override val path: (FileExtension) -> FilePath = path
            override val logger: Logger = Logger(arguments.logLevel)
            override fun read(): String = arguments.reader(file)
        }
    }

    return when (input) {
        is DirectoryPath -> Directory(input)
            .wirespecFiles()
            .flatMap {
                context(it, it.path.out(packageName, output)).wirespec()
            }

        is FilePath ->
            when (input.extension) {
                FileExtension.Wirespec -> {
                    val file = WirespecFile(input)
                    context(file, file.path.out(packageName, output)).wirespec()
                }
                else -> FileReadException("Path $input is not a Wirespec file").nel().left().nel()
            }
    }
        .let { either { it.bindAll() } }
        .let { either ->
            when (either) {
                is Right ->
                    either.value
                        .flatMap { (file, results) -> results.map { (name, result) -> file.copy(FileName(name)) to result } }
                        .forEach { (file, result) -> arguments.writer(file, result) }

                is Left -> arguments.error(either.value.joinToString { it.message })
            }
        }
}

private interface WirespecContext : Reader {
    val languages: Set<Language>
    val packageName: PackageName
    val path: (FileExtension) -> FilePath
    val logger: Logger
}

private fun WirespecContext.wirespec(): WirespecResults = languages
    .emitters(packageName, path).map { (emitter, file) ->
        val errorOrResults = object : CompilationContext {
            override val logger = this@wirespec.logger
            override val emitter = emitter
        }.compile(read())
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
