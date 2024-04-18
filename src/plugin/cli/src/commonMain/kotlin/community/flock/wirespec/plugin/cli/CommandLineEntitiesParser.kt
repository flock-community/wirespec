package community.flock.wirespec.plugin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.Console
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.FullDirPath
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Input
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Language.Spec.Wirespec
import community.flock.wirespec.plugin.Operation
import community.flock.wirespec.plugin.Output
import community.flock.wirespec.plugin.PackageName

enum class Options(vararg val flags: String) {
    InputDir("-d", "--input-dir"),
    InputFile("-f", "--input-file"),
    OutputDir("-o", "--output-dir"),
    Language("-l", "--language"),
    PackageName("-p", "--package"),
    Strict("--strict"),
    Debug("--debug"),
}

class WirespecCli : NoOpCliktCommand(name = "wirespec") {
    companion object {
        fun provide(
            compile: (CompilerArguments) -> Unit,
            convert: (CompilerArguments) -> Unit,
        ): (Array<out String>) -> Unit = WirespecCli().subcommands(Compile(compile), Convert(convert))::main
    }
}

private abstract class CommonOptions : CliktCommand() {
    private val inputFile by option(*Options.InputFile.flags, help = "Input file")
    val inputDir by option(*Options.InputDir.flags, help = "Input directory")
    val outputDir by option(*Options.OutputDir.flags, help = "Output directory")
    val packageName by option(*Options.PackageName.flags, help = "Package name").default(DEFAULT_PACKAGE_STRING)
    val strict by option(*Options.Strict.flags, help = "Strict mode").flag()
    val debug by option(*Options.Debug.flags, help = "Debug mode").flag()

    fun getInput(inputDir: String?): Input =
        if (inputDir != null && inputFile != null) error("Choose either a file or a directory. Not Both.")
        else inputFile
            ?.let(FullFilePath.Companion::parse)
            ?: inputDir?.let(::FullDirPath)
            ?: Console
}

private class Compile(private val block: (CompilerArguments) -> Unit) : CommonOptions() {

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
            strict = strict,
            debug = debug,
        ).let(block)
    }
}

private class Convert(private val block: (CompilerArguments) -> Unit) : CommonOptions() {

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
            strict = strict,
            debug = debug,
        ).let(block)
    }
}
