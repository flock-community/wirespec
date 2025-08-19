package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.tokenize.Token

sealed class ValidationError(coordinates: Token.Coordinates, message: String) : WirespecException("", message, coordinates)

class UnionError :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Only Custom references can be part of a Union",
    )

class EmptyModule :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "AST should not be empty",
    )

class DuplicateEndpointError(endpointName: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Endpoint '$endpointName' is already defined",
    )

class DuplicateTypeError(typeName: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Type '$typeName' is already defined",
    )

class DuplicateChannelError(typeName: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Channel '$typeName' is already defined",
    )
