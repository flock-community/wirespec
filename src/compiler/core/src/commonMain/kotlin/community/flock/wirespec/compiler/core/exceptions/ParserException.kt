package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.name
import kotlin.reflect.KClass

sealed class ParserException(coordinates: Token.Coordinates, message: String) : WirespecException(message, coordinates)

sealed class EatTokenException(coordinates: Token.Coordinates, message: String) : ParserException(coordinates, message)

class GenericParserException(coordinates: Token.Coordinates, message: String) : ParserException(coordinates, message)

class WrongTokenException(expected: KClass<out TokenType>, actual: Token) :
    EatTokenException(
        actual.coordinates,
        "${expected.simpleName} expected, not: ${actual.type.name()} at line ${actual.coordinates.line} and position ${actual.coordinates.position - actual.value.length}",
    ) {
    companion object {
        inline operator fun <reified T : TokenType> invoke(actual: Token) = WrongTokenException(T::class, actual)
    }
}

class DefinitionNotExistsException(referenceName: String, coordinates: Token.Coordinates) :
    EatTokenException(
        coordinates = coordinates,
        message = "Cannot find reference: $referenceName",
    )

sealed class NullTokenException(message: String, coordinates: Token.Coordinates) :
    EatTokenException(
        coordinates = coordinates,
        message = "$message cannot be null",
    )

class NextException(coordinates: Token.Coordinates) : NullTokenException("Next Token", coordinates)
