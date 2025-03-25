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
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.Logger.Level
import community.flock.wirespec.compiler.utils.Logger.Level.DEBUG
import community.flock.wirespec.compiler.utils.Logger.Level.ERROR
import community.flock.wirespec.compiler.utils.Logger.Level.INFO
import community.flock.wirespec.compiler.utils.Logger.Level.WARN
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.FullPath
import community.flock.wirespec.plugin.files.Source
import community.flock.wirespec.plugin.files.Source.Type.JSON
import community.flock.wirespec.plugin.files.Source.Type.Wirespec
import community.flock.wirespec.plugin.files.SourcePath
import community.flock.wirespec.plugin.files.path
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
    val input by option(*Options.Input.flags, help = "Input")
    val output by option(*Options.Output.flags, help = "Output")
    val packageName by option(*Options.PackageName.flags, help = "Package name")
        .default(DEFAULT_GENERATED_PACKAGE_STRING)
    val logLevel by option(*Options.LogLevel.flags, help = "Log level: $Level").default("$ERROR")
    val shared by option(*Options.Shared.flags, help = "Generate shared wirespec code").flag(default = false)
    val strict by option(*Options.Strict.flags, help = "Strict mode").flag()

    fun getFullPath(input: String?, createIfNotExists: Boolean = false) = when {
        input == null -> null
        input.startsWith("classpath:") -> SourcePath(input.substringAfter("classpath:"))
        else -> {
            val path = Path(input).createIfNotExists(createIfNotExists)
            val meta = SystemFileSystem.metadataOrNull(path) ?: throw CannotAccessFileOrDirectory(input)
            val pathString = path.toString()
            when {
                meta.isDirectory -> DirectoryPath(pathString)
                meta.isRegularFile -> FilePath(pathString)
                else -> throw IsNotAFileOrDirectory(pathString)
            }
        }
    }

    fun getOutPutPath(inputPath: FullPath) = when (val it = getFullPath(output, true)) {
        null -> DirectoryPath("${inputPath.path()}/out")
        is DirectoryPath -> it
        is FilePath, is SourcePath -> throw OutputShouldBeADirectory()
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
        val inputPath = getFullPath(input)
        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> throw NoClasspathPossible()
            is DirectoryPath -> Directory(inputPath).wirespecSources()
            is FilePath -> when (inputPath.extension) {
                FileExtension.Wirespec -> nonEmptySetOf(
                    Source<Wirespec>(
                        inputPath.name,
                        inputPath.read(),
                    ),
                )

                else -> throw WirespecFileError()
            }
        }

        val emitters = languages.map {
            when (it) {
                Language.Java -> JavaEmitter(PackageName(packageName))
                Language.Kotlin -> KotlinEmitter(PackageName(packageName))
                Language.Scala -> ScalaEmitter(PackageName(packageName))
                Language.TypeScript -> TypeScriptEmitter()
                Language.Wirespec -> WirespecEmitter()
                Language.OpenAPIV2 -> OpenAPIV2Emitter
                Language.OpenAPIV3 -> OpenAPIV3Emitter
            }
        }.toNonEmptySetOrNull() ?: nonEmptySetOf(WirespecEmitter())

        CompilerArguments(
            input = sources,
            output = Directory(getOutPutPath(inputPath)),
            emitters = emitters,
            writer = { filePath, string -> filePath.write(string) },
            error = ::handleError,
            packageName = PackageName(packageName),
            logger = Logger(logLevel.toLogLevel()),
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
        val inputPath = getFullPath(input)
        val source = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> throw NoClasspathPossible()
            is DirectoryPath -> throw ConvertNeedsAFile()
            is FilePath -> when (inputPath.extension) {
                FileExtension.JSON -> Source<JSON>(inputPath.name, inputPath.read())
                else -> throw JSONFileError()
            }
        }

        val emitters = languages.map {
            when (it) {
                Language.Java -> JavaEmitter(PackageName(packageName))
                Language.Kotlin -> KotlinEmitter(PackageName(packageName))
                Language.Scala -> ScalaEmitter(PackageName(packageName))
                Language.TypeScript -> TypeScriptEmitter()
                Language.Wirespec -> WirespecEmitter()
                Language.OpenAPIV2 -> OpenAPIV2Emitter
                Language.OpenAPIV3 -> OpenAPIV3Emitter
            }
        }.toNonEmptySetOrNull() ?: nonEmptySetOf(WirespecEmitter())

        ConverterArguments(
            format = format,
            input = nonEmptySetOf(source),
            output = Directory(getOutPutPath(inputPath)),
            emitters = emitters,
            writer = { filePath, string -> filePath.write(string) },
            error = ::handleError,
            packageName = PackageName(packageName),
            logger = Logger(logLevel.toLogLevel()),
            shared = shared,
            strict = strict,
        ).let(converter)
    }
}

fun FilePath.read() = Path(toString())
    .let { SystemFileSystem.source(it).buffered().readString() }

private fun Directory.wirespecSources(): NonEmptySet<Source<Wirespec>> = Path(path.value)
    .let(SystemFileSystem::list)
    .filter(::isRegularFile)
    .filter(::isWirespecFile)
    .map { FilePath(it.toString()) to SystemFileSystem.source(it).buffered().readString() }
    .map { (path, source) -> Source<Wirespec>(name = path.name, content = source) }
    .toNonEmptySetOrNull()
    ?: throw WirespecFileError()

private fun FilePath.write(string: String) = Path(toString())
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
