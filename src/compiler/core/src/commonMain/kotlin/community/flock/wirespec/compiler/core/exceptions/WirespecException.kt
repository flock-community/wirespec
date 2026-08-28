package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.tokenize.Token

public sealed interface Error {
    public val message: String
}

public sealed class WirespecException(public val fileUri: FileUri, override val message: String, public val coordinates: Token.Coordinates) : Error

public sealed class IOException(fileUri: FileUri, message: String) : WirespecException(fileUri, message, Token.Coordinates())
