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

    private val input by argument(
        type = ArgType.String,
        description = "Input file"
    )

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

    val format by option(
        type = ArgType.Choice<Format>(),
        shortName = "f",
        description = "Input format"
    )

    class Convert : Subcommand("convert", "Convert any $Format to Wirespec") {
        override fun execute() {
            TODO("Not yet implemented")
        }
    }

    class Compile : Subcommand("compile", "Compile Wirespec to $Language") {
        override fun execute() {
            TODO("Not yet implemented")
        }
    }

    fun parse(): Arguments {
        subcommands(Convert(), Compile())
        parse(args)
        return Arguments(
            debug = debug,
            input = input,
            output = output,
            languages = languages.toSet(),
            format = format,
            packageName = packageName,
            strict = strict
        )
    }
}

data class Arguments(
    val debug: Boolean,
    val input: String,
    val output: String?,
    val languages: Set<Language>,
    val format: Format?,
    val packageName: String,
    val strict: Boolean,
)
