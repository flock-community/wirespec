@file:OptIn(ExperimentalJsExport::class)

import community.flock.wirespec.compiler.core.exceptions.WirespecException

@JsExport
data class WsError(
    @JsName("index") val index: Int,
    @JsName("length") val length: Int,
    @JsName("value") val value: String
)

fun WirespecException.produce() = WsError(
    index = coordinates.idxAndLength.idx - coordinates.idxAndLength.length,
    length = coordinates.idxAndLength.length,
    value = message ?: "No message"
)
