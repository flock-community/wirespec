package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.tokenize.Token

internal sealed interface Error {
    val message: String
}

public sealed class WirespecException(public val fileUri: FileUri, override val message: String, public val coordinates: Token.Coordinates) : Error
