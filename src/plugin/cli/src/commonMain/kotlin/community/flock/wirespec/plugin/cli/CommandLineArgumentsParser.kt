package community.flock.wirespec.plugin.cli

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level
import community.flock.wirespec.compiler.utils.Logger.Level.DEBUG
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.compiler.utils.Logger.Level.INFO
import community.flock.wirespec.compiler.utils.Logger.Level.WARN
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.io.ClassPath
import community.flock.wirespec.plugin.io.Directory
import community.flock.wirespec.plugin.io.DirectoryPath
import community.flock.wirespec.plugin.io.FilePath
import community.flock.wirespec.plugin.io.Name
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.Source.Type.JSON
import community.flock.wirespec.plugin.io.getFullPath
import community.flock.wirespec.plugin.io.getOutPutPath
import community.flock.wirespec.plugin.io.or
import community.flock.wirespec.plugin.io.read
import community.flock.wirespec.plugin.io.wirespecSources
import community.flock.wirespec.plugin.io.write
import community.flock.wirespec.plugin.toEmitter

enum class Options(vararg val flags: String) {
    Input("-i", "--input"),
    Output("-o", "--output"),
    Language("-l", "--language"),
    PackageName("-p", "--package"),
    LogLevel("--log-level"),
    Shared("--shared"),
    Strict("--strict"),
}

class WirespecCli : NoOpCliktCommand(name = "wirespec") {
    companion object {
        operator fun invoke(
            compile: (CompilerArguments) -> Unit,
            convert: (ConverterArguments) -> Unit,
        ): WirespecCli = WirespecCli().subcommands(Compile(compile), Convert(convert))
    }
}

private abstract class CommonOptions : CliktCommand() {
    val input by option(*Options.Input.flags, help = "Input")
    val output by option(*Options.Output.flags, help = "Output")
    val packageName by option(*Options.PackageName.flags, help = "Package name")
        .default(DEFAULT_GENERATED_PACKAGE_STRING)
    val logLevel by option(*Options.LogLevel.flags, help = "Log level: $Level").default("$ERROR")
    val shared by option(*Options.Shared.flags, help = "Generate shared wirespec code").flag(default = false)
    val strict by option(*Options.Strict.flags, help = "Strict mode").flag()

    fun String.toLogLevel() = when (trim().uppercase()) {
        "DEBUG" -> DEBUG
        "INFO" -> INFO
        "WARN" -> WARN
        "ERROR" -> ERROR
        else -> throw ChooseALogLevel()
    }

    fun writer(directory: Directory?): (NonEmptyList<Emitted>) -> Unit = { emittedList ->
        emittedList.forEach { emitted ->
            directory
                ?.let { FilePath(it.path.value + "/" + emitted.file).write(emitted.result) }
                ?: echo(emitted.result)
        }
    }
}

private class Compile(
    private val compiler: (CompilerArguments) -> Unit,
) : CommonOptions() {
    private val stdin by argument(help = "stdin").optional()
    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(choices = Language.toMap(), ignoreCase = true)
        .multiple(default = listOf(Language.Kotlin))

    override fun run() {
        val inputPath = getFullPath(input).getOrElse { throw CliktError(it.message) }
        val sources = when (inputPath) {
            null -> stdin?.let { nonEmptySetOf(Source(Name("stdin"), it)) } ?: throw NoInputReceived()
            is ClassPath -> throw NoClasspathPossible()
            is DirectoryPath -> Directory(inputPath).wirespecSources().or(::handleError)
            is FilePath -> when (inputPath.extension) {
                FileExtension.Wirespec -> nonEmptySetOf(
                    Source(
                        inputPath.name,
                        inputPath.read(),
                    ),
                )

                else -> throw WirespecFileError()
            }
        }

        val emitters = languages
            .map { it.toEmitter(PackageName(packageName), EmitShared(shared)) }
            .toNonEmptySetOrNull()
            ?: nonEmptySetOf(WirespecEmitter())

        val outputDir = inputPath?.let { Directory(getOutPutPath(it, output).or(::handleError)) }
        CompilerArguments(
            input = sources,
            emitters = emitters,
            writer = writer(outputDir),
            error = ::handleError,
            packageName = PackageName(packageName),
            logger = Logger(logLevel.toLogLevel()),
            shared = shared,
            strict = strict,
        ).let(compiler)
    }
}

private class Convert(
    private val converter: (ConverterArguments) -> Unit,
) : CommonOptions() {

    private val format by argument(help = "Input format").enum<Format>()
    private val stdin by argument(help = "stdin").optional()
    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(choices = Language.toMap(), ignoreCase = true)
        .multiple()

    override fun run() {
        val inputPath = getFullPath(input).or(::handleError)
        val source = when (inputPath) {
            null -> stdin?.let { Source(Name("stdin"), it) } ?: throw NoInputReceived()
            is ClassPath -> throw NoClasspathPossible()
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (inputPath.extension) {
                FileExtension.JSON -> Source<JSON>(inputPath.name, inputPath.read())
                else -> throw JSONFileError()
            }
        }

        val emitters = languages
            .map { it.toEmitter(PackageName(packageName), EmitShared(shared)) }.toNonEmptySetOrNull()
            ?: nonEmptySetOf(WirespecEmitter())
        val directory = inputPath?.let { Directory(getOutPutPath(it, output).or(::handleError)) }
        ConverterArguments(
            format = format,
            input = nonEmptySetOf(source),
            emitters = emitters,
            writer = writer(directory),
            error = ::handleError,
            packageName = PackageName(packageName),
            logger = Logger(logLevel.toLogLevel()),
            shared = shared,
            strict = strict,
        ).let(converter)
    }
}

private fun handleError(string: String): Nothing = throw CliktError(string)
