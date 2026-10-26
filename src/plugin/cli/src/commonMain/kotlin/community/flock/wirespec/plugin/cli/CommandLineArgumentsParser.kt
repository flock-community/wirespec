package community.flock.wirespec.plugin.cli

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.nonEmptySetOf
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.transformAll
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
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Extension
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
import community.flock.wirespec.plugin.toIrExtensions
import community.flock.wirespec.plugin.withExtensions

internal enum class Options(vararg val flags: String) {
    Input("-i", "--input"),
    Output("-o", "--output"),
    Language("-l", "--language"),
    Extension("-e", "--extension"),
    PackageName("-p", "--package"),
    LogLevel("--log-level"),
    Shared("--shared"),
    Strict("--strict"),
    IgnoreDefaults("--ignore-defaults"),
}

internal class WirespecCli : NoOpCliktCommand(name = "wirespec") {
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
    val logLevel by option(*Options.LogLevel.flags, help = "Log level: $Level").default("$INFO")
    val shared by option(*Options.Shared.flags, help = "Generate shared wirespec code").flag(default = false)
    val strict by option(*Options.Strict.flags, help = "Strict mode").flag()
    val ignoreDefaults by option(*Options.IgnoreDefaults.flags, help = "Leave field default values out of the emitted code").flag()
    val extensions by option(*Options.Extension.flags, help = "Extension, applied to the languages it supports")
        .choice(choices = Extension.toMap(), ignoreCase = true)
        .multiple()

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
    private val language by option(*Options.Language.flags, help = "Language")
        .choice(choices = Language.toMap(), ignoreCase = true)
        .single(default = Language.Kotlin)

    override fun run() {
        val logger = Logger(logLevel.toLogLevel())
        val inputPath = getFullPath(input).getOrElse { throw CliktError(it.message) }
        val sources = when (inputPath) {
            null -> stdin?.let { nonEmptySetOf(Source(Name("stdin"), it)) } ?: throw NoInputReceived()
            is ClassPath -> throw NoClasspathPossible()
            is DirectoryPath -> Directory(inputPath).wirespecSources(logger).or(::handleError)
            is FilePath -> when (inputPath.extension) {
                FileExtension.Wirespec -> nonEmptySetOf<Source<Source.Type.Wirespec>>(
                    Source(
                        inputPath.name,
                        inputPath.read(),
                    ),
                ).also { logger.info("Found 1 wirespec file to process: $inputPath") }

                else -> throw WirespecFileError()
            }
        }

        val emitter = language.toEmitter(PackageName(packageName), EmitShared(shared), extensions)

        val outputDir = inputPath?.let { Directory(getOutPutPath(it, output).or(::handleError)) }
        CompilerArguments(
            input = sources,
            emitter = emitter,
            writer = writer(outputDir),
            error = ::handleError,
            packageName = PackageName(packageName),
            logger = logger,
            shared = shared,
            strict = strict,
            ignoreDefaults = ignoreDefaults,
        ).let(compiler)
    }
}

private class Convert(
    private val converter: (ConverterArguments) -> Unit,
) : CommonOptions() {

    private val format by argument(help = "Input format").enum<Format>()
    private val stdin by argument(help = "stdin").optional()
    private val language by option(*Options.Language.flags, help = "Language")
        .choice(choices = Language.toMap(), ignoreCase = true)
        .single(default = Language.Wirespec)

    override fun run() {
        val logger = Logger(logLevel.toLogLevel())
        val inputPath = getFullPath(input).or(::handleError)
        val source = when (inputPath) {
            null -> stdin?.let { Source(Name("stdin"), it) } ?: throw NoInputReceived()
            is ClassPath -> throw NoClasspathPossible()
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (inputPath.extension) {
                FileExtension.JSON, FileExtension.GraphQL -> Source<JSON>(inputPath.name, inputPath.read())
                else -> throw JSONFileError()
            }
                .also { logger.info("Found 1 file to process: $inputPath") }
        }

        val emitter = language.toEmitter(PackageName(packageName), EmitShared(shared), extensions)
        val directory = inputPath?.let { Directory(getOutPutPath(it, output).or(::handleError)) }
        ConverterArguments(
            format = format,
            input = nonEmptySetOf(source),
            emitter = emitter,
            writer = writer(directory),
            error = ::handleError,
            packageName = PackageName(packageName),
            logger = logger,
            shared = shared,
            strict = strict,
            ignoreDefaults = ignoreDefaults,
        ).let(converter)
    }
}

private fun handleError(string: String): Nothing = throw CliktError(string)

private fun Language.toEmitter(packageName: PackageName, emitShared: EmitShared, extensions: List<Extension>) = toEmitter(packageName, emitShared)
    .let { it.withExtensions(extensions.toIrExtensions(packageName, it.extension)) }

private fun NullableOption<Language, Language>.single(default: Language) = transformAll(defaultForHelp = "$default") { languages ->
    when (languages.size) {
        0 -> default
        1 -> languages.first()
        else -> fail("Pick one language per run, not ${languages.joinToString()}.")
    }
}
