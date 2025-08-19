package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.name
import kotlin.reflect.KClass

sealed class ParserException(src: String, coordinates: Token.Coordinates, message: String) : WirespecException(src, message, coordinates)

sealed class EatTokenException(src: String, coordinates: Token.Coordinates, message: String) : ParserException(src, coordinates, message)

class WrongTokenException(src: String, expected: KClass<out TokenType>, actual: Token) :
    EatTokenException(
        src,
        actual.coordinates,
        "${expected.simpleName} expected, not: ${actual.type.name()} at line ${actual.coordinates.line} and position ${actual.coordinates.position - actual.value.length}",
    ) {
    companion object {
        inline operator fun <reified T : TokenType> invoke(src: String, actual: Token) = WrongTokenException(src, T::class, actual)
    }
}

class DefinitionNotExistsException(src: String, referenceName: String, coordinates: Token.Coordinates) :
    EatTokenException(
        src,
        coordinates = coordinates,
        message = "Cannot find reference: $referenceName",
    )

sealed class NullTokenException(src: String, message: String, coordinates: Token.Coordinates) :
    EatTokenException(
        src,
        coordinates = coordinates,
        message = "$message cannot be null",
    )

class NextException(src: String, coordinates: Token.Coordinates) : NullTokenException(src, "Next Token", coordinates)
