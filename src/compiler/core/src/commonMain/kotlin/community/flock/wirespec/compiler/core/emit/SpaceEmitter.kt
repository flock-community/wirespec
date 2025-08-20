package community.flock.wirespec.compiler.core.emit

data object Spacer {
    private const val SPACER = "  "

    override fun toString() = SPACER

    operator fun invoke(times: Int) = SPACER.repeat(times)
    operator fun invoke(block: () -> String) = "$SPACER${block().split("\n").joinToString("\n$SPACER")}"
}

fun String.spacer(space: Int = 1) = split("\n")
    .joinToString("\n") {
        if (it.isNotBlank()) {
            "${(1..space).joinToString("") { "$Spacer" }}$it"
        } else {
            it
        }
    }
