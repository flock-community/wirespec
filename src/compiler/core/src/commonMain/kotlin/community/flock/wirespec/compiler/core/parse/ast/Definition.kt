package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value

sealed interface Definition :
    HasMetaData,
    Node {
    val identifier: Identifier
}

data class Shared(
    val packageString: String,
) : Node

data class Field(
    override val annotations: List<Annotation>,
    val identifier: FieldIdentifier,
    val reference: Reference,
) : HasAnnotations

data class Endpoint(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val method: Method,
    val path: List<Segment>,
    val queries: List<Field>,
    val headers: List<Field>,
    val requests: List<Request>,
    val responses: List<Response>,
) : Definition {
    enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    sealed interface Segment {
        data class Literal(override val value: String) :
            Value<String>,
            Segment
        data class Param(
            val identifier: FieldIdentifier,
            val reference: Reference,
        ) : Segment
    }

    data class Request(val content: Content?)
    data class Response(val status: String, val headers: List<Field>, val content: Content?, val annotations: List<Annotation>)
    data class Content(val type: String, val reference: Reference)
}

data class Channel(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val reference: Reference,
) : Definition

sealed interface Model : Definition

data class Type(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val shape: Shape,
    val extends: List<Reference>,
) : Model {
    data class Shape(override val value: List<Field>) : Value<List<Field>>
}

data class Enum(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val entries: Set<String>,
) : Model

data class Union(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val entries: Set<Reference>,
) : Model

data class Refined(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val reference: Reference.Primitive,
) : Model
