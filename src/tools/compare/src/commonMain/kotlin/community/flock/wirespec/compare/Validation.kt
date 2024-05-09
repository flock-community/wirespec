package community.flock.wirespec.compare

import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type

sealed interface Validation {
    val braking: Boolean
    val message: String
}

sealed interface DefinitionValidation : Validation {
    val definition: Definition
}

data class RemovedDefinitionValidation(
    override val definition: Definition
) : DefinitionValidation {
    override val braking: Boolean = true
    override val message: String = "Removed definition ${definition.name}"
}

data class AddedDefinitionValidation(
    override val definition: Definition
) : DefinitionValidation {
    override val braking: Boolean = false
    override val message: String = "Added definition ${definition.name}"
}

sealed interface FieldValidation : Validation
data class RemovedFieldValidation(
    val field: Type.Shape.Field,
) : FieldValidation {
    override val braking = true
    override val message = "Removed field ${field.identifier.value}"
}

data class AddedFieldValidation(
    val field: Type.Shape.Field,
) : FieldValidation {
    override val braking = true
    override val message = "Added field ${field.identifier.value}"
}

data class ChangedNullableFieldValidation(
    val left: Type.Shape.Field,
    val right: Type.Shape.Field
) : FieldValidation {
    override val braking = !left.isNullable && right.isNullable
    override val message = "Changed field from nullable ${left.isNullable} to ${right.isNullable}"
}

data class ChangedIterableFieldValidation(
    val left: Type.Shape.Field,
    val right: Type.Shape.Field
) : FieldValidation {
    override val braking = true
    override val message = "Changed field from iterable ${left.reference.isIterable} to ${right.reference.isIterable}"
}

data class ChangedMapFieldValidation(
    val left: Type.Shape.Field,
    val right: Type.Shape.Field
) : FieldValidation {
    override val braking = true
    override val message = "Changed field from iterable ${left.reference.isMap} to ${right.reference.isMap}"
}


data class ChangedReferenceFieldValidation(
    val left: Type.Shape.Field,
    val right: Type.Shape.Field
) : FieldValidation {
    override val braking = true
    override val message = "Changed field ${left.reference.value} to ${right.reference.value}"
}

sealed interface EndpointValidation : Validation

data class PathEndpointValidation(
    val left: Endpoint,
    val right: Endpoint,
) : EndpointValidation {
    override val braking = true
    override val message = "Changed path ${left.printPath()} to ${right.printPath()}"
}

data class MethodEndpointValidation(
    val left: Endpoint,
    val right: Endpoint,
) : EndpointValidation {
    override val braking = true
    override val message = "Changed method ${left.method} to ${right.method}"
}


data object Unknown : Validation {
    override val braking: Boolean
        get() = TODO("Not yet implemented")
    override val message: String
        get() = TODO("Not yet implemented")
}

fun Type.Shape.Field.printReference() = buildString {
    append(reference.value)
    if (reference.isIterable) append("[]")
    if (isNullable) append("?")
}

fun Endpoint.printPath() = "/" + path.map {
    when (it) {
        is Endpoint.Segment.Literal -> it.value
        is Endpoint.Segment.Param -> it.identifier.value
    }
}.joinToString("/")
