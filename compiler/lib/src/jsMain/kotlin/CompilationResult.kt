import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException

@ExperimentalJsExport
fun Either<WireSpecException, String>.produce(): WsCompilationResult = when (this) {
    is Either.Left -> WsCompilationResult(error = value.produce())
    is Either.Right -> WsCompilationResult(compiled = WsCompiled(value))
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
