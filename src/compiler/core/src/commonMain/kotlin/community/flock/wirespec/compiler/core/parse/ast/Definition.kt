package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.Value

public sealed interface Definition :
    HasComment,
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

public val List<ShapeEntry>.fields: List<Field> get() = filterIsInstance<Field>()

/**
 * A reusable set of fields that is only ever spread into shapes, never emitted on its own.
 * Validation replaces every [Spread] with the fields of its part, so after parsing every
 * shape holds plain [Field]s only.
 */
public data class Part(
    override val comment: Comment?,
    override val identifier: DefinitionIdentifier,
    val shape: Type.Shape,
) : Definition

public data class Endpoint(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val method: Method,
    val path: List<Segment>,
    val queries: List<ShapeEntry>,
    val headers: List<ShapeEntry>,
    val requests: List<Request>,
    val responses: List<Response>,
) : Definition,
    HasMetaData {
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
    public data class Response(val status: String, val headers: List<ShapeEntry>, val content: Content?, val annotations: List<Annotation>)
    public data class Content(val type: String, val reference: Reference)
}

public data class Channel(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val reference: Reference,
) : Definition,
    HasMetaData

public data class Rpc(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val shape: Type.Shape,
    val result: Reference,
    val error: Reference?,
) : Definition,
    HasMetaData

public sealed interface Model :
    Definition,
    HasMetaData

public data class Type(
    override val comment: Comment?,
    override val annotations: List<Annotation>,
    override val identifier: DefinitionIdentifier,
    val shape: Shape,
    val extends: List<Reference>,
) : Model {
    public data class Shape(override val value: List<ShapeEntry>) : Value<List<ShapeEntry>>
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
