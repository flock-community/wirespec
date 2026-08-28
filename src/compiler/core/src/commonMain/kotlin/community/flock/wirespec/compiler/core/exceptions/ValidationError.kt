package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.tokenize.Token

internal sealed class ValidationError(coordinates: Token.Coordinates, message: String) :
    WirespecException(
        FileUri(""),
        message,
        coordinates,
    )

internal class UnionError :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Only Custom references can be part of a Union",
    )

internal class EmptyModule :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "AST should not be empty",
    )

internal class DuplicateEndpointError(endpointName: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Endpoint '$endpointName' is already defined",
    )

internal class DuplicateTypeError(typeName: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Type '$typeName' is already defined",
    )

internal class DuplicateChannelError(typeName: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Channel '$typeName' is already defined",
    )
