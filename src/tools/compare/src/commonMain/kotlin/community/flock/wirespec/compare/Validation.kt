package community.flock.wirespec.compare

import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type

sealed interface Validation {
    val key: String
    val braking: Boolean
    val message: String
}

sealed interface DefinitionValidation : Validation {
    val definition: Definition
}

data class RemovedDefinitionValidation(
    override val key: String,
    override val definition: Definition
) : DefinitionValidation {
    override val braking: Boolean = true
    override val message: String = "Removed definition ${definition.name}"
}

data class AddedDefinitionValidation(
    override val key: String,
    override val definition: Definition
) : DefinitionValidation {
    override val braking: Boolean = false
    override val message: String = "Added definition ${definition.name}"
}

sealed interface FieldValidation : Validation

data class RemovedFieldValidation(
    override val key: String,
    val field: Type.Shape.Field,
) : FieldValidation {
    override val braking = true
    override val message = "Removed field ${field.identifier.value}"
}

data class AddedFieldValidation(
    override val key: String,
    val field: Type.Shape.Field,
) : FieldValidation {
    override val braking = true
    override val message = "Added field ${field.identifier.value}"
}

data class ChangedNullableFieldValidation(
    override val key: String,
    val left: Type.Shape.Field,
    val right: Type.Shape.Field
) : FieldValidation {
    override val braking = !left.isNullable && right.isNullable
    override val message = "Changed field from nullable ${left.isNullable} to ${right.isNullable}"
}

sealed interface EndpointValidation : Validation

data class PathEndpointValidation(
    override val key: String,
    val left: Endpoint,
    val right: Endpoint,
) : EndpointValidation {
    override val braking = true
    override val message = "Changed path ${left.printPath()} to ${right.printPath()}"
}

data class MethodEndpointValidation(
    override val key: String,
    val left: Endpoint,
    val right: Endpoint,
) : EndpointValidation {
    override val braking = true
    override val message = "Changed method ${left.method} to ${right.method}"
}

interface RequestValidation: Validation {
    val request: Endpoint.Request
}

data class RemovedRequestValidation(
    override val key: String,
    override val request: Endpoint.Request
) : RequestValidation {
    override val braking: Boolean = true
    override val message: String = "Removed definition ${request.content?.type}"
}

data class AddedRequestValidation(
    override val key: String,
    override val request: Endpoint.Request
) : RequestValidation {
    override val braking: Boolean = false
    override val message: String = "Added definition  ${request.content?.type}"
}

interface ReferenceValidation: Validation

data class RemovedReferenceValidation(
    override val key: String,
    val reference: Type.Shape.Field.Reference
) : ReferenceValidation {
    override val braking: Boolean = true
    override val message: String = "Removed definition ${reference.value}"
}

data class AddedReferenceValidation(
    override val key: String,
    val reference: Type.Shape.Field.Reference
) : ReferenceValidation {
    override val braking: Boolean = false
    override val message: String = "Added definition  ${reference.value}"
}

data class ChangedIterableReferenceValidation(
    override val key: String,
    val left: Type.Shape.Field.Reference,
    val right: Type.Shape.Field.Reference
) : ReferenceValidation {
    override val braking = true
    override val message = "Changed field from iterable ${left.isIterable} to ${right.isIterable}"
}

data class ChangedMapReferenceValidation(
    override val key: String,
    val left: Type.Shape.Field.Reference,
    val right: Type.Shape.Field.Reference,
) : ReferenceValidation {
    override val braking = true
    override val message = "Changed field from iterable ${left.isMap} to ${right.isMap}"
}

data class ChangedValueReferenceValidation(
    override val key: String,
    val left: Type.Shape.Field.Reference,
    val right: Type.Shape.Field.Reference,
) : ReferenceValidation {
    override val braking = true
    override val message = "Changed field ${left.value} to ${right.value}"
}
interface ContentValidation: Validation

data class RemovedContentValidation(
    override val key: String,
    val content: Endpoint.Content
) : ContentValidation {
    override val braking: Boolean = true
    override val message: String = "Removed definition ${content.type}"
}

data class AddedContentValidation(
    override val key: String,
    val content: Endpoint.Content
) : ContentValidation {
    override val braking: Boolean = false
    override val message: String = "Added definition  ${content.type}"
}

data class ChangedTypeContentValidation(
    override val key: String,
    val left: Endpoint.Content,
    val right: Endpoint.Content
) : ContentValidation {
    override val braking: Boolean = false
    override val message: String = "Changed type from ${left.type} to ${right.type}"
}

data object Unknown : Validation {
    override val key: String
        get() = TODO("Not yet implemented")
    override val braking: Boolean
        get() = TODO("Not yet implemented")
    override val message: String
        get() = TODO("Not yet implemented")
}

fun Endpoint.printPath() = "/" + path.map {
    when (it) {
        is Endpoint.Segment.Literal -> it.value
        is Endpoint.Segment.Param -> it.identifier.value
    }
}.joinToString("/")
