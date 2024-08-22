package community.flock.wirespec.compiler.core.emit.common

const val DEFAULT_PACKAGE_STRING = "community.flock.wirespec.generated"

data object Spacer {
    private const val SPACER = "  "

    operator fun invoke(times: Int) = SPACER.repeat(times)
    override fun toString() = SPACER

}
