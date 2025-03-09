package community.flock.wirespec.plugin.cli

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.NonEmptySet
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Directory
import community.flock.wirespec.plugin.File
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Reader

fun compile(arguments: CompilerArguments) {
    val context = { file: File, output: Directory ->
        object : WirespecContext {
            override val languages: NonEmptySet<Language> = arguments.languages
            override val packageName: PackageName = arguments.packageName ?: PackageName(DEFAULT_GENERATED_PACKAGE_STRING)
            override val path: (FileExtension) -> FilePath = file.out(arguments.packageName, output)
            override val logger: Logger = Logger(arguments.logLevel)
            override fun read(): String = arguments.reader(file)
        }
    }

    return arguments.input
        .flatMap { context(it, arguments.output).wirespec() }
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
    val languages: NonEmptySet<Language>
    val packageName: PackageName
    val path: (FileExtension) -> FilePath
    val logger: Logger
}

private fun WirespecContext.wirespec() = languages
    .emitters(packageName, path).map { (emitter, file) ->
        val errorOrResults = object : CompilationContext {
            override val logger = this@wirespec.logger
            override val emitter = emitter
        }.compile(read())
        when {
            emitter.split -> errorOrResults.map { file to it }
            else -> errorOrResults.map {
                file to nonEmptyListOf(
                    Emitted(
                        path(FileExtension.Wirespec).fileName.value.firstToUpper(),
                        it.first().result,
                    ),
                )
            }
        }.map(::WirespecResult)
    }
