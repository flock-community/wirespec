package community.flock.wirespec.plugin

data class CompilerArguments(
    val operation: Operation,
    val input: Input,
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

enum class Format {
    OpenApiV2, OpenApiV3;

    companion object {
        override fun toString() = entries.joinToString()
    }
}
