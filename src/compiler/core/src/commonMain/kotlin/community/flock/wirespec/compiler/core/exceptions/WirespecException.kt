package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.tokenize.Token

sealed interface Error {
    val message: String
}

sealed class WirespecException(override val message: String, val coordinates: Token.Coordinates) : Error

sealed class IOException(message: String) : WirespecException(message, Token.Coordinates())
class FileReadException(message: String) : IOException(message)
