import community.flock.wirespec.compiler.core.tokenize.Token

@ExperimentalJsExport
fun List<Token>.produce(): WsTokenResult = WsTokenResult(tokens = WsTokens(value = map { it.produce() }.toTypedArray()))
fun List<Ast>.produce(): WsParseResult = WsParseResult(ast = this)

@ExperimentalJsExport
fun Token.produce() = WsToken(
    type = type.name(),
    value = value,
    coordinates = coordinates.produce()
)

@JsExport
@ExperimentalJsExport
data class WsTokenResult(
    val tokens: WsTokens? = null,
    val error: WsError? = null
)

@JsExport
@ExperimentalJsExport
data class WsParseResult(
    val ast: List<Ast>,
    val error: WsError? = null
)

@JsExport
@ExperimentalJsExport
data class WsTokens(val value: Array<WsToken>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as WsTokens

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

@JsExport
@ExperimentalJsExport
data class WsToken(
    val type: String,
    val value: String,
    val coordinates: WsCoordinates = WsCoordinates()
)

@ExperimentalJsExport
fun Token.Coordinates.produce() = WsCoordinates(
    line = line,
    position = position,
    idxAndLength = idxAndLength.produce()
)

@JsExport
@ExperimentalJsExport
data class WsCoordinates(
    val line: Int = 1,
    val position: Int = 1,
    val idxAndLength: WsIndex = WsIndex()
)

@ExperimentalJsExport
fun Token.Coordinates.IdxAndLength.produce() = WsIndex(
    idx = idx,
    length = length
)

@JsExport
@ExperimentalJsExport
data class WsIndex(
    val idx: Int = 0,
    val length: Int = 0
)
