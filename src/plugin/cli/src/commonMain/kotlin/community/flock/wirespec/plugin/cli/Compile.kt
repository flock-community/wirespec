package community.flock.wirespec.plugin.cli

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.NonEmptySet
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.FileName
import community.flock.wirespec.plugin.files.Reader
import community.flock.wirespec.plugin.files.WirespecFile
import community.flock.wirespec.plugin.files.changeDirectory

fun compile(arguments: CompilerArguments) {
    val context = { file: WirespecFile, output: Directory ->
        object : WirespecContext {
            override val languages: NonEmptySet<Language> = arguments.languages
            override val packageName: PackageName = arguments.packageName
            override val sourceFile: WirespecFile = file
            override val outputDir: Directory = output
            override val liveLogger: Logger = Logger(arguments.logLevel)
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
    val sourceFile: WirespecFile
    val outputDir: Directory
    val liveLogger: Logger
}

private fun WirespecContext.wirespec() = sourceFile
    .changeDirectory(outputDir)
    .emitters(languages, packageName)
    .map { (emitter, outputFile) ->
        val errorOrResults = object : CompilationContext {
            override val logger = liveLogger
            override val emitter = emitter
        }.compile(read())
        when {
            emitter.split -> errorOrResults.map { outputFile to it }
            else -> errorOrResults.map {
                outputFile to nonEmptyListOf(
                    Emitted(
                        outputFile.path.fileName.value.firstToUpper(),
                        it.first().result,
                    ),
                )
            }
        }.map(::WirespecResult)
    }
