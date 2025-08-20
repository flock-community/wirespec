package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.name
import kotlin.reflect.KClass

sealed class ParserException(fileUri: FileUri, coordinates: Token.Coordinates, message: String) : WirespecException(fileUri, message, coordinates)

sealed class EatTokenException(fileUri: FileUri, coordinates: Token.Coordinates, message: String) : ParserException(fileUri, coordinates, message)

class WrongTokenException(fileUri: FileUri, expected: KClass<out TokenType>, actual: Token) :
    EatTokenException(
        fileUri,
        actual.coordinates,
        "${expected.simpleName} expected, not: ${actual.type.name()} at line ${actual.coordinates.line} and position ${actual.coordinates.position - actual.value.length}",
    ) {
    companion object {
        inline operator fun <reified T : TokenType> invoke(fileUri: FileUri, actual: Token) = WrongTokenException(fileUri, T::class, actual)
    }
}

class DefinitionNotExistsException(fileUri: FileUri, referenceName: String, coordinates: Token.Coordinates) :
    EatTokenException(
        fileUri,
        coordinates = coordinates,
        message = "Cannot find reference: $referenceName",
    )

sealed class NullTokenException(fileUri: FileUri, message: String, coordinates: Token.Coordinates) :
    EatTokenException(
        fileUri,
        coordinates = coordinates,
        message = "$message cannot be null",
    )

class NextException(fileUri: FileUri, coordinates: Token.Coordinates) : NullTokenException(fileUri, "Next Token", coordinates)
