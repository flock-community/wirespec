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
    Output("-o", "--output"),
    Language("-l", "--language"),
    PackageName("-p", "--package"),
    Strict("-s", "--strict"),
    Debug("-d", "--debug"),
}

class WirespecCli : NoOpCliktCommand(name = "wirespec") {
    companion object {
        fun run(
            compile: (Arguments) -> Unit,
            convert: (Arguments) -> Unit,
        ): (Array<out String>) -> Unit = WirespecCli().subcommands(Compile(compile), Convert(convert))::main

    }
}

abstract class SubCommand : CliktCommand() {
    val input by argument(help = "Input file")
    val output by option(*Options.Output.flags, help = "Output directory")
    val packageName by option(*Options.PackageName.flags, help = "Package name").default(DEFAULT_PACKAGE_NAME)
    val strict by option(*Options.Strict.flags, help = "Strict mode").flag()
    val debug by option(*Options.Debug.flags, help = "Debug mode").flag()
}

class Compile(private val block: (Arguments) -> Unit) : SubCommand() {
    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(Language.toMap())
        .multiple(required = true)

    override fun run() {
        Arguments(
            input = input,
            operation = Operation.Compile,
            output = output,
            languages = languages.toSet(),
            packageName = packageName,
            strict = strict,
            debug = debug,
        ).let(block)
    }
}

class Convert(private val block: (Arguments) -> Unit) : SubCommand() {

    private val format by argument(help = "Input format").enum<Format>()
    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(choices = Language.toMap(), ignoreCase = true)
        .multiple(listOf(Wirespec))

    override fun run() {
        Arguments(
            input = input,
            operation = Operation.Convert(format = format),
            output = output,
            languages = languages.toSet().ifEmpty { setOf(Wirespec) },
            packageName = packageName,
            strict = strict,
            debug = debug,
        ).let(block)
    }
}

data class Arguments(
    val input: String,
    val operation: Operation,
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
