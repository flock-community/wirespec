package community.flock.wirespec.compare

import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Field

sealed interface Difference {
    val key: String
    val breaking: Boolean
    val message: String
}

sealed interface DefinitionDifference : Difference {
    val definition: Definition
}

data class RemovedDefinitionDifference(
    override val key: String,
    override val definition: Definition
) : DefinitionDifference {
    override val breaking: Boolean = true
    override val message: String = "Removed definition ${definition.identifier}"
}

data class AddedDefinitionDifference(
    override val key: String,
    override val definition: Definition
) : DefinitionDifference {
    override val breaking: Boolean = false
    override val message: String = "Added definition ${definition.identifier}"
}

sealed interface FieldDifference : Difference

data class RemovedFieldDifference(
    override val key: String,
    val field: Field,
) : FieldDifference {
    override val breaking = true
    override val message = "Removed field ${field.identifier.value}"
}

data class AddedFieldDifference(
    override val key: String,
    val field: Field,
) : FieldDifference {
    override val breaking = true
    override val message = "Added field ${field.identifier.value}"
}

data class ChangedNullableFieldDifference(
    override val key: String,
    val old: Field,
    val new: Field
) : FieldDifference {
    override val breaking = old.isNotNullable && new.isNullable
    override val message = "Changed field from nullable ${old.isNullable} to ${new.isNullable}"
}

sealed interface EndpointDifference : Difference

data class PathEndpointDifference(
    override val key: String,
    val old: Endpoint,
    val new: Endpoint,
) : EndpointDifference {
    override val breaking = true
    override val message = "Changed path ${old.printPath()} to ${new.printPath()}"
}

data class MethodEndpointDifference(
    override val key: String,
    val old: Endpoint,
    val new: Endpoint,
) : EndpointDifference {
    override val breaking = true
    override val message = "Changed method ${old.method} to ${new.method}"
}

interface RequestDifference : Difference {
    val request: Endpoint.Request
}

data class RemovedRequestDifference(
    override val key: String,
    override val request: Endpoint.Request
) : RequestDifference {
    override val breaking: Boolean = true
    override val message: String = "Removed definition ${request.content?.type}"
}

data class AddedRequestDifference(
    override val key: String,
    override val request: Endpoint.Request
) : RequestDifference {
    override val breaking: Boolean = false
    override val message: String = "Added definition  ${request.content?.type}"
}

interface ReferenceDifference : Difference

data class RemovedReferenceDifference(
    override val key: String,
    val reference: Field.Reference
) : ReferenceDifference {
    override val breaking: Boolean = true
    override val message: String = "Removed definition ${reference.value}"
}

data class AddedReferenceDifference(
    override val key: String,
    val reference: Field.Reference
) : ReferenceDifference {
    override val breaking: Boolean = false
    override val message: String = "Added definition  ${reference.value}"
}

data class ChangedIterableReferenceDifference(
    override val key: String,
    val old: Field.Reference,
    val new: Field.Reference
) : ReferenceDifference {
    override val breaking = true
    override val message = "Changed field from iterable ${old.isIterable} to ${new.isIterable}"
}

data class ChangedMapReferenceDifference(
    override val key: String,
    val old: Field.Reference,
    val new: Field.Reference,
) : ReferenceDifference {
    override val breaking = true
    override val message = "Changed field from iterable ${old.isDictionary} to ${new.isDictionary}"
}

data class ChangedValueReferenceDifference(
    override val key: String,
    val old: Field.Reference,
    val new: Field.Reference,
) : ReferenceDifference {
    override val breaking = true
    override val message = "Changed field ${old.value} to ${new.value}"
}

interface ContentDifference : Difference

data class RemovedContentDifference(
    override val key: String,
    val content: Endpoint.Content
) : ContentDifference {
    override val breaking: Boolean = true
    override val message: String = "Removed definition ${content.type}"
}

data class AddedContentDifference(
    override val key: String,
    val content: Endpoint.Content
) : ContentDifference {
    override val breaking: Boolean = false
    override val message: String = "Added definition  ${content.type}"
}

data class ChangedTypeContentDifference(
    override val key: String,
    val old: Endpoint.Content,
    val new: Endpoint.Content
) : ContentDifference {
    override val breaking: Boolean = false
    override val message: String = "Changed type from ${old.type} to ${new.type}"
}

data object Unknown : Difference {
    override val key: String
        get() = TODO("Not yet implemented")
    override val breaking: Boolean
        get() = TODO("Not yet implemented")
    override val message: String
        get() = TODO("Not yet implemented")
}

fun Endpoint.printPath() = "/" + path.joinToString("/") {
    when (it) {
        is Endpoint.Segment.Literal -> it.value
        is Endpoint.Segment.Param -> it.identifier.value
    }
}
