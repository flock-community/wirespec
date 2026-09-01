package community.flock.wirespec.compiler.core.emit

public data object Spacer {
    private const val SPACER = "  "

    override fun toString(): String = SPACER

    public operator fun invoke(times: Int): String = SPACER.repeat(times)
    public operator fun invoke(block: () -> String): String = "$SPACER${block().split("\n").joinToString("\n$SPACER")}"
}
