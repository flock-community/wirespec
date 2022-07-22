package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.TokenType

sealed class WireSpecException(message: String, val coordinates: Token.Coordinates) : RuntimeException(message) {

    sealed class IOException(message: String) : WireSpecException(message, Token.Coordinates()) {
        class FileReadException(message: String) : IOException(message)
        class FileWriteException(message: String) : IOException(message)
    }

    sealed class CompilerException(message: String, coordinates: Token.Coordinates) :
        WireSpecException(message, coordinates) {

        sealed class ParserException(coordinates: Token.Coordinates, message: String) :
            CompilerException(message, coordinates) {
            class WrongTokenException(expected: TokenType, actual: Token) :
                ParserException(
                    actual.coordinates,
                    "${expected::class.simpleName} expected, not: ${actual.type::class.simpleName} at line ${actual.coordinates.line} and position ${actual.coordinates.position - actual.value.length}"
                )

            sealed class NullTokenException(message: String, coordinates: Token.Coordinates) :
                ParserException(coordinates, "$message cannot be null") {
                class StartingException : NullTokenException("Starting Token", Token.Coordinates())
                class NextException(coordinates: Token.Coordinates) : NullTokenException("Next Token", coordinates)
            }
        }

        class TokenizerException(coordinates: Token.Coordinates, message: String) :
            CompilerException(message, coordinates)

    }

}
