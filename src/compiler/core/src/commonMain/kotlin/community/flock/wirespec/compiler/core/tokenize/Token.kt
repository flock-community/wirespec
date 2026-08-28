package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates.IdxAndLength

public data class Token(
    override val value: String,
    val type: TokenType,
    val coordinates: Coordinates,
) : Value<String> {
    public data class Coordinates(
        val line: Int = 1,
        val position: Int = 1,
        val idxAndLength: IdxAndLength = IdxAndLength(),
    ) {
        public data class IdxAndLength(val idx: Int = 0, val length: Int = 0)
    }
}

internal operator fun Token.Coordinates.plus(length: Int) = copy(
    position = position + length,
    idxAndLength = idxAndLength + length,
)

internal operator fun IdxAndLength.plus(length: Int) = IdxAndLength(idx + length, length)
