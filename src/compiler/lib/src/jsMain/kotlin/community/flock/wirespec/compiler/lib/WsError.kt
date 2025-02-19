@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.exceptions.WirespecException

@JsExport
data class WsError(
    @JsName("line") val line: Int,
    @JsName("position") val position: Int,
    @JsName("index") val index: Int,
    @JsName("length") val length: Int,
    @JsName("value") val value: String,
)

fun WirespecException.produce() = WsError(
    line = coordinates.line,
    position = coordinates.position,
    index = coordinates.idxAndLength.idx - coordinates.idxAndLength.length,
    length = coordinates.idxAndLength.length,
    value = message ?: "No message",
)
