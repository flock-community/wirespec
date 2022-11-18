import community.flock.wirespec.compiler.core.exceptions.WirespecException

@JsExport
@ExperimentalJsExport
data class WsError(
    @JsName("index") val index: Int,
    @JsName("length") val length: Int,
    @JsName("value") val value: String
)

@ExperimentalJsExport
fun WirespecException.produce() = WsError(
    index = coordinates.idxAndLength.idx - coordinates.idxAndLength.length,
    length = coordinates.idxAndLength.length,
    value = message ?: "No message"
)
