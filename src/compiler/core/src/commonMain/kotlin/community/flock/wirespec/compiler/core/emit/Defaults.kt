package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Model
import community.flock.wirespec.compiler.core.parse.Reference

const val DEFAULT_PACKAGE = "community.flock.wirespec"
const val DEFAULT_SHARED_PACKAGE_STRING = DEFAULT_PACKAGE
const val DEFAULT_GENERATED_PACKAGE_STRING = "$DEFAULT_PACKAGE.generated"

fun Definition.namespace() = when (this) {
    is Endpoint -> "endpoint"
    is Channel -> "channel"
    is Model -> "model"
}

fun Reference.root() = when (this) {
    is Reference.Dict -> reference
    is Reference.Iterable -> reference
    else -> this
}
fun Reference.flattenListDict(): Reference = when (this) {
    is Reference.Dict -> reference.flattenListDict()
    is Reference.Iterable -> reference.flattenListDict()
    else -> this
}
