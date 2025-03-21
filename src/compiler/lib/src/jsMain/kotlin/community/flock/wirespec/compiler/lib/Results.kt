@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST

fun EitherNel<WirespecException, List<Emitted>>.produce(): WsCompilationResult = when (this) {
    is Left -> WsCompilationResult(errors = value.map { it.produce() }.toTypedArray())
    is Right -> WsCompilationResult(
        result = WsCompiled(
            value = value
                .map { it.produce() }
                .toTypedArray(),
        ),
    )
}

fun Emitted.produce() = WsEmitted(
    typeName = typeName,
    result = result,
)

@JsExport
class WsCompilationResult(
    val result: WsCompiled? = null,
    val errors: Array<WsError> = emptyArray(),
)

@JsExport
class WsCompiled(val value: Array<WsEmitted>)

@JsExport
class WsCompiledFile(val name: String, val value: String)

fun EitherNel<WirespecException, AST>.produce(): WsParseResult = when (this) {
    is Left -> WsParseResult(errors = value.map { it.produce() }.toTypedArray())
    is Right -> WsParseResult(result = value.produce())
}

@JsExport
class WsParseResult(
    val result: WsAST? = null,
    val errors: Array<WsError>? = null,
)

fun EitherNel<WirespecException, String>.produce(): WsStringResult = when (this) {
    is Left -> WsStringResult(errors = value.map { it.produce() }.toTypedArray())
    is Right -> WsStringResult(result = value)
}

@JsExport
class WsStringResult(
    val result: String? = null,
    val errors: Array<WsError>? = null,
)

@JsExport
class WsEmitted(
    val typeName: String,
    val result: String,
)
