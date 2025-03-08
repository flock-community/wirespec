package community.flock.wirespec.plugin

import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.utils.Logger
import kotlin.jvm.JvmInline

sealed interface WirespecArguments {
    val input: Input
    val output: Output?
    val languages: NonEmptySet<Language>
    val packageName: PackageName
    val logLevel: Logger.Level
    val shared: Boolean
    val strict: Boolean
}

data class CompilerArguments(
    override val input: Input,
    override val output: Output?,
    override val languages: NonEmptySet<Language>,
    override val packageName: PackageName,
    override val logLevel: Logger.Level,
    override val shared: Boolean,
    override val strict: Boolean,
) : WirespecArguments

data class ConverterArguments(
    val format: Format,
    override val input: FilePath,
    override val output: Output?,
    override val languages: NonEmptySet<Language>,
    override val packageName: PackageName,
    override val logLevel: Logger.Level,
    override val shared: Boolean,
    override val strict: Boolean,
) : WirespecArguments

@JvmInline
value class PackageName(override val value: String) : Value<String> {
    override fun toString() = value
}

fun PackageName?.toDirectory() = let { (it)?.value }
    ?.split(".")
    ?.joinToString("/")
    ?: ""
