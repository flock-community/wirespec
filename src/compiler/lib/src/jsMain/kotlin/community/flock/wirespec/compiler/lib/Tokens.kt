@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.tokenize.Token

fun List<Token>.produce(): WsTokenResult = WsTokenResult(tokens = WsTokens(value = map { it.produce() }.toTypedArray()))

fun Token.produce() = WsToken(
    type = type.name(),
    value = value,
    coordinates = coordinates.produce()
)

@JsExport
data class WsTokenResult(
    val tokens: WsTokens? = null,
    val error: WsError? = null
)

@JsExport
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
data class WsToken(
    val type: String,
    val value: String,
    val coordinates: WsCoordinates = WsCoordinates()
)

fun Token.Coordinates.produce() = WsCoordinates(
    line = line,
    position = position,
    idxAndLength = idxAndLength.produce()
)

@JsExport
data class WsCoordinates(
    val line: Int = 1,
    val position: Int = 1,
    val idxAndLength: WsIndex = WsIndex()
)

fun Token.Coordinates.IdxAndLength.produce() = WsIndex(
    idx = idx,
    length = length
)

@JsExport
data class WsIndex(
    val idx: Int = 0,
    val length: Int = 0
)
