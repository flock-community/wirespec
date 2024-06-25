package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.Value
import kotlin.jvm.JvmInline

data class CompilerArguments(
    val operation: Operation,
    val input: Input,
    val output: Output?,
    val languages: Set<Language>,
    val packageName: PackageName,
    val shared: Boolean,
    val strict: Boolean,
    val debug: Boolean,
)

sealed interface Operation {
    data object Compile : Operation
    data class Convert(val format: Format) : Operation
}

enum class Format {
    OpenApiV2, OpenApiV3;

    companion object {
        override fun toString() = entries.joinToString()
    }
}

@JvmInline
value class PackageName(override val value: String) : Value<String>

fun PackageName?.toDirectory() = let { (it)?.value }
    ?.split(".")
    ?.joinToString("/")
    ?: ""
