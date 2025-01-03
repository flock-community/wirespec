package community.flock.wirespec.plugin.cli

import arrow.core.Either
import arrow.core.EitherNel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.utils.Logger.Level
import community.flock.wirespec.compiler.utils.Logger.Level.DEBUG
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.compiler.utils.Logger.Level.INFO
import community.flock.wirespec.compiler.utils.Logger.Level.WARN
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Console
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.FullDirPath
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Input
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.Operation
import community.flock.wirespec.plugin.Output
import community.flock.wirespec.plugin.PackageName
import community.flock.wirespec.plugin.cli.io.File

enum class Options(vararg val flags: String) {
    InputDir("-d", "--input-dir"),
    InputFile("-f", "--input-file"),
    OutputDir("-o", "--output-dir"),
    Language("-l", "--language"),
    PackageName("-p", "--package"),
    LogLevel("--log-level"),
    Shared("--shared"),
    Strict("--strict"),
}

class WirespecCli : NoOpCliktCommand(name = "wirespec") {
    companion object {
        fun provide(
            compile: (CompilerArguments) -> List<EitherNel<WirespecException, Pair<List<Emitted>, File?>>>,
            convert: (CompilerArguments) -> List<EitherNel<WirespecException, Pair<List<Emitted>, File?>>>,
            write: List<Emitted>.(File?) -> Unit,
        ): (Array<out String>) -> Unit = WirespecCli().subcommands(Compile(compile, write), Convert(convert, write))::main
    }
}

private abstract class CommonOptions : CliktCommand() {
    private val inputFile by option(*Options.InputFile.flags, help = "Input file")
    val inputDir by option(*Options.InputDir.flags, help = "Input directory")
    val outputDir by option(*Options.OutputDir.flags, help = "Output directory")
    val packageName by option(*Options.PackageName.flags, help = "Package name").default(DEFAULT_GENERATED_PACKAGE_STRING)
    val logLevel by option(*Options.LogLevel.flags, help = "Log level: $Level").default("$ERROR")
    val shared by option(*Options.Shared.flags, help = "Generate shared wirespec code").flag(default = false)
    val strict by option(*Options.Strict.flags, help = "Strict mode").flag()

    fun getInput(inputDir: String? = null): Input =
        if (inputDir != null && inputFile != null) throw CliktError("Choose either a file or a directory. Not Both.")
        else inputFile
            ?.let(FullFilePath.Companion::parse)
            ?: inputDir?.let(::FullDirPath)
            ?: Console

    fun String.toLogLevel() = when (trim().uppercase()) {
        "DEBUG" -> DEBUG
        "INFO" -> INFO
        "WARN" -> WARN
        "ERROR" -> ERROR
        else -> throw CliktError("Choose one of these log levels: $Level")
    }
}

private class Compile(
    private val block: (CompilerArguments) -> List<EitherNel<WirespecException, Pair<List<Emitted>, File?>>>,
    private val write: (List<Emitted>, File?) -> Unit,
) : CommonOptions() {

    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(Language.toMap())
        .multiple(required = true)

    override fun run() {
        CompilerArguments(
            operation = Operation.Compile,
            input = getInput(inputDir),
            output = Output(outputDir),
            languages = languages.toSet(),
            packageName = PackageName(packageName),
            logLevel = logLevel.toLogLevel(),
            shared = shared,
            strict = strict,
        ).let(block).forEach {
            when (it) {
                is Either.Right -> it.value.let { (result, file) -> write(result, file) }
                is Either.Left -> throw CliktError(it.value.joinToString { e -> e.message ?: "" })
            }
        }
    }
}

private class Convert(
    private val block: (CompilerArguments) -> List<EitherNel<WirespecException, Pair<List<Emitted>, File?>>>,
    private val write: (List<Emitted>, File?) -> Unit,
) : CommonOptions() {

    private val format by argument(help = "Input format").enum<Format>()
    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(choices = Language.toMap(), ignoreCase = true)
        .multiple(listOf(Wirespec))

    override fun run() {
        inputDir?.let { echo("To convert, please specify a file", err = true) }
        CompilerArguments(
            operation = Operation.Convert(format = format),
            input = getInput(null),
            output = Output(outputDir),
            languages = languages.toSet().ifEmpty { setOf(Wirespec) },
            packageName = PackageName(packageName),
            logLevel = logLevel.toLogLevel(),
            shared = shared,
            strict = strict,
        ).let(block).forEach {
            when (it) {
                is Either.Right -> it.value.let { (result, file) -> write(result, file) }
                is Either.Left -> echo(it.value, err = true)
            }
        }
    }
}
