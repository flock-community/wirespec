package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.multiple

@OptIn(ExperimentalCli::class)
class CommandLineEntitiesParser(private val args: Array<String>) : ArgParser("wirespec") {

    private val output by option(
        type = ArgType.String,
        shortName = "o",
        description = "Output directory"
    )

    private val debug by option(
        type = ArgType.Boolean,
        shortName = "d",
        description = "Debug mode"
    ).default(false)

    private val languages by option(
        type = ArgType.Choice(
            Language.values().map { it.name }.map { Language.valueOf(it) ?: error("Language not found") },
            { Language.valueOf(it) ?: error("Language not found") }), shortName = "l", description = "Language type"
    ).multiple()

    private val packageName by option(
        type = ArgType.String,
        shortName = "p",
        description = "Package name"
    ).default(DEFAULT_PACKAGE_NAME)

    private val strict by option(
        type = ArgType.Boolean,
        shortName = "s",
        description = "Strict mode"
    ).default(false)

    class CompileCommand : Subcommand(name = "compile", actionDescription = "Compile Wirespec") {
        private val input by argument(
            type = ArgType.String,
            description = "Input file"
        )

        var command: Command? = null

        override fun execute() {
            command = Compile(input = input)
        }
    }

    class ConvertCommand : Subcommand("convert", "Convert from OpenAPI") {
        private val input by argument(
            type = ArgType.String,
            description = "Input file"
        )

        private val format by argument(
            type = ArgType.Choice<Format>(),
            description = "Input format"
        )

        var command: Command? = null

        override fun execute() {
            command = Convert(format = format, input = input)
        }
    }

    fun parse() = run {
        val compileCommand = CompileCommand()
        val convertCommand = ConvertCommand()
        subcommands(compileCommand, convertCommand)
        parse(args)
        val compile = (subcommands["compile"] as? CompileCommand)?.command
        val convert = (subcommands["convert"] as? ConvertCommand)?.command

        Operation(
            command = compile ?: convert ?: error("provide an operation"),
            output = output,
            languages = languages.toSet(),
            packageName = packageName,
            strict = strict,
            debug = debug,
        )
    }
}

data class Operation(
    val command: Command,
    val output: String?,
    val languages: Set<Language>,
    val packageName: String,
    val strict: Boolean,
    val debug: Boolean,
)

sealed interface Command {
    val input: String
}

data class Compile(
    override val input: String,
) : Command

data class Convert(
    val format: Format,
    override val input: String,
) : Command
