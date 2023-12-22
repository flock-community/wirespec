package community.flock.wirespec.compiler.cli

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
import community.flock.wirespec.compiler.cli.Language.Spec.Wirespec
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME

enum class Options(vararg val flags: String) {
    InputDir("-d", "--input-dir"),
    InputFile("-f", "--input-file"),
    OutputDir("-o", "--output-dir"),
    Language("-l", "--language"),
    PackageName("-p", "--package"),
    Strict("-s", "--strict"),
    Debug("-b", "--debug"),
}

class WirespecCli : NoOpCliktCommand(name = "wirespec") {
    companion object {
        fun provide(
            compile: (Arguments) -> Unit,
            convert: (Arguments) -> Unit,
        ): (Array<out String>) -> Unit = WirespecCli().subcommands(Compile(compile), Convert(convert))::main
    }
}

abstract class CommonOptions : CliktCommand() {
    private val inputFile by option(*Options.InputFile.flags, help = "Input file")
    val inputDir by option(*Options.InputDir.flags, help = "Input directory")
    val outputDir by option(*Options.OutputDir.flags, help = "Output directory")
    val packageName by option(*Options.PackageName.flags, help = "Package name").default(DEFAULT_PACKAGE_NAME)
    val strict by option(*Options.Strict.flags, help = "Strict mode").flag()
    val debug by option(*Options.Debug.flags, help = "Debug mode").flag()

    fun getInput(inputDir: String?): Input =
        if (inputDir != null && inputFile != null) error("Choose either a file or a directory. Not Both.")
        else inputFile
            ?.let(FullFilePath.Companion::parse)
            ?: inputDir?.let(::FullDirPath)
            ?: Console
}

class Compile(private val block: (Arguments) -> Unit) : CommonOptions() {

    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(Language.toMap())
        .multiple(required = true)

    override fun run() {
        Arguments(
            operation = Operation.Compile,
            input = getInput(inputDir),
            output = outputDir,
            languages = languages.toSet(),
            packageName = packageName,
            strict = strict,
            debug = debug,
        ).let(block)
    }
}

class Convert(private val block: (Arguments) -> Unit) : CommonOptions() {

    private val format by argument(help = "Input format").enum<Format>()
    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(choices = Language.toMap(), ignoreCase = true)
        .multiple(listOf(Wirespec))

    override fun run() {
        inputDir?.let { echo("To convert, please specify a file", err = true) }
        Arguments(
            operation = Operation.Convert(format = format),
            input = getInput(null),
            output = outputDir,
            languages = languages.toSet().ifEmpty { setOf(Wirespec) },
            packageName = packageName,
            strict = strict,
            debug = debug,
        ).let(block)
    }
}

data class Arguments(
    val operation: Operation,
    val input: Input,
    val output: String?,
    val languages: Set<Language>,
    val packageName: String,
    val strict: Boolean,
    val debug: Boolean,
)

sealed interface Operation {
    data object Compile : Operation
    data class Convert(val format: Format) : Operation
}
