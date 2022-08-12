package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException
import community.flock.wirespec.compiler.core.tokenize.Token

@ExperimentalJsExport
fun Either<WireSpecException, List<Token>>.produce(): WsTokenResult = when (this) {
    is Either.Left -> WsTokenResult(error = value.produce())
    is Either.Right -> WsTokenResult(tokens = WsTokens(value.map { it.type.toString() }.toTypedArray()))
}

@JsExport
@ExperimentalJsExport
data class WsTokenResult(
    val tokens: WsTokens? = null,
    val error: WsError? = null
)

@JsExport
@ExperimentalJsExport
data class WsTokens(val value: Array<String>)
