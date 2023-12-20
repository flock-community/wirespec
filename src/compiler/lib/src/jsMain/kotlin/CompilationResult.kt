@file:OptIn(ExperimentalJsExport::class)

import arrow.core.Either
import arrow.core.Nel
import community.flock.wirespec.compiler.core.exceptions.WirespecException

fun Either<Nel<WirespecException>, List<Pair<String, String>>>.produce(): WsCompilationResult = when (this) {
    is Either.Left -> WsCompilationResult(errors = value.map { it.produce() }.toTypedArray())
    is Either.Right -> WsCompilationResult(compiled = WsCompiled(value.first().second))
}

@JsExport
class WsCompilationResult(
    val compiled: WsCompiled? = null,
    val errors: Array<WsError> = emptyArray()
)

@JsExport
class WsCompiled(val value: String)

@JsExport
class WsCompiledFile(val name: String, val value: String)
