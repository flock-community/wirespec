import arrow.core.Validated
import community.flock.wirespec.compiler.core.exceptions.WirespecException

@ExperimentalJsExport
fun Validated<WirespecException, List<Pair<String, String>>>.produce(): WsCompilationResult = when (this) {
    is Validated.Invalid -> WsCompilationResult(error = value.produce())
    is Validated.Valid -> WsCompilationResult(compiled = WsCompiled(value.first().second))
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
