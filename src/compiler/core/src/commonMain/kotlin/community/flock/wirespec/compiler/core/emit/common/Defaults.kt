package community.flock.wirespec.compiler.core.emit.common

const val DEFAULT_PACKAGE = "community.flock.wirespec"
const val DEFAULT_SHARED_PACKAGE_STRING = DEFAULT_PACKAGE
const val DEFAULT_GENERATED_PACKAGE_STRING = "$DEFAULT_PACKAGE.generated"

data object Spacer {
    private const val SPACER = "  "

    override fun toString() = SPACER

    operator fun invoke(times: Int) = SPACER.repeat(times)
    operator fun invoke(block: () -> String) = "$SPACER${block().split("\n").joinToString("\n$SPACER")}"
}
