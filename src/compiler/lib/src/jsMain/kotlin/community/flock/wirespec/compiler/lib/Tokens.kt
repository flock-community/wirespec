@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.name

public fun Token.produce(): WsToken = WsToken(
    type = type.name(),
    value = value,
    coordinates = coordinates.produce(),
)

@JsExport
public data class WsTokenResult(
    val tokens: WsTokens? = null,
    val error: WsError? = null,
)

@JsExport
public data class WsTokens(val value: Array<WsToken>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as WsTokens

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int = value.contentHashCode()
}

@JsExport
public data class WsToken(
    val type: String,
    val value: String,
    val coordinates: WsCoordinates = WsCoordinates(),
)

public fun Token.Coordinates.produce(): WsCoordinates = WsCoordinates(
    line = line,
    position = position,
    idxAndLength = idxAndLength.produce(),
)

@JsExport
public data class WsCoordinates(
    val line: Int = 1,
    val position: Int = 1,
    val idxAndLength: WsIndex = WsIndex(),
)

public fun Token.Coordinates.IdxAndLength.produce(): WsIndex = WsIndex(
    idx = idx,
    length = length,
)

@JsExport
public data class WsIndex(
    val idx: Int = 0,
    val length: Int = 0,
)
