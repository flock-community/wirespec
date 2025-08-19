package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.tokenize.Token

sealed interface Error {
    val message: String
}

sealed class WirespecException(val src: String, override val message: String, val coordinates: Token.Coordinates) : Error

sealed class IOException(src: String, message: String) : WirespecException(src, message, Token.Coordinates())
class FileReadException(src: String, message: String) : IOException(src, message)
