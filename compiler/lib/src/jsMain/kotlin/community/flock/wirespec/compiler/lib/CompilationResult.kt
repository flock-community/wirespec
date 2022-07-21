package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException

@JsExport
@ExperimentalJsExport
fun Either<WireSpecException, String>.produce(): WsResult = when (this) {
    is Either.Left -> WsResult(error = WsError(value.index.position, value.message?: "No message"))
    is Either.Right -> WsResult(compiled = WsCompiled(value))
}

@JsExport
@ExperimentalJsExport
data class WsResult(
    val compiled: WsCompiled? = null,
    val error: WsError? = null
)

@JsExport
@ExperimentalJsExport
data class WsCompiled(val value: String)


@JsExport
@ExperimentalJsExport
data class WsError(val index: Int, val value: String)
