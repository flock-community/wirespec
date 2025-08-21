package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.tokenize.Token

sealed interface Error {
    val message: String
}

sealed class WirespecException(val fileUri: FileUri, override val message: String, val coordinates: Token.Coordinates) : Error

sealed class IOException(fileUri: FileUri, message: String) : WirespecException(fileUri, message, Token.Coordinates())
