package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.tokenize.Token.Coordinates.IdxAndLength
import community.flock.wirespec.compiler.core.tokenize.types.TokenType

data class Token(
    val type: TokenType,
    val value: String,
    val coordinates: Coordinates
) {
    data class Coordinates(
        val line: Int = 1,
        val position: Int = 1,
        val idxAndLength: IdxAndLength = IdxAndLength()
    ) {
        data class IdxAndLength(val idx: Int = 0, val length: Int = 0)
    }
}

operator fun IdxAndLength.plus(length: Int) = IdxAndLength(idx + length, length)
