@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import arrow.core.Either
import arrow.core.Nel
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST

fun Either<Nel<WirespecException>, List<Emitted>>.produce(): WsCompilationResult = when (this) {
    is Either.Left -> WsCompilationResult(errors = value.map { it.produce() }.toTypedArray())
    is Either.Right -> WsCompilationResult(
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

fun Either<Nel<WirespecException>, AST>.produce(): WsParseResult = when (this) {
    is Either.Left -> WsParseResult(errors = value.map { it.produce() }.toTypedArray())
    is Either.Right -> WsParseResult(result = value.map { it.produce() }.toTypedArray())
}

@JsExport
class WsParseResult(
    val result: Array<WsNode>? = null,
    val errors: Array<WsError>? = null,
)

fun Either<Nel<WirespecException>, String>.produce(): WsStringResult = when (this) {
    is Either.Left -> WsStringResult(errors = value.map { it.produce() }.toTypedArray())
    is Either.Right -> WsStringResult(result = value)
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
