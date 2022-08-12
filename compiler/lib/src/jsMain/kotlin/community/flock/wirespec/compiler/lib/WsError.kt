package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.exceptions.WireSpecException

@JsExport
@ExperimentalJsExport
data class WsError(val index: Int, val length: Int, val value: String)

@ExperimentalJsExport
fun WireSpecException.produce() = WsError(
    index = coordinates.idxAndLength.idx - coordinates.idxAndLength.length,
    length = coordinates.idxAndLength.length,
    value = message ?: "No message"
)
