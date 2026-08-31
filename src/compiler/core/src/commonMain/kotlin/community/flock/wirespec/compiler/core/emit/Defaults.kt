package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Reference

private const val DEFAULT_PACKAGE = "community.flock.wirespec"
public const val DEFAULT_SHARED_PACKAGE_STRING: String = DEFAULT_PACKAGE
public const val DEFAULT_GENERATED_PACKAGE_STRING: String = "$DEFAULT_PACKAGE.generated"

public fun Definition.namespace(): String = when (this) {
    is Endpoint -> "endpoint"
    is Channel -> "channel"
    is Model -> "model"
}

internal fun Reference.root(): Reference = when (this) {
    is Reference.Dict -> reference
    is Reference.Iterable -> reference
    else -> this
}

internal fun Reference.flatten(): Reference = when (this) {
    is Reference.Dict -> reference.flatten()
    is Reference.Iterable -> reference.flatten()
    else -> this
}
