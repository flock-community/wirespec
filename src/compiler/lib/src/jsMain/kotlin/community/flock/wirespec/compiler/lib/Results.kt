@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ast.AST

public fun EitherNel<WirespecException, List<Emitted>>.produce(): WsCompilationResult = when (this) {
    is Left -> WsCompilationResult(errors = value.map { it.produce() }.toTypedArray())
    is Right -> WsCompilationResult(
        result = WsCompiled(
            value = value
                .map { it.produce() }
                .toTypedArray(),
        ),
    )
}

public fun Emitted.produce(): WsEmitted = WsEmitted(
    file = file,
    result = result,
)

@JsExport
public class WsCompilationResult(
    public val result: WsCompiled? = null,
    public val errors: Array<WsError> = emptyArray(),
)

@JsExport
public class WsCompiled(public val value: Array<WsEmitted>)

@JsExport
public class WsCompiledFile(public val name: String, public val value: String)

public fun EitherNel<WirespecException, AST>.produce(): WsParseResult = when (this) {
    is Left -> WsParseResult(errors = value.map { it.produce() }.toTypedArray())
    is Right -> WsParseResult(result = value.produce())
}

@JsExport
public class WsParseResult(
    public val result: WsAST? = null,
    public val errors: Array<WsError>? = null,
)

public fun EitherNel<WirespecException, String>.produce(): WsStringResult = when (this) {
    is Left -> WsStringResult(errors = value.map { it.produce() }.toTypedArray())
    is Right -> WsStringResult(result = value)
}

@JsExport
public class WsStringResult(
    public val result: String? = null,
    public val errors: Array<WsError>? = null,
)

@JsExport
public class WsEmitted(
    public val file: String,
    public val result: String,
)
