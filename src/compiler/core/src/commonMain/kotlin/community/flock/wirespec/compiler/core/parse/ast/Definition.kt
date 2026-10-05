package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value

public sealed interface Definition :
    HasMetaData,
    Node {
    public val identifier: Identifier
}

public sealed interface ShapeEntry

public data class Field(
    override val annotations: List<Annotation>,
    val identifier: FieldIdentifier,
    val reference: Reference,
    val defaultValue: DefaultValue? = null,
) : HasAnnotations,
    ShapeEntry

public data class Spread(val identifier: DefinitionIdentifier) : ShapeEntry

/**
 * A reusable set of fields that is only ever spread into shapes, never emitted on its own.
 * Every [Spread] is flattened into plain [Field]s during validation; the source layout stays
 * available through the `entries` properties for consumers that need to reproduce it.
 */
public data class Part(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    val identifier: DefinitionIdentifier,
    val shape: Type.Shape,
) : HasMetaData,
    Node

public data class Endpoint(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val method: Method,
    val path: List<Segment>,
    val queries: List<Field>,
    val headers: List<Field>,
    val requests: List<Request>,
    val responses: List<Response>,
    val queryEntries: List<ShapeEntry> = queries,
    val headerEntries: List<ShapeEntry> = headers,
) : Definition {
    public enum class Method { GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    public sealed interface Segment {
        public data class Literal(override val value: String) :
            Value<String>,
            Segment
        public data class Param(
            val identifier: FieldIdentifier,
            val reference: Reference,
        ) : Segment
    }

    public data class Request(val content: Content?)
    public data class Response(
        val status: String,
        val headers: List<Field>,
        val content: Content?,
        val annotations: List<Annotation>,
        val headerEntries: List<ShapeEntry> = headers,
    )
    public data class Content(val type: String, val reference: Reference)
}

public data class Channel(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val reference: Reference,
) : Definition

public data class Rpc(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val shape: Type.Shape,
    val result: Reference,
    val error: Reference?,
) : Definition

public sealed interface Model : Definition

public data class Type(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val shape: Shape,
    val extends: List<Reference>,
) : Model {
    public data class Shape(
        override val value: List<Field>,
        val entries: List<ShapeEntry> = value,
    ) : Value<List<Field>>
}

public data class Enum(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val entries: Set<String>,
) : Model

public data class Union(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val entries: Set<Reference>,
) : Model

public data class Refined(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val reference: Reference.Primitive,
) : Model
