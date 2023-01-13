import arrow.core.Either
import community.flock.wirespec.compiler.core.exceptions.WirespecException

@ExperimentalJsExport
fun Either<WirespecException, List<Pair<String, String>>>.produce(): WsCompilationResult = when (this) {
    is Either.Left -> WsCompilationResult(error = value.produce())
    is Either.Right -> WsCompilationResult(compiled = WsCompiled(value.first().second))
}

@JsExport
@ExperimentalJsExport
data class WsCompilationResult(
    val compiled: WsCompiled? = null,
    val error: WsError? = null
)

@JsExport
@ExperimentalJsExport
data class WsCompiled(val value: String)
