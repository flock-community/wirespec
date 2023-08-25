package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.TokenType
import kotlin.reflect.KClass

sealed class WirespecException(message: String, val coordinates: Token.Coordinates) : RuntimeException(message) {

    sealed class IOException(message: String) : WirespecException(message, Token.Coordinates()) {
        class FileReadException(message: String) : IOException(message)
        class FileWriteException(message: String) : IOException(message)
    }

    sealed class CompilerException(message: String, coordinates: Token.Coordinates) :
        WirespecException(message, coordinates) {

        sealed class ParserException(coordinates: Token.Coordinates, message: String) :
            CompilerException(message, coordinates) {
            class WrongTokenException(expected: KClass<out TokenType>, actual: Token) :
                ParserException(
                    actual.coordinates,
                    "${expected.simpleName} expected, not: ${actual.type::class.simpleName} at line ${actual.coordinates.line} and position ${actual.coordinates.position - actual.value.length}"
                )

            sealed class NullTokenException(message: String, coordinates: Token.Coordinates) :
                ParserException(coordinates, "$message cannot be null") {
                class NextException(coordinates: Token.Coordinates) : NullTokenException("Next Token", coordinates)
            }
        }
    }
}
