package community.flock.wirespec.plugin

import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.File
import community.flock.wirespec.plugin.files.JSONFile
import community.flock.wirespec.plugin.files.WirespecFile

sealed interface WirespecArguments {
    val inputFiles: NonEmptySet<File>
    val outputDirectory: Directory
    val reader: (File) -> String
    val writer: (File, String) -> Unit
    val error: (String) -> Unit
    val languages: NonEmptySet<Language>
    val packageName: PackageName
    val logger: Logger
    val shared: Boolean
    val strict: Boolean
}

data class CompilerArguments(
    override val inputFiles: NonEmptySet<WirespecFile>,
    override val outputDirectory: Directory,
    override val reader: (File) -> String,
    override val writer: (File, String) -> Unit,
    override val error: (String) -> Unit,
    override val languages: NonEmptySet<Language>,
    override val packageName: PackageName,
    override val logger: Logger,
    override val shared: Boolean,
    override val strict: Boolean,
) : WirespecArguments

data class ConverterArguments(
    val format: Format,
    override val inputFiles: NonEmptySet<JSONFile>,
    override val outputDirectory: Directory,
    override val reader: (File) -> String,
    override val writer: (File, String) -> Unit,
    override val error: (String) -> Unit,
    override val languages: NonEmptySet<Language>,
    override val packageName: PackageName,
    override val logger: Logger,
    override val shared: Boolean,
    override val strict: Boolean,
) : WirespecArguments

fun PackageName?.toDirectory() = this?.value
    ?.split(".")
    ?.joinToString("/")
    ?: ""
