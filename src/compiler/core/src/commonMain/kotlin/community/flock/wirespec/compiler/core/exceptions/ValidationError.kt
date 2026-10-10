package community.flock.wirespec.compiler.core.exceptions

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.Reference
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

internal class InvalidEnumDefaultError(fieldName: String, value: String, reference: Reference) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Invalid default value $value for field $fieldName of type ${reference.value}${if (reference.isNullable) "?" else ""}",
    )

internal class DuplicateRpcError(typeName: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Rpc '$typeName' is already defined",
    )

internal class DuplicatePartError(partName: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Part '$partName' is already defined",
    )

internal class SpreadNonPartError(name: String, owner: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Cannot spread '$name' into $owner: only parts can be spread",
    )

internal class PartAsReferenceError(partName: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Part '$partName' cannot be used as a type; spread it into a shape with ...$partName",
    )

internal class CyclicPartError(partNames: List<String>) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Parts cannot spread themselves, directly or indirectly: ${partNames.joinToString()}",
    )

internal class DuplicateFieldError(fieldName: String, owner: String) :
    ValidationError(
        coordinates = Token.Coordinates(),
        message = "Field '$fieldName' is defined more than once in $owner",
    )
