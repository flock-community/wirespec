package community.flock.wirespec.plugin

import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.File
import community.flock.wirespec.plugin.files.JSONFile
import community.flock.wirespec.plugin.files.WirespecFile

sealed interface WirespecArguments {
    val output: Directory
    val reader: (File) -> String
    val writer: (File, String) -> Unit
    val error: (String) -> Unit
    val languages: NonEmptySet<Language>
    val packageName: PackageName
    val logLevel: Logger.Level
    val shared: Boolean
    val strict: Boolean
}

data class CompilerArguments(
    val input: NonEmptySet<WirespecFile>,
    override val output: Directory,
    override val reader: (File) -> String,
    override val writer: (File, String) -> Unit,
    override val error: (String) -> Unit,
    override val languages: NonEmptySet<Language>,
    override val packageName: PackageName,
    override val logLevel: Logger.Level,
    override val shared: Boolean,
    override val strict: Boolean,
) : WirespecArguments

data class ConverterArguments(
    val format: Format,
    val input: JSONFile,
    override val output: Directory,
    override val reader: (File) -> String,
    override val writer: (File, String) -> Unit,
    override val error: (String) -> Unit,
    override val languages: NonEmptySet<Language>,
    override val packageName: PackageName,
    override val logLevel: Logger.Level,
    override val shared: Boolean,
    override val strict: Boolean,
) : WirespecArguments

fun PackageName?.toDirectory() = let { (it)?.value }
    ?.split(".")
    ?.joinToString("/")
    ?: ""
