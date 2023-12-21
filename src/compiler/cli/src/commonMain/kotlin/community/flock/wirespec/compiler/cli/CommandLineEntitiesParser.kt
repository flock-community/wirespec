package community.flock.wirespec.compiler.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import community.flock.wirespec.compiler.cli.Language.Spec.Wirespec
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME

enum class Options(val flag: String) {
    Output("-o"),
    Language("-l"),
    PackageName("-p"),
    Strict("-s"),
    Debug("-d"),
}

class WirespecCli : NoOpCliktCommand(name = "wirespec")

abstract class SubCommand : CliktCommand() {
    val input by argument(help = "Input file")
    val output by option(Options.Output.flag, help = "Output directory")
    val languages by option(Options.Language.flag, help = "Language").choice(Language.toMap()).multiple()
    val packageName by option(Options.PackageName.flag, help = "Package name").default(DEFAULT_PACKAGE_NAME)
    val strict by option(Options.Strict.flag, help = "Strict mode").boolean().default(false)
    val debug by option(Options.Debug.flag, help = "Debug mode").boolean().default(false)
}

class Compile(private val block: (Arguments) -> Unit) : SubCommand() {
    override fun run() {
        block(
            Arguments(
                input = input,
                operation = Operation.Compile,
                output = output,
                languages = languages.toSet(),
                packageName = packageName,
                strict = strict,
                debug = debug,
            )
        )
    }
}

class Convert(private val block: (Arguments) -> Unit) : SubCommand() {

    private val format by argument(help = "Input format").enum<Format>()

    override fun run() {
        block(
            Arguments(
                input = input,
                operation = Operation.Convert(format = format),
                output = output,
                languages = languages.toSet().ifEmpty { setOf(Wirespec) },
                packageName = packageName,
                strict = strict,
                debug = debug,
            )
        )
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
