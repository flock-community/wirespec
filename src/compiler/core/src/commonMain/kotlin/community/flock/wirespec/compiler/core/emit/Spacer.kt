package community.flock.wirespec.compiler.core.emit

data object Spacer {
    private const val INDENT = "  "

    override fun toString() = INDENT

    operator fun invoke(times: Int) = INDENT.repeat(times)
    operator fun invoke(block: () -> String) = "$INDENT${block().split("\n").joinToString("\n$INDENT")}"
}

fun String.spacer(space: Int = 1) = split("\n")
    .joinToString("\n") {
        if (it.isNotBlank()) {
            "${(1..space).joinToString("") { "$Spacer" }}$it"
        } else {
            it
        }
    }
