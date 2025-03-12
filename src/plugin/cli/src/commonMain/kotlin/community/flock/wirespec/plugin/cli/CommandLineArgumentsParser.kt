package community.flock.wirespec.plugin.cli

import arrow.core.NonEmptySet
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
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.utils.Logger.Level
import community.flock.wirespec.compiler.utils.Logger.Level.DEBUG
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.compiler.utils.Logger.Level.INFO
import community.flock.wirespec.compiler.utils.Logger.Level.WARN
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.File
import community.flock.wirespec.plugin.files.FileName
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.FullPath
import community.flock.wirespec.plugin.files.JSONFile
import community.flock.wirespec.plugin.files.WirespecFile
import community.flock.wirespec.plugin.files.plus
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

class WirespecCli : NoOpCliktCommand(name = "wirespec") {
    companion object {
        fun provide(
            compile: (CompilerArguments) -> Unit,
            convert: (ConverterArguments) -> Unit,
        ): WirespecCli = WirespecCli().subcommands(Compile(compile), Convert(convert))
    }
}

private abstract class CommonOptions : CliktCommand() {
    val inputPath by option(*Options.Input.flags, help = "Input")
    val outputPath by option(*Options.Output.flags, help = "Output")
    val packageName by option(*Options.PackageName.flags, help = "Package name")
        .default(DEFAULT_GENERATED_PACKAGE_STRING)
    val logLevel by option(*Options.LogLevel.flags, help = "Log level: $Level").default("$ERROR")
    val shared by option(*Options.Shared.flags, help = "Generate shared wirespec code").flag(default = false)
    val strict by option(*Options.Strict.flags, help = "Strict mode").flag()

    fun getFullPath(input: String?, createIfNotExists: Boolean = false): FullPath? = input?.let {
        val path = Path(it).createIfNotExists(createIfNotExists)
        val meta = SystemFileSystem.metadataOrNull(path) ?: throw CannotAccessFileOrDirectory(it)
        when {
            meta.isDirectory -> DirectoryPath(it)
            meta.isRegularFile -> FilePath(it)
            else -> throw IsNotAFileOrDirectory(it)
        }
    }

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
        val input = when (val it = getFullPath(inputPath)) {
            null -> throw IsNotAFileOrDirectory(null)
            is DirectoryPath -> Directory(it).wirespecFiles()
            is FilePath -> when (it.extension) {
                FileExtension.Wirespec -> nonEmptySetOf(WirespecFile(it))
                else -> throw WirespecFileError()
            }
        }
        val output = when (val it = getFullPath(outputPath, true)) {
            null -> Directory(input.first().path + "/out")
            is DirectoryPath -> Directory(it)
            is FilePath -> throw OutputShouldBeADirectory()
        }
        CompilerArguments(
            inputFiles = input,
            outputDirectory = output + PackageName(packageName),
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
        val input = when (val it = getFullPath(inputPath)) {
            null -> throw IsNotAFileOrDirectory(null)
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (it.extension) {
                FileExtension.JSON -> JSONFile(it)
                else -> throw JSONFileError()
            }
        }
        val output = when (val it = getFullPath(outputPath, true)) {
            null -> Directory(input.path + "/out")
            is DirectoryPath -> Directory(it)
            is FilePath -> throw OutputShouldBeADirectory()
        }
        ConverterArguments(
            format = format,
            inputFiles = nonEmptySetOf(input),
            outputDirectory = output + PackageName(packageName),
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

private fun Directory.wirespecFiles(): NonEmptySet<WirespecFile> = Path(path.value)
    .let(SystemFileSystem::list)
    .asSequence()
    .filter(::isRegularFile)
    .filter(::isWirespecFile)
    .map { it.name }
    .map { it.dropLast(FileExtension.Wirespec.value.length + 1) }
    .map { FilePath(path, FileName(it)) }
    .map(::WirespecFile)
    .toList()
    .toNonEmptySetOrNull()
    ?: throw WirespecFileError()

private fun File.write(string: String) = Path(path.toString())
    .also { it.parent?.createIfNotExists() }
    .let {
        SystemFileSystem.sink(it).buffered()
            .apply { writeString(string) }
            .flush()
    }

private fun Path.createIfNotExists(create: Boolean = true) = also {
    when {
        create && !SystemFileSystem.exists(this) -> SystemFileSystem.createDirectories(this, true)
        else -> Unit
    }
}

private fun isRegularFile(path: Path) = SystemFileSystem.metadataOrNull(path)?.isRegularFile == true

private fun isWirespecFile(path: Path) = path.extension == FileExtension.Wirespec.value

private val Path.extension: String
    get() = name.substringAfterLast('.', "")

private fun handleError(string: String): Nothing = throw CliktError(string)
