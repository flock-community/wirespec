package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates.IdxAndLength
import community.flock.wirespec.compiler.core.tokenize.types.TokenType

data class Token(
    override val value: String,
    val type: TokenType,
    val coordinates: Coordinates
) : Value<String> {
    data class Coordinates(
        val line: Int = 1,
        val position: Int = 1,
        val idxAndLength: IdxAndLength = IdxAndLength()
    ) {
        data class IdxAndLength(val idx: Int = 0, val length: Int = 0)
    }
}

operator fun Token.Coordinates.plus(length: Int) = copy(
    position = position + length,
    idxAndLength = idxAndLength + length
)

operator fun IdxAndLength.plus(length: Int) = IdxAndLength(idx + length, length)
