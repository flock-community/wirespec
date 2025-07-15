package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Model

const val DEFAULT_PACKAGE = "community.flock.wirespec"
const val DEFAULT_SHARED_PACKAGE_STRING = DEFAULT_PACKAGE
const val DEFAULT_GENERATED_PACKAGE_STRING = "$DEFAULT_PACKAGE.generated"

fun Definition.namespace() = when (this) {
    is Endpoint -> "endpoint"
    is Channel -> "channel"
    is Model -> "model"
}

data object Spacer {
    private const val SPACER = "  "

    override fun toString() = SPACER

    operator fun invoke(times: Int) = SPACER.repeat(times)
    operator fun invoke(block: () -> String) = "$SPACER${block().split("\n").joinToString("\n$SPACER")}"
}
