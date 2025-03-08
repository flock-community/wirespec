package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.tokenize.Token

sealed class ValidationError(coordinates: Token.Coordinates, message: String) : WirespecException(message, coordinates)

class UnionsNotAllowed(identifier: Identifier) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Unions are not allowed. Found: $identifier",
    )

class UnionError :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Only Custom references can be part of a Union",
    )
