package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.utils.Logger
import kotlin.jvm.JvmInline

data class CompilerArguments(
    val operation: Operation,
    val input: Input,
    val output: Output?,
    val languages: Set<Language>,
    val packageName: PackageName,
    val logLevel: Logger.Level,
    val shared: Boolean,
    val strict: Boolean,
)

sealed interface Operation {
    data object Compile : Operation
    data class Convert(val format: Format) : Operation
}

@JvmInline
value class PackageName(override val value: String) : Value<String>

fun PackageName?.toDirectory() = let { (it)?.value }
    ?.split(".")
    ?.joinToString("/")
    ?: ""
