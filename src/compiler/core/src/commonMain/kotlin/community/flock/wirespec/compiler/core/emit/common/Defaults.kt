package community.flock.wirespec.compiler.core.emit.common

const val DEFAULT_PACKAGE_STRING = "community.flock.wirespec.generated"

data object Spacer {
    private const val SPACER = "  "

    override fun toString() = SPACER

    operator fun invoke(times: Int) = SPACER.repeat(times)
    operator fun invoke(block: () -> String) = "$SPACER${block().split("\n").joinToString("\n$SPACER")}"

}
