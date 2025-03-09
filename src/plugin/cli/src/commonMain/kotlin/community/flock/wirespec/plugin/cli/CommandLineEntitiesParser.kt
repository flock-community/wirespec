package community.flock.wirespec.plugin.cli

import arrow.core.EitherNel
import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
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
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.DirectoryPath
import community.flock.wirespec.plugin.File
import community.flock.wirespec.plugin.FilePath
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Input
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.Output
import community.flock.wirespec.plugin.PackageName
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

enum class Options(vararg val flags: String) {
    Input("-i", "--input"),
    Output("-o", "--output"),
    Language("-l", "--language"),
    PackageName("-p", "--package"),
    LogLevel("--log-level"),
    Shared("--shared"),
    Strict("--strict"),
}

data class WirespecResult(val file: File, val emitted: List<Emitted>) {
    constructor(pair: Pair<File, List<Emitted>>) : this(pair.first, pair.second)
}

typealias WirespecResults = List<EitherNel<WirespecException, WirespecResult>>

class WirespecCli : NoOpCliktCommand(name = "wirespec") {
    companion object {
        fun provide(
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

    fun getInput(input: String?): Input = input?.let {
        val meta = SystemFileSystem.metadataOrNull(Path(it)) ?: throw CannotAccessFileOrDirectory(it)
        when {
            meta.isDirectory -> DirectoryPath(it)
            meta.isRegularFile -> FilePath.parse(it)
            else -> throw IsNotAFileOrDirectory(it)
        }
    } ?: throw IsNotAFileOrDirectory("null")

    fun String.toLogLevel() = when (trim().uppercase()) {
        "DEBUG" -> DEBUG
        "INFO" -> INFO
        "WARN" -> WARN
        "ERROR" -> ERROR
        else -> throw ChooseALogLevel()
    }
}

private class Compile(
    private val compiler: (CompilerArguments) -> Unit,
) : CommonOptions() {

    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(choices = Language.toMap(), ignoreCase = true)
        .multiple(required = true)

    override fun run() {
        CompilerArguments(
            input = getInput(input),
            output = Output(output),
            reader = { it.read() },
            writer = { file, string -> file.write(string) },
            error = ::handleError,
            languages = languages.toNonEmptySetOrNull() ?: throw ThisShouldNeverHappen(),
            packageName = PackageName(packageName),
            logLevel = logLevel.toLogLevel(),
            shared = shared,
            strict = strict,
        ).let(compiler)
    }
}

private class Convert(
    private val converter: (ConverterArguments) -> Unit,
) : CommonOptions() {

    private val format by argument(help = "Input format").enum<Format>()
    private val languages by option(*Options.Language.flags, help = "Language")
        .choice(choices = Language.toMap(), ignoreCase = true)
        .multiple()

    override fun run() {
        val filePath = when (val it = getInput(input)) {
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> it
        }
        ConverterArguments(
            format = format,
            input = filePath,
            output = Output(output),
            reader = { it.read() },
            writer = { file, string -> file.write(string) },
            error = ::handleError,
            languages = languages.toNonEmptySetOrNull() ?: nonEmptySetOf(Wirespec),
            packageName = PackageName(packageName),
            logLevel = logLevel.toLogLevel(),
            shared = shared,
            strict = strict,
        ).let(converter)
    }
}

fun File.read() = Path(path.toString())
    .let { SystemFileSystem.source(it).buffered().readString() }

private fun File.write(string: String) = Path(path.toString())
    .let { path ->
        path.parent
            ?.takeIf { !SystemFileSystem.exists(it) }
            ?.let { SystemFileSystem.createDirectories(it, true) }
        SystemFileSystem.sink(path).buffered()
            .apply { writeString(string) }
            .flush()
    }

private fun handleError(string: String): Nothing = throw CliktError(string)
